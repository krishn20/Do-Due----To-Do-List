package com.example.doDue.ui.deleteallcompleted

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.example.doDue.data.TaskDao
import com.example.doDue.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteAllCompletedDialogViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    @AppModule.ApplicationScope private val applicationScope: CoroutineScope
    ): ViewModel()
{
    /*We used the applicationScope because DialogFragment's viewModel gets removed just after the Dialog is clicked on. Thus it may happen that, before we even do our
    * TaskDao operation, the viewModel scope gets removed and thus delete operation is just cancelled. So, we use the ApplicationScope created in the AppModule. */

    fun onConfirmClick() = applicationScope.launch {
        taskDao.deleteCompletedTasks()
    }


}