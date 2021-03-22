package com.example.doDue.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.example.doDue.data.PreferencesManager
import com.example.doDue.data.Task
import com.example.doDue.data.TaskDao
import com.example.doDue.ui.ADD_TASK_RESULT_OK
import com.example.doDue.ui.EDIT_TASK_RESULT_OK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class SortOrder { BY_NAME, BY_DATE }

/* Our ViewModel needs a DAO reference and then this ViewModel needs to be injected in the respective fragment. Thus, we use @ViewModelInject here. */
/* ViewModels don't (and shouldn't) keep reference to the Fragments as upon the deletion of Fragments it may cause memory leaks. That is why we use Flow of data,
* which can be observed live by any fragment, instead of the ViewModel remembering the Fragment itself. */
/* Now we also inject our PreferencesManager file here (using Dependency Injection). Although the PreferencesManager file doesn't need to be added in the DI.
* We used @Inject in it directly. */

class TaskViewModel @ViewModelInject constructor(private val taskDao: TaskDao, private val preferencesManager: PreferencesManager,
                                                 @Assisted private val state: SavedStateHandle): ViewModel() {

    /* Flow and LiveData is almost the same. The difference is that LiveData() has just the latest value, whereas Flow has track of the whole stream.
    * LiveData() has it's benefits, that it is LifeCycle aware i.e. if a Fragment stops then LiveData() stops dispatching data as well, which Flow doesn't guarantee.
    * We used both here, as we want the info. of the whole stream and then we want each Task to be a LiveData() because of the above reason.
    * Therefore we turn the data inside the ViewModel as a LiveData. */

    /* We want to pass the current string in the search bar to the DAO function. Therefore, we want a Live String of searchQuery. Therefore, we use MutableStateFlow. */

    /* flatMapLatest basically takes the String value from the searchQuery Flow, performs the DAO operation on that String,
    and then returns another Flow(this time a Flow of List of Tasks).
    * This Flow is then converted and used as a LiveData which is then passed to the Adapter List (and hence to the RecyclerView). */

    /* We have to store the state of "Hide completed tasks" and "Sort Order", so that when we open it these are not reset. We use Jetpack Datastore for this purpose. */

    /* We create an enum class for the Sort type. And initialize them with BY_DATE and false to the variables below. */
    /* combine() is a function that takes in multiple flows and finally another function(used as a trailing lambda here) which helps perform some action on the flows.
    * Whenever a Flow value changes, combine will immediately take the latest values and combine them and forward them to the DAO through flatMapLatest. */


    /* This function takes these Flows and combines them into one Triple(changed to Pair) Object. In "query, sortOrder, hideCompleted -> Triple(query, sortOrder, hideCompleted)",
    * query, sortOrder and hideCompleted are the function variables which are getting their values from combine() parameters. And then this lambda combines them into
    * the Triple.*/
    /* Later, we changed it to preferencesFlow which has both sortOrder and hideCompleted in a single object. So we changed everything accordingly, and then passed
    * sortOrder and hideCompleted from it to the DAO. */

    /* We will also add a SavedStateHandle for the searchQuery as we want to persist it's value even when the Fragment is paused/rotated, i.e. even when the data might
    * get lost/forgotten.
    * Although SavedStateHandle doesn't work with Flow, we can convert it into a LiveData() here and then change to Flow wherever needed (in combine() here).
    * Cool thing is that we don't need to set the setter function for a LiveData() as it automatically gets persisted. */

    val searchQuery = state.getLiveData("searchQuery", "")

//    val sortOrder = MutableStateFlow(SortOrder.BY_DATE)
//    val hideCompletedTasks = MutableStateFlow(false)

    val preferencesFlow = preferencesManager.preferencesFlow

    private val tasksFlow =
        combine(searchQuery.asFlow(), preferencesFlow) { query, preferencesFlow ->
            Pair(query, preferencesFlow)
        }.flatMapLatest {
            taskDao.getTasks(it.first, it.second.sortOrder, it.second.hideCompleted)
        }

    val tasks = tasksFlow.asLiveData()

    /* As Fragments should talk to only their ViewModels for any updation/operation, we have to create a function that will get the values of sortOrder and hideCompleted
    * from the Fragment and then this has to be updated to the DataStore, so we pass it to the update functions created there.
    *
    * To pass them to the DataStore update functions(which are suspend functions), we have to call viewModelScope.launch{}, which again, obviously, has
    * to be a Coroutine/CoroutineScope. */

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedSelected(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    /* These functions will open the Add/Edit task fragment or do the updation of the checkbox of task. These have been called from the "OnItemClickListener"
    interface definitions, defined in the Fragment. */
    /* Just like PreferencesManager file, DAO also has suspend functions, which need a Coroutine Scope to be launched to execute on them. For this we again use
    * viewModelScope.launch{}. */

    //-------------------------------------------------
    /* This is although an interface function, but we actually call the Event created in the end here. This will open up the AddEditTaskFragment and also passes the task to
    be displayed already in that Fragment. */

    fun onTaskSelected(task: Task) = viewModelScope.launch{
        tasksEventChannel.send(TasksEvent.NavigateToEditTaskScreen(task))
    }

    //-------------------------------------------------

    fun onTaskCheckChanged(task: Task, isChecked: Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }
    //task.copy() is used as all the task properties are immutable, and thus we have to create a copy of them and then overwrite them, instead of directly changing them.

//    |
//    |
//    |
//    |
//    |

    private val tasksEventChannel = Channel<TasksEvent>()
    val tasksEvent = tasksEventChannel.receiveAsFlow()
    /* We make the above variable public as this has to be accessed by the Fragment to help establish a link to the Channel created here. And we pass it as a Flow,
    so that it can be used within the coroutine scope. */

    /* Therefore, the way this works is that-
    * A function(Task Swipe Delete) triggers a viewModel function(onTaskSwiped),
    * which then sends an event which tells to do something in the Fragment. Then after doing the required task in the Fragment(displaying SnackBar), we can again
    * trigger a ViewModel function(onUndoDeleteClick) that'll perform another operation(insert Task back to DB). */

    fun onTaskSwiped(task: Task) = viewModelScope.launch {
        taskDao.delete(task)

        /*Deleting the task should display a SnackBar for undo option. But the Fragment shouldn't be responsible for that, it's the ViewModel's work. But showing the
        * SnackBar and it's functions can only be done in an Activity or Fragment. Therefore, we need some workaround to dispatch this event from the ViewModel
        * to the Fragment.
        * Now we can use LiveData or Flow to do so. But the problem is that they take the latest values and would give
        * them every time a new event is created (This should happen normally, which is good, but not for our case). So whenever the Fragment is created again after deletion, i.e. when let say the
        * Fragment is rotated (therefore, a new Fragment will be generated again), it will create a SnackBar again for the same deletion that already happened. This will be done over and over again
        * for all rotations. This is not good.
        *
        * "Channels" is the solution to it. This is like a tunnel from one Coroutine to another. Thus if here we want to dispatch the SnackBar event(as it should happen through the ViewModel only)
        * this would happen, and the Fragment on the other side of the channel can take these dispatch objects sent by ViewModel and can work upon displaying the SnackBar (as only a Fragment/
        * Activity can do that).
        * Also since both are in a Coroutine tie-up, they both respect each others suspend states. Therefore, if the ViewModel is not ready/empty, then the Fragment would not do any SnackBar work.
        * And vice-versa. All this helps enable the single event functionality and also the dispatch event functionality. */

        tasksEventChannel.send(TasksEvent.ShowUndoDeleteTaskMessage(task))
    }

    // Function that adds back the task, when Undo is clicked on the SnackBar.

    fun onUndoDeleteClick(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

//    |
//    |
//    |
//    |
//    |

    /* Function that adds a new task when the FAB is clicked. This works the same as the above 2 functions. A function(Fragment FAB click) triggers a viewModel function,
    * which then sends an event which tells to do something in the Fragment. Then after doing the required task in the Fragment, we can again trigger a ViewModel function
    * that'll perform another operation, if needed. */

    fun onAddNewTaskClicked() = viewModelScope.launch{
        tasksEventChannel.send(TasksEvent.NavigateToAddTaskScreen)
    }

//    |
//    |
//    |
//    |
//    |

    /* Function that sends an event to display the appropriate SnackBar upon correctly adding/editing a task. */

    fun onAddEditResult(result: Int) {

        when(result){

            ADD_TASK_RESULT_OK -> showTaskSavedConfirmMessage("Task Added!")
            EDIT_TASK_RESULT_OK -> showTaskSavedConfirmMessage("Task Updated!")
        }

    }

    private fun showTaskSavedConfirmMessage(text: String) = viewModelScope.launch{
        tasksEventChannel.send(TasksEvent.ShowTaskSavedConfirmationMessage(text))
    }

    /* Final ViewModel function that dispatches an event again to the Fragment, to open DeleteAllCompletedDialogFragment.  */

    fun deleteAllCompletedClick() = viewModelScope.launch{
        tasksEventChannel.send(TasksEvent.NavigateToDeleteAllCompletedScreen)
    }


    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------//

    /* To create a class of events (which in themselves are classes/data classes), we use a sealed class. Also, sealed class helps in clearly identifying each type of
    * Event/class created, thus helping us provide a container for them and also helping in providing checks on the correct class/event usage.
    * Therefore, TasksEvent contains all the events to be forwarded to the Fragment. */

    /* We have created 3 events, which trigger the SnackBar, or open AddEditTaskFragment when clicked on FAB button, or open AddEditTaskFragment when clicked on
    * the Task itself(in this case we have to pass the task too). */

    //We should create an object when we don't have any parameters to pass.

    sealed class TasksEvent {
        data class ShowUndoDeleteTaskMessage(val task: Task): TasksEvent()
        object NavigateToAddTaskScreen: TasksEvent()
        data class NavigateToEditTaskScreen(val task: Task): TasksEvent()
        data class ShowTaskSavedConfirmationMessage(val msg: String): TasksEvent()
        object NavigateToDeleteAllCompletedScreen: TasksEvent()
    }

}

