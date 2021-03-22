 package com.example.doDue.ui.addedittask

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doDue.data.Task
import com.example.doDue.data.TaskDao
import com.example.doDue.ui.ADD_TASK_RESULT_OK
import com.example.doDue.ui.EDIT_TASK_RESULT_OK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

 class AddEditTaskViewModel @ViewModelInject constructor(private val taskDao: TaskDao, @Assisted private val state: SavedStateHandle): ViewModel() {

     /* Whenever we pass something from one Fragment to another, we can receive them in the Fragment itself. But we actually want our ViewModel to responsible for
     taking it, as we want our Fragment to be as dumb as possible and we want ViewModel to get the latest/updated/any data.
      *
      * So here, we want a Task type value (that is passed from TasksFragment) directly into this ViewModel. To do so we use SavedStateHandle().
      * It is useful especially when a process death occurs. For eg.- When we rotate our screen, the Fragment is killed and a new fragment is created. This is Process
      * Death. So, to save values while this happens, we use "savedInstanceState" and Bundle those values up, to be used when the Fragment is re-created.
      * The same is available to be used directly into our Fragment's ViewModel using a handler i.e. the SavedStateHandle(). */

     /* Now we get our task value by using get() in our saved state(which is a task here).
     * Also we separate our taskName and taskPriority as they can either already be set or have to be set now. So we need to work upon them, separately.
     * We can(and should) get taskName from the state, or directly from the saved task earlier or if it was empty already(i.e. new task) then it should be null/"".
     * Same for taskPriority.
     *
     * Now to set them for both the cases, we use the variable's in-built set() function. "value" is the value that we give for that field. Thus the field is set in
     * real-time. Also we save it in the state, for the same reasons mentioned above. */

     /* The name for the task here should be same as the argument name which we pass from the TasksFragment to this Fragment's ViewModel. */

     val task = state.get<Task>("task")

     var taskName =  state.get<String>("taskName") ?: task?.name ?: ""
        set(value){
            field = value
            state.set("taskName", value)
        }

     var taskPriority = state.get<Boolean>("taskPriority") ?: task?.priority ?: false
        set(value) {
            field = value
            state.set("taskPriority", value)
        }

     /* The function that implements the logic when the Check FAB Button is clicked. */

     fun onSaveClick(task: Task?){
         if (taskName.isBlank()){
             showInvalidMessage("Name cannot be empty!")
             return
             //That is, don't do anything after it.
         }

         if (task != null) {
             val updatedTask = task.copy(name = taskName, priority = taskPriority)
             updateTask(updatedTask)

         } else {
             val newTask = Task(name = taskName, priority = taskPriority)
             createTask(newTask)
         }

     }

     //--------------------------------------------------------------------------

     private val addEditTasksEventChannel = Channel<AddEditTaskEvents>()
     val addEditTasksEvent = addEditTasksEventChannel.receiveAsFlow()

     //--------------------------------------------------------------------------

     /* Just created these functions here as we don't want the above code to be very long in just one scope. */

     private fun showInvalidMessage(text: String) = viewModelScope.launch{
         addEditTasksEventChannel.send(AddEditTaskEvents.ShowInvalidInputMessage(text))
     }

     private fun updateTask(updatedTask: Task) = viewModelScope.launch{
         taskDao.update(updatedTask)
         addEditTasksEventChannel.send(AddEditTaskEvents.NavigateBackWithResult(EDIT_TASK_RESULT_OK))
     }

     private fun createTask(newTask: Task) = viewModelScope.launch{
         taskDao.insert(newTask)
         addEditTasksEventChannel.send(AddEditTaskEvents.NavigateBackWithResult(ADD_TASK_RESULT_OK))
     }

     //----------------------------------------------------------------------------------------//

     sealed class AddEditTaskEvents{
         data class ShowInvalidInputMessage(val msg: String): AddEditTaskEvents()
         data class NavigateBackWithResult(val result: Int): AddEditTaskEvents()
     }


 }