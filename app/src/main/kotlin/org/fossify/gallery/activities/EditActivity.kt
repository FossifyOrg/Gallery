package org.fossify.gallery.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.widget.RelativeLayout
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.canhub.cropper.CropImageView
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import org.fossify.commons.dialogs.ColorPickerDialog
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.checkAppSideloading
import org.fossify.commons.extensions.getCompressionFormat
import org.fossify.commons.extensions.getFileOutputStream
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.getRealPathFromURI
import org.fossify.commons.extensions.isGone
import org.fossify.commons.extensions.isPathOnOTG
import org.fossify.commons.extensions.isVisible
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.commons.extensions.onSeekBarChangeListener
import org.fossify.commons.extensions.rescanPaths
import org.fossify.commons.extensions.sharePathIntent
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.REAL_FILE_PATH
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.FileDirItem
import org.fossify.gallery.BuildConfig
import org.fossify.gallery.R
import org.fossify.gallery.adapters.FiltersAdapter
import org.fossify.gallery.databinding.ActivityEditBinding
import org.fossify.gallery.dialogs.OtherAspectRatioDialog
import org.fossify.gallery.dialogs.ResizeDialog
import org.fossify.gallery.dialogs.SaveAsDialog
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.ensureWritablePath
import org.fossify.gallery.extensions.fixDateTaken
import org.fossify.gallery.extensions.getCompressionFormatFromUri
import org.fossify.gallery.extensions.openEditor
import org.fossify.gallery.extensions.proposeNewFilePath
import org.fossify.gallery.extensions.readExif
import org.fossify.gallery.extensions.resolveUriScheme
import org.fossify.gallery.extensions.showContentDescriptionOnLongClick
import org.fossify.gallery.extensions.writeBitmapToCache
import org.fossify.gallery.extensions.writeExif
import org.fossify.gallery.helpers.ASPECT_RATIO_FOUR_THREE
import org.fossify.gallery.helpers.ASPECT_RATIO_FREE
import org.fossify.gallery.helpers.ASPECT_RATIO_ONE_ONE
import org.fossify.gallery.helpers.ASPECT_RATIO_OTHER
import org.fossify.gallery.helpers.ASPECT_RATIO_SIXTEEN_NINE
import org.fossify.gallery.helpers.ColorModeHelper
import org.fossify.gallery.helpers.FilterThumbnailsManager
import org.fossify.gallery.helpers.getPermissionToRequest
import org.fossify.gallery.models.FilterItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max

class EditActivity : BaseCropActivity() {
    companion object {
        init {
            System.loadLibrary("NativeImageProcessor")
        }

        private const val ASPECT_X = "aspectX"
        private const val ASPECT_Y = "aspectY"
        private const val CROP = "crop"

        // constants for bottom primary action groups
        private const val PRIMARY_ACTION_NONE = 0
        private const val PRIMARY_ACTION_FILTER = 1
        private const val PRIMARY_ACTION_CROP_ROTATE = 2
        private const val PRIMARY_ACTION_DRAW = 3

        private const val CROP_ROTATE_NONE = 0
        private const val CROP_ROTATE_ASPECT_RATIO = 1
    }

    private lateinit var saveUri: Uri
    private var uri: Uri? = null
    private var resizeWidth = 0
    private var resizeHeight = 0
    private var drawColor = 0
    private var lastOtherAspectRatio: Pair<Float, Float>? = null
    private var currPrimaryAction = PRIMARY_ACTION_NONE
    private var currCropRotateAction = CROP_ROTATE_ASPECT_RATIO
    private var currAspectRatio = ASPECT_RATIO_FREE
    private var isCropIntent = false
    private var isEditingWithThirdParty = false
    private var isSharingBitmap = false
    private var wasDrawCanvasPositioned = false
    private var oldExif: ExifInterface? = null
    private var filterInitialBitmap: Bitmap? = null
    private var originalUri: Uri? = null
    private val binding by viewBinding(ActivityEditBinding::inflate)

