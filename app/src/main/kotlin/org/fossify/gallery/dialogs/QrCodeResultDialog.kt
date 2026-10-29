package org.fossify.gallery.dialogs

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogQrResultBinding

/** Treats decoded content as text and offers only explicit copy and web-link actions. */
class QrCodeResultDialog(activity: BaseSimpleActivity, results: List<String>) {
    var dialog: AlertDialog? = null
        private set

    init {
        val binding = DialogQrResultBinding.inflate(activity.layoutInflater)
        var index = 0

        // Keep each code separate so actions always target one result.
        fun showResult() {
            val text = results[index]
            binding.qrResultText.text = text
            binding.qrResultPosition.text = activity.getString(R.string.qr_code_position, index + 1, results.size)
            binding.qrResultPosition.visibility = if (results.size > 1) View.VISIBLE else View.GONE
            val uri = Uri.parse(text)
            val isWebLink = text.none { it.isWhitespace() } &&
                (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) && !uri.host.isNullOrEmpty()
            dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.visibility = if (isWebLink) View.VISIBLE else View.GONE
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.qr_copy_text, null)
            .setNegativeButton(org.fossify.commons.R.string.close, null)
            .setNeutralButton(R.string.qr_open_link, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.qr_code_result) { alert ->
                    dialog = alert
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.qr_code_result), results[index]))
                        activity.toast(R.string.qr_text_copied)
                    }
                    alert.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        try {
                            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(results[index])).addCategory(Intent.CATEGORY_BROWSABLE))
                        } catch (_: ActivityNotFoundException) {
                            activity.toast(R.string.qr_link_unavailable)
                        }
                    }
                    binding.qrNextResult.visibility = if (results.size > 1) View.VISIBLE else View.GONE
                    binding.qrNextResult.setOnClickListener {
                        index = (index + 1) % results.size
                        showResult()
                    }
                    showResult()
                }
            }
    }
}
