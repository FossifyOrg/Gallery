package org.fossify.gallery.activities

import android.graphics.Bitmap
import android.view.View
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseCropActivity : SimpleActivity() {
    private var bitmapCroppingJob: Job? = null

    abstract val cropImageView: CropImageView

    abstract fun onImageCropped(bitmap: Bitmap?, error: Exception?)

    private fun setCropProgressBarVisibility(visible: Boolean) {
        cropImageView
            .findViewById<View>(com.canhub.cropper.R.id.CropProgressBar)
            ?.isInvisible = visible.not()
    }

    protected fun cropImage() {
        setCropProgressBarVisibility(true)
        bitmapCroppingJob?.cancel()
        bitmapCroppingJob = lifecycleScope.launch(CoroutineExceptionHandler { _, t ->
            onImageCropped(bitmap = null, error = Exception(t))
        }) {
            val bitmap = withContext(Dispatchers.Default) {
                cropImageView.getCroppedImage()
            }
            onImageCropped(bitmap, null)
        }.apply {
            lifecycleScope.launch(Dispatchers.Main) {
                setCropProgressBarVisibility(false)
            }
        }
    }
}
