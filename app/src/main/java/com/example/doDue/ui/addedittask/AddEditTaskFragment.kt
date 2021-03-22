package com.example.doDue.ui.addedittask

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.doDue.R
import com.example.doDue.databinding.FragmentAddEditTaskBinding
import com.example.doDue.util.exhaustive
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class AddEditTaskFragment: Fragment(R.layout.fragment_add_edit_task) {

    private val addEditTaskViewModel : AddEditTaskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAddEditTaskBinding.bind(view)

        binding.apply {

            /* These will set the value of the view
            items to the received task values. */

            addEditTaskName.setText(addEditTaskViewModel.taskName)

            checkboxImp.isChecked = addEditTaskViewModel.taskPriority
            checkboxImp.jumpDrawablesToCurrentState()
            //this method skips the animation of checking and directly shows the checked box (if it was already checked when we opened the AddEdit Fragment).

            textViewDateCreated.isVisible = addEditTaskViewModel.task != null
            textViewDateCreated.text = "Created: ${addEditTaskViewModel.task?.createdDateTimeFormat}"
            //Here we only want the dateTime to be visible when we actually have a value. So we use visible function and also set it's value upon finding one.

            //---------------------------------------------------------------------------------------------------------------------------------------------------//
            //---------------------------------------------------------------------------------------------------------------------------------------------------//

            /* These will update the values upon
            editing/adding the task. */

            addEditTaskName.addTextChangedListener{
                addEditTaskViewModel.taskName = it.toString()
            }

            checkboxImp.setOnCheckedChangeListener { _, isChecked ->
                addEditTaskViewModel.taskPriority = isChecked
            }

            /* Now after adding/editing the task, we have to click the Done/Tick FAB which then
            * should trigger this Fragment's ViewModel to update/add the task and
            * also return to the previous fragment. */

            fabSaveTask.setOnClickListener {
                addEditTaskViewModel.onSaveClick(addEditTaskViewModel.task)
            }

        } //***binding close***



        viewLifecycleOwner.lifecycleScope.launchWhenStarted {

            addEditTaskViewModel.addEditTasksEvent.collect { event ->

                when(event){
                    is AddEditTaskViewModel.AddEditTaskEvents.ShowInvalidInputMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }

                    is AddEditTaskViewModel.AddEditTaskEvents.NavigateBackWithResult -> {

                        //Hide the keyboard when we move back to the previous fragment.
                        binding.addEditTaskName.clearFocus()

                        /*setFragmentResult is a new API that helps send results from one Fragment to another relatively easily. Also after that we pop this fragment and
                        * go back to the previous fragment. */

                        setFragmentResult("add_edit_request", bundleOf("add_edit_result" to event.result))
                        findNavController().popBackStack()
                    }

                }.exhaustive

            }
        }


    }


}