    private var overwriteRequested = false

    override val cropImageView: CropImageView
        get() = binding.cropImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(
            padBottomSystem = listOf(binding.bottomEditorPrimaryActions.root)
        )

        if (checkAppSideloading()) {
            return
        }

        setupOptionsMenu()
        handlePermission(getPermissionToRequest()) {
            if (!it) {
                toast(org.fossify.commons.R.string.no_storage_permissions)
                finish()
            }
            initEditActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        isEditingWithThirdParty = false
        binding.bottomEditorDrawActions.bottomDrawWidth.setColors(getProperTextColor(), getProperPrimaryColor(), getProperBackgroundColor())
        setupTopAppBar(binding.editorAppbar, NavigationIcon.Arrow)
    }

    override fun onStop() {
        super.onStop()
        if (isEditingWithThirdParty) {
            finish()
        }
    }

    private fun setupOptionsMenu() {
        binding.editorToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save_as -> startSaveFlow(overwrite = false)
                R.id.overwrite_original -> startSaveFlow(overwrite = true)
                R.id.edit -> editWith()
                R.id.share -> shareImage()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun initEditActivity() {
        if (intent.data == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        uri = intent.data!!
        originalUri = uri
        if (uri!!.scheme != "file" && uri!!.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        val extras = intent.extras
        if (extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras!!.getString(REAL_FILE_PATH)
            uri = when {
                isPathOnOTG(realPath!!) -> uri
                realPath.startsWith("file:/") -> realPath.toUri()
                else -> Uri.fromFile(File(realPath))
            }
        } else {
            (getRealPathFromURI(uri!!))?.apply {
                uri = Uri.fromFile(File(this))
            }
        }

        saveUri = when {
            extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true
                    && extras.get(MediaStore.EXTRA_OUTPUT) is Uri -> extras.get(MediaStore.EXTRA_OUTPUT) as Uri
            else -> uri!!
        }

        isCropIntent = extras?.get(CROP) == "true"
        if (isCropIntent) {
            binding.bottomEditorPrimaryActions.root.beGone()
            (binding.bottomEditorCropRotateActions.root.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1)
            binding.editorToolbar.menu.findItem(R.id.overwrite_original).isVisible = false
        }

        loadDefaultImageView()
        setupBottomActions()

        if (config.lastEditorCropAspectRatio == ASPECT_RATIO_OTHER) {
            if (config.lastEditorCropOtherAspectRatioX == 0f) {
                config.lastEditorCropOtherAspectRatioX = 1f
            }

            if (config.lastEditorCropOtherAspectRatioY == 0f) {
                config.lastEditorCropOtherAspectRatioY = 1f
            }

            lastOtherAspectRatio = Pair(config.lastEditorCropOtherAspectRatioX, config.lastEditorCropOtherAspectRatioY)
        }
        updateAspectRatio(config.lastEditorCropAspectRatio)
        binding.cropImageView.guidelines = CropImageView.Guidelines.ON
        binding.bottomAspectRatios.root.beVisible()
    }

    override fun onDestroy() {
        super.onDestroy()
        ColorModeHelper.resetColorMode(this)
    }

    private fun loadDefaultImageView() {
        binding.defaultImageView.beVisible()
        binding.cropImageView.beGone()
        binding.editorDrawCanvas.beGone()

        val options = RequestOptions()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .apply(options)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>, isFirstResource: Boolean): Boolean {
                    ColorModeHelper.resetColorMode(this@EditActivity)
                    if (uri != originalUri) {
                        uri = originalUri
                        Handler().post {
                            loadDefaultImageView()
                        }
                    }
                    return false
                }

                override fun onResourceReady(
                    bitmap: Bitmap,
                    model: Any,
                    target: Target<Bitmap>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    ColorModeHelper.setColorModeForImage(this@EditActivity, bitmap)
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                    if (filterInitialBitmap == null) {
                        loadCropImageView()
                        bottomCropRotateClicked()
                    }

                    if (filterInitialBitmap != null && currentFilter != null && currentFilter.filter.name != getString(org.fossify.commons.R.string.none)) {
                        binding.defaultImageView.onGlobalLayout {
                            applyFilter(currentFilter)
                        }
                    } else {
                        filterInitialBitmap = bitmap
                    }

                    if (isCropIntent) {
                        binding.bottomEditorPrimaryActions.bottomPrimaryFilter.beGone()
                        binding.bottomEditorPrimaryActions.bottomPrimaryDraw.beGone()
                    }

                    return false
                }
            }).into(binding.defaultImageView)
    }

