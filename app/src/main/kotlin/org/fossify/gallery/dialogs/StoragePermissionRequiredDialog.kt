package org.fossify.gallery.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.DialogStoragePermissionRequiredBinding

class StoragePermissionRequiredDialog(
    val activity: SimpleActivity,
    val onOkay: () -> Unit,
    val onCancel: () -> Unit,
    val callback: (dialog: AlertDialog) -> Unit
) {

    init {
        val binding = DialogStoragePermissionRequiredBinding.inflate(activity.layoutInflater)
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.go_to_settings) { dialog, _ ->
                dialog.dismiss()
                onOkay()
            }
            .setNegativeButton(org.fossify.commons.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    cancelOnTouchOutside = false,
                    callback = callback
                )
            }
    }
}
