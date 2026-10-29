package org.fossify.gallery.helpers

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showErrorToast
import org.fossify.gallery.R
import org.fossify.gallery.activities.BaseViewerActivity
import org.fossify.gallery.databinding.DialogQrScanBinding
import org.fossify.gallery.databinding.DialogQrMessageBinding
import org.fossify.gallery.dialogs.QrCodeResultDialog

/** Owns cancellable scanning and dialogs for a single image viewer. */
class QrScanController(private val activity: BaseViewerActivity) {
    private var scanJob: Job? = null
    private var dialog: AlertDialog? = null

    /** Scans the captured source. */
    fun scan(source: String) {
        if (scanJob?.isActive == true) return

        cancel()
        val binding = DialogQrScanBinding.inflate(activity.layoutInflater)
        activity.getAlertDialogBuilder()
            .setNegativeButton(org.fossify.commons.R.string.cancel) { _, _ -> cancel() }
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.scan_qr_code) {
                    dialog = it
                    it.setCanceledOnTouchOutside(false)
                    it.setOnCancelListener { cancel() }
                }
            }

        scanJob = activity.lifecycleScope.launch {
            try {
                val results = runInterruptible(Dispatchers.IO) {
                    QrCodeScanner.scan(activity.applicationContext, source)
                }
                dialog?.dismiss()
                dialog = if (results.isEmpty()) {
                    showMessage(R.string.qr_code_not_found)
                } else {
                    QrCodeResultDialog(activity, results).dialog
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                dialog?.dismiss()
                activity.showErrorToast(exception)
            }
        }
    }

    /** Presents recoverable outcomes. */
    private fun showMessage(message: Int): AlertDialog {
        var result: AlertDialog? = null
        val binding = DialogQrMessageBinding.inflate(activity.layoutInflater)
        binding.root.setText(message)
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.scan_qr_code) { result = it }
            }
        return result!!
    }

    /** Cancels the current scan and dismisses its dialog. */
    fun cancel() {
        scanJob?.cancel()
        scanJob = null
        dialog?.dismiss()
        dialog = null
    }
}