    private fun loadCropImageView() {
        binding.defaultImageView.beGone()
        binding.editorDrawCanvas.beGone()
        binding.cropImageView.apply {
            beVisible()
            setImageUriAsync(uri)
            guidelines = CropImageView.Guidelines.ON

            if (isCropIntent && shouldCropSquare()) {
                currAspectRatio = ASPECT_RATIO_ONE_ONE
                setFixedAspectRatio(true)
                binding.bottomEditorCropRotateActions.bottomAspectRatio.beGone()
            }
        }
    }

    private fun loadDrawCanvas() {
        binding.defaultImageView.beGone()
        binding.cropImageView.beGone()
        binding.editorDrawCanvas.beVisible()

        if (!wasDrawCanvasPositioned) {
            wasDrawCanvasPositioned = true
            binding.editorDrawCanvas.onGlobalLayout {
                ensureBackgroundThread {
                    fillCanvasBackground()
                }
            }
        }
    }

    private fun fillCanvasBackground() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_ARGB_8888)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()

        try {
            val builder = Glide.with(applicationContext)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(binding.editorDrawCanvas.width, binding.editorDrawCanvas.height)

            val bitmap = builder.get()
            runOnUiThread {
                binding.editorDrawCanvas.apply {
                    updateBackgroundBitmap(bitmap)
                    layoutParams.width = bitmap.width
                    layoutParams.height = bitmap.height
                    y = (height - bitmap.height) / 2f
                    requestLayout()
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun setOldExif() {
        oldExif = readExif(uri!!)
    }

    private fun startSaveFlow(overwrite: Boolean) {
        overwriteRequested = overwrite
        setOldExif()
        when {
            binding.cropImageView.isVisible() -> cropImage()
            binding.editorDrawCanvas.isVisible() -> saveDrawnImage()
            else -> saveFilteredImage(overwrite)
        }
    }

    private fun saveDrawnImage() {
        saveBitmap(
            overwrite = overwriteRequested,
            bitmap = binding.editorDrawCanvas.getBitmap()
        )
    }

    override fun onImageCropped(bitmap: Bitmap?, error: Exception?) {
        if (isFinishing || isDestroyed) return
        if (error != null || bitmap == null) {
            toast("${getString(R.string.image_editing_failed)}: ${error?.message}")
            return
        }

        setOldExif()

        if (isSharingBitmap) {
            isSharingBitmap = false
            shareBitmap(bitmap)
            return
        }

        if (isCropIntent) {
            resolveUriScheme(
                uri = saveUri,
                onPath = { saveBitmapToPath(bitmap, it, true) },
                onContentUri = {
                    saveBitmapToContentUri(bitmap, it, showSavingToast = true, isCropCommit = true)
                }
            )
            return
        }

        saveBitmap(overwriteRequested, bitmap, showSavingToast = true)
    }

    private fun getOriginalBitmap(): Bitmap {
        return Glide.with(applicationContext)
            .asBitmap()
            .load(uri)
            .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .get()
    }

    private fun withFilteredImage(callback: (Bitmap) -> Unit) {
        val currentFilter = getFiltersAdapter()?.getCurrentFilter()?.filter ?: return
        freeMemory()
        ensureBackgroundThread {
            try {
                val original = getOriginalBitmap()
                currentFilter.processFilter(original)
                callback(original)
            } catch (_: OutOfMemoryError) {
                toast(org.fossify.commons.R.string.out_of_memory_error)
            }
        }
    }

    private fun saveFilteredImage(overwrite: Boolean) {
        if (overwrite) {
            withFilteredImage {
                saveBitmap(true, it)
            }
        } else {
            resolveSaveAsPath { path ->
                withFilteredImage {
                    saveBitmapToPath(it, path, showSavingToast = true)
                }
            }
        }
    }

    private fun shareImage() {
        ensureBackgroundThread {
            when {
                binding.defaultImageView.isVisible() -> {
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                    if (currentFilter == null) {
                        toast(org.fossify.commons.R.string.unknown_error_occurred)
                        return@ensureBackgroundThread
                    }

                    val originalBitmap = getOriginalBitmap()
                    currentFilter.filter.processFilter(originalBitmap)
                    shareBitmap(originalBitmap)
                }

                binding.cropImageView.isVisible() -> {
                    isSharingBitmap = true
                    runOnUiThread {
                        cropImage()
                    }
                }

                binding.editorDrawCanvas.isVisible() -> shareBitmap(binding.editorDrawCanvas.getBitmap())
            }
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        writeBitmapToCache(saveUri, bitmap) {
            if (it != null) {
                sharePathIntent(it, BuildConfig.APPLICATION_ID)
            } else {
                toast(org.fossify.commons.R.string.unknown_error_occurred)
            }
        }
    }

    private fun getFiltersAdapter(): FiltersAdapter? {
        return binding.bottomEditorFilterActions.bottomActionsFilterList.adapter as? FiltersAdapter
    }

    private fun setupBottomActions() {
        setupPrimaryActionButtons()
        setupCropRotateActionButtons()
        setupAspectRatioButtons()
        setupDrawButtons()
    }

    private fun setupPrimaryActionButtons() {
        binding.bottomEditorPrimaryActions.bottomPrimaryFilter.setOnClickListener {
            bottomFilterClicked()
        }

        binding.bottomEditorPrimaryActions.bottomPrimaryCropRotate.setOnClickListener {
            bottomCropRotateClicked()
        }

        binding.bottomEditorPrimaryActions.bottomPrimaryDraw.setOnClickListener {
            bottomDrawClicked()
        }
        arrayOf(
            binding.bottomEditorPrimaryActions.bottomPrimaryFilter,
            binding.bottomEditorPrimaryActions.bottomPrimaryCropRotate,
            binding.bottomEditorPrimaryActions.bottomPrimaryDraw
        ).forEach {
            it.showContentDescriptionOnLongClick()
        }
    }

    private fun bottomFilterClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_FILTER) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_FILTER
        }
        updatePrimaryActionButtons()
    }

    private fun bottomCropRotateClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_CROP_ROTATE
        }
        updatePrimaryActionButtons()
    }

    private fun bottomDrawClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_DRAW) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_DRAW
        }
        updatePrimaryActionButtons()
    }

    private fun setupCropRotateActionButtons() {
        binding.bottomEditorCropRotateActions.bottomRotate.setOnClickListener {
            binding.cropImageView.rotateImage(90)
        }

        binding.bottomEditorCropRotateActions.bottomResize.beGoneIf(isCropIntent)
        binding.bottomEditorCropRotateActions.bottomResize.setOnClickListener {
            resizeImage()
        }

        binding.bottomEditorCropRotateActions.bottomFlipHorizontally.setOnClickListener {
            binding.cropImageView.flipImageHorizontally()
        }

        binding.bottomEditorCropRotateActions.bottomFlipVertically.setOnClickListener {
            binding.cropImageView.flipImageVertically()
        }

        binding.bottomEditorCropRotateActions.bottomAspectRatio.setOnClickListener {
            currCropRotateAction = if (currCropRotateAction == CROP_ROTATE_ASPECT_RATIO) {
                binding.cropImageView.guidelines = CropImageView.Guidelines.OFF
                binding.bottomAspectRatios.root.beGone()
                CROP_ROTATE_NONE
            } else {
                binding.cropImageView.guidelines = CropImageView.Guidelines.ON
                binding.bottomAspectRatios.root.beVisible()
                CROP_ROTATE_ASPECT_RATIO
            }
            updateCropRotateActionButtons()
        }

        arrayOf(
            binding.bottomEditorCropRotateActions.bottomRotate,
            binding.bottomEditorCropRotateActions.bottomResize,
            binding.bottomEditorCropRotateActions.bottomFlipHorizontally,
            binding.bottomEditorCropRotateActions.bottomFlipVertically,
            binding.bottomEditorCropRotateActions.bottomAspectRatio
        ).forEach {
            it.showContentDescriptionOnLongClick()
        }
    }

    private fun setupAspectRatioButtons() {
        binding.bottomAspectRatios.bottomAspectRatioFree.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FREE)
        }

        binding.bottomAspectRatios.bottomAspectRatioOneOne.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_ONE_ONE)
        }

        binding.bottomAspectRatios.bottomAspectRatioFourThree.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FOUR_THREE)
        }

        binding.bottomAspectRatios.bottomAspectRatioSixteenNine.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_SIXTEEN_NINE)
        }

        binding.bottomAspectRatios.bottomAspectRatioOther.setOnClickListener {
            OtherAspectRatioDialog(this, lastOtherAspectRatio) {
                lastOtherAspectRatio = it
                config.lastEditorCropOtherAspectRatioX = it.first
                config.lastEditorCropOtherAspectRatioY = it.second
                updateAspectRatio(ASPECT_RATIO_OTHER)
            }
        }

        updateAspectRatioButtons()
    }

    private fun setupDrawButtons() {
        updateDrawColor(config.lastEditorDrawColor)
        binding.bottomEditorDrawActions.bottomDrawWidth.progress = config.lastEditorBrushSize
        updateBrushSize(config.lastEditorBrushSize)

        binding.bottomEditorDrawActions.bottomDrawColorClickable.setOnClickListener {
            ColorPickerDialog(this, drawColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    updateDrawColor(color)
                }
            }
        }

        binding.bottomEditorDrawActions.bottomDrawWidth.onSeekBarChangeListener {
            config.lastEditorBrushSize = it
            updateBrushSize(it)
        }

        binding.bottomEditorDrawActions.bottomDrawUndo.setOnClickListener {
            binding.editorDrawCanvas.undo()
        }
    }

    private fun updateBrushSize(percent: Int) {
        binding.editorDrawCanvas.updateBrushSize(percent)
        val scale = max(0.03f, percent / 100f)
        binding.bottomEditorDrawActions.bottomDrawColor.scaleX = scale
        binding.bottomEditorDrawActions.bottomDrawColor.scaleY = scale
    }

    private fun updatePrimaryActionButtons() {
        if (binding.cropImageView.isGone() && currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            loadCropImageView()
        } else if (binding.defaultImageView.isGone() && currPrimaryAction == PRIMARY_ACTION_FILTER) {
            loadDefaultImageView()
        } else if (binding.editorDrawCanvas.isGone() && currPrimaryAction == PRIMARY_ACTION_DRAW) {
            loadDrawCanvas()
        }

        arrayOf(
            binding.bottomEditorPrimaryActions.bottomPrimaryFilter,
            binding.bottomEditorPrimaryActions.bottomPrimaryCropRotate,
            binding.bottomEditorPrimaryActions.bottomPrimaryDraw
        ).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val currentPrimaryActionButton = when (currPrimaryAction) {
            PRIMARY_ACTION_FILTER -> binding.bottomEditorPrimaryActions.bottomPrimaryFilter
            PRIMARY_ACTION_CROP_ROTATE -> binding.bottomEditorPrimaryActions.bottomPrimaryCropRotate
            PRIMARY_ACTION_DRAW -> binding.bottomEditorPrimaryActions.bottomPrimaryDraw
            else -> null
        }

        currentPrimaryActionButton?.applyColorFilter(getProperPrimaryColor())
        binding.bottomEditorFilterActions.root.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_FILTER)
        binding.bottomEditorCropRotateActions.root.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE)
        binding.bottomEditorDrawActions.root.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_DRAW)

        if (currPrimaryAction == PRIMARY_ACTION_FILTER && binding.bottomEditorFilterActions.bottomActionsFilterList.adapter == null) {
            ensureBackgroundThread {
                val thumbnailSize = resources.getDimension(R.dimen.bottom_filters_thumbnail_size).toInt()

                val bitmap = try {
                    Glide.with(this)
                        .asBitmap()
                        .load(uri).listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>, isFirstResource: Boolean): Boolean {
                                showErrorToast(e.toString())
                                return false
                            }

                            override fun onResourceReady(
                                resource: Bitmap,
                                model: Any,
                                target: Target<Bitmap>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ) = false
                        })
                        .submit(thumbnailSize, thumbnailSize)
                        .get()
                } catch (e: GlideException) {
                    showErrorToast(e)
                    finish()
                    return@ensureBackgroundThread
                }

                runOnUiThread {
                    val filterThumbnailsManager = FilterThumbnailsManager()
                    filterThumbnailsManager.clearThumbs()

                    val noFilter = Filter(getString(org.fossify.commons.R.string.none))
                    filterThumbnailsManager.addThumb(FilterItem(bitmap, noFilter))

                    FilterPack.getFilterPack(this).forEach {
                        val filterItem = FilterItem(bitmap, it)
                        filterThumbnailsManager.addThumb(filterItem)
                    }

                    val filterItems = filterThumbnailsManager.processThumbs()
                    val adapter = FiltersAdapter(applicationContext, filterItems) {
                        val layoutManager = binding.bottomEditorFilterActions.bottomActionsFilterList.layoutManager as LinearLayoutManager
                        applyFilter(filterItems[it])

                        if (it == layoutManager.findLastCompletelyVisibleItemPosition() || it == layoutManager.findLastVisibleItemPosition()) {
                            binding.bottomEditorFilterActions.bottomActionsFilterList.smoothScrollBy(thumbnailSize, 0)
                        } else if (it == layoutManager.findFirstCompletelyVisibleItemPosition() || it == layoutManager.findFirstVisibleItemPosition()) {
                            binding.bottomEditorFilterActions.bottomActionsFilterList.smoothScrollBy(-thumbnailSize, 0)
                        }
                    }

                    binding.bottomEditorFilterActions.bottomActionsFilterList.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            }
        }

        if (currPrimaryAction != PRIMARY_ACTION_CROP_ROTATE) {
            binding.bottomAspectRatios.root.beGone()
            currCropRotateAction = CROP_ROTATE_NONE
        }
        updateCropRotateActionButtons()
    }

    private fun applyFilter(filterItem: FilterItem) {
        val newBitmap = Bitmap.createBitmap(filterInitialBitmap!!)
        binding.defaultImageView.setImageBitmap(filterItem.filter.processFilter(newBitmap))
    }

    private fun updateAspectRatio(aspectRatio: Int) {
        currAspectRatio = aspectRatio
        config.lastEditorCropAspectRatio = aspectRatio
        updateAspectRatioButtons()

        binding.cropImageView.apply {
            if (aspectRatio == ASPECT_RATIO_FREE) {
                setFixedAspectRatio(false)
            } else {
                val newAspectRatio = when (aspectRatio) {
                    ASPECT_RATIO_ONE_ONE -> Pair(1f, 1f)
                    ASPECT_RATIO_FOUR_THREE -> Pair(4f, 3f)
                    ASPECT_RATIO_SIXTEEN_NINE -> Pair(16f, 9f)
                    else -> Pair(lastOtherAspectRatio!!.first, lastOtherAspectRatio!!.second)
                }

                setAspectRatio(newAspectRatio.first.toInt(), newAspectRatio.second.toInt())
            }
        }
    }

    private fun updateAspectRatioButtons() {
        arrayOf(
            binding.bottomAspectRatios.bottomAspectRatioFree,
            binding.bottomAspectRatios.bottomAspectRatioOneOne,
            binding.bottomAspectRatios.bottomAspectRatioFourThree,
            binding.bottomAspectRatios.bottomAspectRatioSixteenNine,
            binding.bottomAspectRatios.bottomAspectRatioOther,
        ).forEach {
            it.setTextColor(Color.WHITE)
        }

        val currentAspectRatioButton = when (currAspectRatio) {
            ASPECT_RATIO_FREE -> binding.bottomAspectRatios.bottomAspectRatioFree
            ASPECT_RATIO_ONE_ONE -> binding.bottomAspectRatios.bottomAspectRatioOneOne
            ASPECT_RATIO_FOUR_THREE -> binding.bottomAspectRatios.bottomAspectRatioFourThree
            ASPECT_RATIO_SIXTEEN_NINE -> binding.bottomAspectRatios.bottomAspectRatioSixteenNine
            else -> binding.bottomAspectRatios.bottomAspectRatioOther
        }

        currentAspectRatioButton.setTextColor(getProperPrimaryColor())
    }

    private fun updateCropRotateActionButtons() {
        arrayOf(binding.bottomEditorCropRotateActions.bottomAspectRatio).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val primaryActionView = when (currCropRotateAction) {
            CROP_ROTATE_ASPECT_RATIO -> binding.bottomEditorCropRotateActions.bottomAspectRatio
            else -> null
        }

        primaryActionView?.applyColorFilter(getProperPrimaryColor())
    }

    private fun updateDrawColor(color: Int) {
        drawColor = color
        binding.bottomEditorDrawActions.bottomDrawColor.applyColorFilter(color)
        config.lastEditorDrawColor = color
        binding.editorDrawCanvas.updateColor(color)
    }

    private fun resizeImage() {
        val point = getAreaSize()
        if (point == null) {
            toast(org.fossify.commons.R.string.unknown_error_occurred)
            return
        }

        ResizeDialog(this, point) {
            resizeWidth = it.x
            resizeHeight = it.y
            cropImage()
        }
    }

    private fun shouldCropSquare(): Boolean {
        val extras = intent.extras
        return if (extras != null && extras.containsKey(ASPECT_X) && extras.containsKey(ASPECT_Y)) {
            extras.getInt(ASPECT_X) == extras.getInt(ASPECT_Y)
        } else {
            false
        }
    }

    private fun getAreaSize(): Point? {
        val rect = binding.cropImageView.cropRect ?: return null
        val rotation = binding.cropImageView.rotatedDegrees
        return if (rotation == 0 || rotation == 180) {
            Point(rect.width(), rect.height())
        } else {
            Point(rect.height(), rect.width())
        }
    }

    private fun resolveSaveAsPath(callback: (String) -> Unit) {
        runOnUiThread {
            resolveUriScheme(
                uri = saveUri,
                onPath = {
                    SaveAsDialog(this, it, true, callback = callback)
                },
                onContentUri = {
                    val (path, append) = proposeNewFilePath(it)
                    SaveAsDialog(this, path, append, callback = callback)
                }
            )
        }
    }

    private fun saveBitmap(overwrite: Boolean, bitmap: Bitmap, showSavingToast: Boolean = true) {
        if (overwrite) {
            resolveUriScheme(
                uri = saveUri,
                onPath = { path ->
                    ensureWritablePath(targetPath = path, confirmOverwrite = false) {
                        saveBitmapToPath(bitmap, it, showSavingToast)
                    }
                },
                onContentUri = { contentUri ->
                    saveBitmapToContentUri(bitmap, contentUri, showSavingToast, isCropCommit = false)
                }
            )
        } else {
            resolveSaveAsPath { path ->
                saveBitmapToPath(bitmap, path, showSavingToast)
            }
        }
    }

    private fun finishCropResultForContent(uri: Uri) {
        val result = Intent().apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private fun freeMemory() {
        // clean up everything to free as much memory as possible
        binding.defaultImageView.setImageResource(0)
        binding.cropImageView.setImageBitmap(null)
        binding.bottomEditorFilterActions.bottomActionsFilterList.adapter = null
        binding.bottomEditorFilterActions.bottomActionsFilterList.beGone()
    }

    private fun saveBitmapToPath(bitmap: Bitmap, path: String, showSavingToast: Boolean) {
        try {
            ensureBackgroundThread {
                val file = File(path)
                val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
                try {
                    val out = FileOutputStream(file)
                    saveBitmapToFile(file, bitmap, out, showSavingToast)
                } catch (e: Exception) {
                    getFileOutputStream(fileDirItem, true) {
                        if (it != null) {
                            saveBitmapToFile(file, bitmap, it, showSavingToast)
                        } else {
                            toast(R.string.image_editing_failed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        } catch (e: OutOfMemoryError) {
            toast(org.fossify.commons.R.string.out_of_memory_error)
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap, out: OutputStream, showSavingToast: Boolean) {
        if (showSavingToast) {
            toast(org.fossify.commons.R.string.saving)
        }

        out.use {
            if (resizeWidth > 0 && resizeHeight > 0) {
                val resized = bitmap.scale(resizeWidth, resizeHeight, false)
                resized.compress(file.absolutePath.getCompressionFormat(), 90, out)
            } else {
                bitmap.compress(file.absolutePath.getCompressionFormat(), 90, out)
            }
        }

        writeExif(oldExif, file.toUri())
        setResult(RESULT_OK, intent)
        scanFinalPath(file.absolutePath)
    }

    private fun saveBitmapToContentUri(
        bitmap: Bitmap,
        uri: Uri,
        showSavingToast: Boolean,
        isCropCommit: Boolean
    ) {
        if (showSavingToast) {
            toast(org.fossify.commons.R.string.saving)
        }

        ensureBackgroundThread {
            var out: OutputStream? = null
            try {
                out = contentResolver.openOutputStream(uri, "wt")
                    ?: contentResolver.openOutputStream(uri)
                if (out == null) {
                    val (path, append) = proposeNewFilePath(uri)
                    runOnUiThread {
                        SaveAsDialog(this, path, append) { path ->
                            saveBitmapToPath(bitmap, path, showSavingToast)
                        }
                    }
                    return@ensureBackgroundThread
                }

                val quality = if (isCropCommit) 100 else 90
                bitmap.compress(getCompressionFormatFromUri(uri), quality, out)
                out.flush()
                writeExif(oldExif, uri)

                runOnUiThread {
                    if (isCropCommit) {
                        finishCropResultForContent(uri)
                    } else {
                        setResult(RESULT_OK, intent)
                        toast(org.fossify.commons.R.string.file_saved)
                        finish()
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
            } finally {
                try { out?.close() } catch (_: Exception) {}
            }
        }
    }

    private fun editWith() {
        openEditor(uri.toString(), true)
        isEditingWithThirdParty = true
    }

    private fun scanFinalPath(path: String) {
        val paths = arrayListOf(path)
        rescanPaths(paths) {
            fixDateTaken(paths, false)
            setResult(RESULT_OK, intent)
            toast(org.fossify.commons.R.string.file_saved)
            finish()
        }
    }
}
