package com.example.doDue.ui.tasks

import android.graphics.BlendMode
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doDue.R
import com.example.doDue.data.Task
import com.example.doDue.databinding.FragmentTasksBinding
import com.example.doDue.util.exhaustive
import com.example.doDue.util.onQueryTextChanged
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment: Fragment(R.layout.fragment_tasks), TaskAdapter.OnItemClickListener {

    /* We just need to pass the list of Tasks as well to the Adapter, but we don't (and shouldn't) provide it to the Fragment, as Fragment shouldn't be responsible
    * for doing that. The reason is that on changing orientations, the views (and thus the fragments) get destroyed and thus the data if taken along with them, will
    * also be destroyed. */
    /* We use the ViewModel for the same, as they don't get destroyed when the orientation/layout changes. */

    private val viewModel: TaskViewModel by viewModels()

    private lateinit var searchView: SearchView

    /*  We also make a ViewBinding for the view of the Fragment itself, which contains the RecyclerView. Here we don't need to inflate it,
    as it is already done in the Fragment. */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentTasksBinding.bind(view)

        // "this" keyword will straight forward pass a reference object of interface type to our Adapter.
        val taskAdapter = TaskAdapter(this)

        // Instead of using binding.recyclerViewTasks.adapter = TaskAdapter(), we just use .apply which helps for a quicker experience.

        binding.apply {

            recyclerViewTasks.apply {
                adapter = taskAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            /*Here we add the functionality for Swipe to Delete Tasks. ItemTouchHelper as the name says, enables Touch options on the RecyclerView List Items. That is why we don't need to do it
            * in the Adapter (or by using Interface). Therefore we use it within the layout binding scope (from where we can also access the 'recyclerView' view.
            * But to actually attach the renewed list (after deleting anything), we use attachToRecyclerView(recyclerViewTasks).
            *
            * We don't need onMove functionality here. And onSwiped here calls the viewModel and calls a function which helps delete the task using tasksDao.
            * This function also calls the event to display the SnackBar through the ViewModel and then we do the operation of SnackBar here only.
            * We just do this as all events should pass to the ViewModel and then it should delegate the work on the Fragment. */

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT){

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val task = taskAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTaskSwiped(task)
                }

            }).attachToRecyclerView(recyclerViewTasks)

            /* Adding the action to go to AddEditFragment when the FAB is clicked. */

            fabAddTasks.setOnClickListener {
                viewModel.onAddNewTaskClicked()
            }

        } //***binding close***



        /* viewLifecycleOwner is important as it keeps track of the Fragment views. Thus if a Fragment changes, then this list of Tasks will be provided to that
        * Fragment (which obviously doesn't need it) and thus the app may crash. */
        // Also we use lambdas here to directly use the List<task> and pass it to the Adapter.

        viewModel.tasks.observe(viewLifecycleOwner){
            taskAdapter.submitList(it)
        }



        /* This Coroutine scope is to allow the Channel Events to be consumed here(only when Channel has started, thus using launchWhenStarted). We have already created a TaskEventChannel (and it's
        * variable to connect to the channel here, i.e. tasksEvent) using which we call the TasksEvent Class type(sealed class) events(classes inside it). */

        /* SnackBar takes parameters just like a Toast Message. setAction is the action that happens when the button on the SnackBar is clicked. Again, we pass this action to the viewModel,
        * which will then add this task back again to the DB. */

        //event.task is a SmartCast.

        viewLifecycleOwner.lifecycleScope.launchWhenStarted{
            viewModel.tasksEvent.collect{ event ->

                when(event){

                    is TaskViewModel.TasksEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(requireView(), "Task Deleted", Snackbar.LENGTH_LONG).setAction("UNDO") {
                            viewModel.onUndoDeleteClick(event.task)
                        }.show()
                    }

                    /*TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment() directly gives access to the action/intent to go to the next Fragment.
                    * We just have to use navController to use that action. */
                    /*Also we have to pass the title of the Fragment based on the requirement. For this we have already created NavArgs(which are now compile-time Safe).
                    * These are thus called SafeArgs as well. */

                    is TaskViewModel.TasksEvent.NavigateToAddTaskScreen -> {
                        val action =
                            TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(
                                null,
                                "New Task"
                            )
                        findNavController().navigate(action)
                    }

                    is TaskViewModel.TasksEvent.NavigateToEditTaskScreen -> {
                        val action =
                            TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(
                                event.task,
                                "Edit Task"
                            )
                        findNavController().navigate(action)
                        //event.task is a SmartCast.
                    }

                    is TaskViewModel.TasksEvent.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }

                    is TaskViewModel.TasksEvent.NavigateToDeleteAllCompletedScreen -> {
                        val action =
                            TasksFragmentDirections.actionGlobalDeleteAllCompletedDialogFragment()
                        findNavController().navigate(action)
                    }


                }.exhaustive
                //exhaustive ensures that all events are added, thus ensuring compile-time safety (which is not done normally by the when statement).

            }
        }



        /* Here we call the setFragmentResult's Listener and then take the results of the AddEditTaskFragment upon which we work then. */
        /* Again we send it to the viewModel, which then dispatches an event to this Fragment which then shows the appropriate SnackBar. */

        setFragmentResultListener("add_edit_request"){ _, bundle ->

            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)

        }

        setHasOptionsMenu(true)

    }



    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_tasks, menu)

        //menu here is the Menu type object created to use here, within this scope (so as to use and work upon the items in the menu here).

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        /* Here we add the logic to keep our searchView up to date with the searchQuery entered and thus keep open the searchView action (to expand the search bar)
        * upon deleting and re-creating a Fragment (mostly done when the Fragment is rotated). */

        val pendingQuery = viewModel.searchQuery.value
        if(pendingQuery != null && pendingQuery.isNotBlank()){
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

//------- The simpler way to use the text typed in Search Bar and immediately give it to the ViewModel.-------//

//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return true
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                viewModel.searchQuery.value = newText.orEmpty()
//                return true
//            }
//
//        })

//------------------------------------------------------------------------------------------------------------//

        searchView.onQueryTextChanged {

            /* searchView.onQueryTextChanged gets back the String typed in the search bar which we then pass to the ViewModel's searchQuery variable. */

            viewModel.searchQuery.value = it

        }

        /* When we open our app again, we want the hideCompleted to be set but also we want it's checkMark to be set in the optionsMenu. For this, we will check the
        * lifecycle of this view and then launch a lifeCycleScope Coroutine. Inside this coroutine/thread, we will check for the value of hideCompleted using
        * preferencesFlow created in the ViewModel. And then we will check mark it if set.
        *
        * But we will(and should) do this, only for the first time i.e. only just when we opened the app after pause/stop state or after some time, we want
        * this to be inferred. Because after that, it will be set automatically within the app itself. For this one time operation,
        * and thus to cancel the Flow later, we use first().
        *
        * After the Flow is cancelled, we won't get Live Async updates again, as we don't need them. Also, this won't cancel hideCompleted Flows elsewhere, as
        * this Flow is only cancelled in this Coroutine/Thread Scope. */

        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked = viewModel.preferencesFlow.first().hideCompleted

            if(viewModel.preferencesFlow.first().sortOrder == SortOrder.BY_DATE){
                menu.findItem(R.id.action_sort_by_date).isChecked
            }
            else{
                menu.findItem(R.id.action_sort_by_name).isChecked
            }

        }

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId)
        {
            R.id.action_sort_by_name -> {
                viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                item.isCheckable
                item.isChecked = true
                true
            }

            R.id.action_sort_by_date -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                item.isCheckable
                item.isChecked = true
                true
            }

            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                viewModel.onHideCompletedSelected(item.isChecked)
                true
            }

            R.id.action_delete_all_completed_tasks -> {
                viewModel.deleteAllCompletedClick()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /* These are the OnItemClickListener interface functions implemented here. Here, we will write their definitions, which basically calls the ViewModel to implement
    * the changes in the list tasks.
    *
    * The first function should open up the Add/Edit tasks fragment. The second function is to update the task if it has been completed or not. */

    override fun onItemClick(task: Task) {
        viewModel.onTaskSelected(task)
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
        viewModel.onTaskCheckChanged(task, isChecked)
    }

    /* Android does this weird thing where it sends an empty String to the Fragment's SearchView if deleted and re-created i.e. if it is rotated.
    * To avoid sending that empty string, we make the listener value null (that we passed using SearchView to get the latest String added). */

    override fun onDestroyView() {
        super.onDestroyView()
        searchView.setOnQueryTextListener(null)
    }

}