package com.example.doDue.ui.deleteallcompleted

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

/* A Dialog can also be added as a Fragment and also has it's own Class and Default Functions. Although it can be directly created in an Activity/Fragment, but using this
* way helps in re-using the same module/fragment elsewhere as well. Re-usability of code is also a good technique which should be maintained as much as possible (even if
* we are just going to use it once here).
*
* We also have to therefore create a ViewModel for it as well. But since the Builder() method of AlertDialog provides for a default layout, we don't need to make a layout
* file for the same.
*
* This Dialog "Fragment" is then added to the NavGraph (as it will now be treated as a Fragment). */


@AndroidEntryPoint
class DeleteAllCompletedDialogFragment: DialogFragment() {

    private val deleteAllCompletedDialogViewModel: DeleteAllCompletedDialogViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion ?")
            .setMessage("Do you really want to delete all completed tasks ?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes"){ _, _ ->
                deleteAllCompletedDialogViewModel.onConfirmClick()
            }
            .create()

}