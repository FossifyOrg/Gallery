package org.fossify.gallery.fragments

import android.content.res.Configuration
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.bumptech.glide.Glide
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.gallery.R
import org.fossify.gallery.activities.VideoActivity
import org.fossify.gallery.databinding.PagerVideoItemBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.hasNavBar
import org.fossify.gallery.extensions.parseFileChannel
import org.fossify.gallery.helpers.Config
import org.fossify.gallery.helpers.FAST_FORWARD_VIDEO_MS
import org.fossify.gallery.helpers.MEDIUM
import org.fossify.gallery.helpers.SHOULD_INIT_FRAGMENT
import org.fossify.gallery.models.Medium
import org.fossify.gallery.views.MediaSideScroll
import java.io.File
import java.io.FileInputStream

@UnstableApi
class VideoFragment : ViewPagerFragment(), TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener {
    private val PROGRESS = "progress"

    private var mIsFullscreen = false
    private var mWasFragmentInit = false
    private var mIsPanorama = false
    private var mIsFragmentVisible = false
    private var mIsDragged = false
    private var mWasVideoStarted = false
    private var mWasPlayerInited = false
    private var mWasLastPositionRestored = false
    private var mPlayOnPrepared = false
    private var mIsPlayerPrepared = false
    private var mCurrTime = 0
    private var mDuration = 0
    private var mPositionWhenInit = 0
    private var mPositionAtPause = 0L
    var mIsPlaying = false

    private var mExoPlayer: ExoPlayer? = null
    private var mVideoSize = Point(1, 1)
    private var mTimerHandler = Handler()

    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredBottomActions = true
    private var mStoredExtendedDetails = 0
    private var mStoredRememberLastVideoPosition = false

    private lateinit var mTimeHolder: View
    private lateinit var mBrightnessSideScroll: MediaSideScroll
    private lateinit var mVolumeSideScroll: MediaSideScroll
    private lateinit var binding: PagerVideoItemBinding
    private lateinit var mView: View
    private lateinit var mMedium: Medium
    private lateinit var mConfig: Config
    private lateinit var mTextureView: TextureView
    private lateinit var mCurrTimeView: TextView
    private lateinit var mPlayPauseButton: ImageView
    private lateinit var mSeekBar: SeekBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val activity = requireActivity()
        val arguments = requireArguments()

        mMedium = arguments.getSerializable(MEDIUM) as Medium
        mConfig = context.config
        binding = PagerVideoItemBinding.inflate(inflater, container, false).apply {
            panoramaOutline.setOnClickListener { openPanorama() }
            bottomVideoTimeHolder.videoCurrTime.setOnClickListener { skip(false) }
            bottomVideoTimeHolder.videoDuration.setOnClickListener { skip(true) }
            videoHolder.setOnClickListener { toggleFullscreen() }
            videoPreview.setOnClickListener { toggleFullscreen() }
            videoSurfaceFrame.controller.settings.swallowDoubleTaps = true

            videoPlayOutline.setOnClickListener {
                if (mConfig.openVideosOnSeparateScreen) {
                    launchVideoPlayer()
                } else {
                    togglePlayPause()
                }
            }

            mPlayPauseButton = bottomVideoTimeHolder.videoTogglePlayPause
            mPlayPauseButton.setOnClickListener {
                togglePlayPause()
            }

            mSeekBar = bottomVideoTimeHolder.videoSeekbar
            mSeekBar.setOnSeekBarChangeListener(this@VideoFragment)
            // adding an empty click listener just to avoid ripple animation at toggling fullscreen
            mSeekBar.setOnClickListener { }

            mTimeHolder = bottomVideoTimeHolder.videoTimeHolder
            mCurrTimeView = bottomVideoTimeHolder.videoCurrTime
            mBrightnessSideScroll = videoBrightnessController
            mVolumeSideScroll = videoVolumeController
            mTextureView = videoSurface
            mTextureView.surfaceTextureListener = this@VideoFragment

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (!mConfig.allowInstantChange) {
                        toggleFullscreen()
                        return true
                    }

                    val viewWidth = root.width
                    val instantWidth = viewWidth / 7
                    val clickedX = e.rawX
                    when {
                        clickedX <= instantWidth -> listener?.goToPrevItem()
                        clickedX >= viewWidth - instantWidth -> listener?.goToNextItem()
                        else -> toggleFullscreen()
                    }
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    handleDoubleTap(e.rawX)
                    return true
                }
            })

            videoPreview.setOnTouchListener { view, event ->
                handleEvent(event)
                false
            }

            videoSurfaceFrame.setOnTouchListener { view, event ->
                if (videoSurfaceFrame.controller.state.zoom == 1f) {
                    handleEvent(event)
                }

                gestureDetector.onTouchEvent(event)
                false
            }
        }
        mView = binding.root

        if (!arguments.getBoolean(SHOULD_INIT_FRAGMENT, true)) {
            return mView
        }

        storeStateVariables()
        Glide.with(context).load(mMedium.path).into(binding.videoPreview)

        // setMenuVisibility is not called at VideoActivity (third party intent)
        if (!mIsFragmentVisible && activity is VideoActivity) {
            mIsFragmentVisible = true
        }

        mIsFullscreen = activity.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        initTimeHolder()
        // checkIfPanorama() TODO: Implement panorama using a FOSS library

        ensureBackgroundThread {
            activity.getVideoResolution(mMedium.path)?.apply {
                mVideoSize.x = x
                mVideoSize.y = y
            }
        }

        if (mIsPanorama) {
            binding.apply {
                panoramaOutline.beVisible()
                videoPlayOutline.beGone()
                mVolumeSideScroll.beGone()
                mBrightnessSideScroll.beGone()
                Glide.with(context).load(mMedium.path).into(videoPreview)
            }
        }

        if (!mIsPanorama) {
            if (savedInstanceState != null) {
                mCurrTime = savedInstanceState.getInt(PROGRESS)
            }

            mWasFragmentInit = true
            setVideoSize()

            binding.apply {
                mBrightnessSideScroll.initialize(activity, slideInfo, true, container, singleTap = { x, y ->
                    if (mConfig.allowInstantChange) {
                        listener?.goToPrevItem()
                    } else {
                        toggleFullscreen()
                    }
                }, doubleTap = { x, y ->
                    doSkip(false)
                })

                mVolumeSideScroll.initialize(activity, slideInfo, false, container, singleTap = { x, y ->
                    if (mConfig.allowInstantChange) {
                        listener?.goToNextItem()
                    } else {
                        toggleFullscreen()
                    }
                }, doubleTap = { x, y ->
                    doSkip(true)
                })

                videoSurface.onGlobalLayout {
                    if (mIsFragmentVisible && mConfig.autoplayVideos && !mConfig.openVideosOnSeparateScreen) {
                        playVideo()
                    }
                }
            }
        }

        setupVideoDuration()
        if (mStoredRememberLastVideoPosition) {
            restoreLastVideoSavedPosition()
        }

        return mView
    }

    override fun onResume() {
        super.onResume()
        mConfig = requireContext().config      // make sure we get a new config, in case the user changed something in the app settings
        requireActivity().updateTextColors(binding.videoHolder)
        val allowVideoGestures = mConfig.allowVideoGestures
        mTextureView.beGoneIf(mConfig.openVideosOnSeparateScreen || mIsPanorama)
        binding.videoSurfaceFrame.beGoneIf(mTextureView.isGone())

        mVolumeSideScroll.beVisibleIf(allowVideoGestures && !mIsPanorama)
        mBrightnessSideScroll.beVisibleIf(allowVideoGestures && !mIsPanorama)

        checkExtendedDetails()
        initTimeHolder()
        storeStateVariables()
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        pauseVideo()
        if (mStoredRememberLastVideoPosition && mIsFragmentVisible && mWasVideoStarted) {
            saveVideoProgress()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity?.isChangingConfigurations == false) {
            cleanup()
        }
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (mIsFragmentVisible && !menuVisible) {
            pauseVideo()
        }

        mIsFragmentVisible = menuVisible
        if (mWasFragmentInit && menuVisible && mConfig.autoplayVideos && !mConfig.openVideosOnSeparateScreen) {
            playVideo()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initTimeHolder()
        checkExtendedDetails()
        binding.videoSurfaceFrame.onGlobalLayout {
            binding.videoSurfaceFrame.controller.resetState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PROGRESS, mCurrTime)
    }

    private fun storeStateVariables() {
        mConfig.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredExtendedDetails = extendedDetails
            mStoredBottomActions = bottomActions
            mStoredRememberLastVideoPosition = rememberLastVideoPosition
        }
    }

    private fun saveVideoProgress() {
        if (!videoEnded()) {
            if (mExoPlayer != null) {
                mConfig.saveLastVideoPosition(mMedium.path, mExoPlayer!!.currentPosition.toInt() / 1000)
            } else {
                mConfig.saveLastVideoPosition(mMedium.path, mPositionAtPause.toInt() / 1000)
            }
        }
    }

    private fun restoreLastVideoSavedPosition() {
        val pos = mConfig.getLastVideoPosition(mMedium.path)
        if (pos > 0) {
            mPositionAtPause = pos * 1000L
            setPosition(pos)
        }
    }

    private fun setupTimeHolder() {
        mSeekBar.max = mDuration
        binding.bottomVideoTimeHolder.videoDuration.text = mDuration.getFormattedDuration()
        setupTimer()
    }

    private fun setupTimer() {
        activity?.runOnUiThread(object : Runnable {
            override fun run() {
                if (mExoPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = (mExoPlayer!!.currentPosition / 1000).toInt()
                    mSeekBar.progress = mCurrTime
                    mCurrTimeView.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun initExoPlayer() {
        if (activity == null || mConfig.openVideosOnSeparateScreen || mIsPanorama || mExoPlayer != null) {
            return
        }

        val isContentUri = mMedium.path.startsWith("content://")
        val uri = if (isContentUri) Uri.parse(mMedium.path) else Uri.fromFile(File(mMedium.path))
        val dataSpec = DataSpec(uri)
        val fileDataSource = if (isContentUri) {
            ContentDataSource(requireContext())
        } else {
            FileDataSource()
        }

        try {
            fileDataSource.open(dataSpec)
        } catch (e: Exception) {
            fileDataSource.close()
            activity?.showErrorToast(e)
            return
        }

        val factory = DataSource.Factory { fileDataSource }
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(fileDataSource.uri!!))

        fileDataSource.close()

        mPlayOnPrepared = true

        mExoPlayer = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(requireContext()))
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
            .apply {
                if (mConfig.loopVideos && listener?.isSlideShowActive() == false) {
                    repeatMode = Player.REPEAT_MODE_ONE
                }
                setMediaSource(mediaSource)
                setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(), false
                )
                prepare()

                if (mTextureView.surfaceTexture != null) {
                    setVideoSurface(Surface(mTextureView.surfaceTexture))
                }

                initListeners()
            }
    }

    private fun ExoPlayer.initListeners() {
        addListener(object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                @Player.DiscontinuityReason reason: Int
            ) {
                // Reset progress views when video loops.
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    mSeekBar.progress = 0
                    mCurrTimeView.text = 0.getFormattedDuration()
                }
            }

            override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> videoPrepared()
                    Player.STATE_ENDED -> videoCompleted()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                mVideoSize.x = videoSize.width
                mVideoSize.y = (videoSize.height / videoSize.pixelWidthHeightRatio).toInt()
                setVideoSize()
            }
        })
    }

    private fun launchVideoPlayer() {
        listener?.launchViewVideoIntent(mMedium.path)
    }

    private fun toggleFullscreen() {
        listener?.fragmentClicked()
    }

    private fun handleDoubleTap(x: Float) {
        val viewWidth = mView.width
        val instantWidth = viewWidth / 7
        when {
            x <= instantWidth -> doSkip(false)
            x >= viewWidth - instantWidth -> doSkip(true)
            else -> togglePlayPause()
        }
    }

    private fun checkExtendedDetails() {
        if (mConfig.showExtendedDetails) {
            binding.videoDetails.apply {
                beInvisible()   // make it invisible so we can measure it, but not show yet
                text = getMediumExtendedDetails(mMedium)
                onGlobalLayout {
                    if (isAdded) {
                        val realY = getExtendedDetailsY(height)
                        if (realY > 0) {
                            y = realY
                            beVisibleIf(text.isNotEmpty())
                            alpha = if (!mConfig.hideExtendedDetails || !mIsFullscreen) 1f else 0f
                        }
                    }
                }
            }
        } else {
            binding.videoDetails.beGone()
        }
    }

    private fun initTimeHolder() {
        var right = 0
        var bottom = requireContext().navigationBarHeight
        if (mConfig.bottomActions) {
            bottom += resources.getDimension(R.dimen.bottom_actions_height).toInt()
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && activity?.hasNavBar() == true) {
            right += requireActivity().navigationBarWidth
        }

        (mTimeHolder.layoutParams as RelativeLayout.LayoutParams).apply {
            bottomMargin = bottom
            rightMargin = right
        }
        mTimeHolder.beInvisibleIf(mIsFullscreen)
    }

    private fun checkIfPanorama() {
        try {
            val fis = FileInputStream(File(mMedium.path))
            fis.use {
                requireContext().parseFileChannel(mMedium.path, it.channel, 0, 0, 0) {
                    mIsPanorama = true
                }
            }
        } catch (ignored: Exception) {
        } catch (ignored: OutOfMemoryError) {
        }
    }

    private fun openPanorama() {
        TODO("Panorama is not yet implemented.")
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        mIsFullscreen = isFullscreen
        val newAlpha = if (isFullscreen) 0f else 1f
        if (!mIsFullscreen) {
            mTimeHolder.beVisible()
        }

        mSeekBar.setOnSeekBarChangeListener(if (mIsFullscreen) null else this)
        arrayOf(
            binding.bottomVideoTimeHolder.videoCurrTime,
            binding.bottomVideoTimeHolder.videoDuration,
            binding.bottomVideoTimeHolder.videoTogglePlayPause
        ).forEach {
            it.isClickable = !mIsFullscreen
        }

        mTimeHolder.animate().alpha(newAlpha).start()
        binding.videoDetails.apply {
            if (mStoredShowExtendedDetails && isVisible() && context != null && resources != null) {
                animate().y(getExtendedDetailsY(height))

                if (mStoredHideExtendedDetails) {
                    animate().alpha(newAlpha).start()
                }
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = context?.resources?.getDimension(org.fossify.commons.R.dimen.small_margin) ?: return 0f
        val fullscreenOffset = smallMargin + if (mIsFullscreen) 0 else requireContext().navigationBarHeight
        var actionsHeight = 0f
        if (!mIsFullscreen) {
            actionsHeight += resources.getDimension(R.dimen.video_player_play_pause_size)
            if (mConfig.bottomActions) {
                actionsHeight += resources.getDimension(R.dimen.bottom_actions_height)
            }
        }
        return requireContext().realScreenSize.y - height - actionsHeight - fullscreenOffset
    }

    private fun skip(forward: Boolean) {
        if (mIsPanorama) {
            return
        } else if (mExoPlayer == null) {
            playVideo()
            return
        }

        mPositionAtPause = 0L
        doSkip(forward)
    }

    private fun doSkip(forward: Boolean) {
        if (mExoPlayer == null) {
            return
        }

        val curr = mExoPlayer!!.currentPosition
        val newProgress = if (forward) curr + FAST_FORWARD_VIDEO_MS else curr - FAST_FORWARD_VIDEO_MS
        val roundProgress = Math.round(newProgress / 1000f)
        val limitedProgress = Math.max(Math.min(mExoPlayer!!.duration.toInt() / 1000, roundProgress), 0)
        setPosition(limitedProgress)
        if (!mIsPlaying) {
            togglePlayPause()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            if (mExoPlayer != null) {
                if (!mWasPlayerInited) {
                    mPositionWhenInit = progress
                }
                setPosition(progress)
            }

            if (mExoPlayer == null) {
                mPositionAtPause = progress * 1000L
                playVideo()
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        if (mExoPlayer == null) {
            return
        }

        mExoPlayer!!.playWhenReady = false
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (mIsPanorama) {
            openPanorama()
            return
        }

        if (mExoPlayer == null) {
            return
        }

        if (mIsPlaying) {
            mExoPlayer!!.playWhenReady = true
        } else {
            playVideo()
        }

        mIsDragged = false
    }

    private fun togglePlayPause() {
        if (activity == null || !isAdded) {
            return
        }

        if (mIsPlaying) {
            pauseVideo()
        } else {
            playVideo()
        }
    }

    fun playVideo() {
        if (mExoPlayer == null) {
            initExoPlayer()
            return
        }

        if (binding.videoPreview.isVisible()) {
            binding.videoPreview.beGone()
            initExoPlayer()
        }

        val wasEnded = videoEnded()
        if (wasEnded) {
            setPosition(0)
        }

        if (mStoredRememberLastVideoPosition && !mWasLastPositionRestored) {
            mWasLastPositionRestored = true
            restoreLastVideoSavedPosition()
        }

        if (!wasEnded || !mConfig.loopVideos) {
            mPlayPauseButton.setImageResource(org.fossify.commons.R.drawable.ic_pause_outline_vector)
        }

        if (!mWasVideoStarted) {
            binding.videoPlayOutline.beGone()
            mPlayPauseButton.beVisible()
        }

        mWasVideoStarted = true
        if (mIsPlayerPrepared) {
            mIsPlaying = true
        }
        mExoPlayer?.playWhenReady = true
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        if (mExoPlayer == null) {
            return
        }

        mIsPlaying = false
        if (!videoEnded()) {
            mExoPlayer?.playWhenReady = false
        }

        mPlayPauseButton.setImageResource(org.fossify.commons.R.drawable.ic_play_outline_vector)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mPositionAtPause = mExoPlayer?.currentPosition ?: 0L
    }

    private fun videoEnded(): Boolean {
        val currentPos = mExoPlayer?.currentPosition ?: 0
        val duration = mExoPlayer?.duration ?: 0
        return currentPos != 0L && currentPos >= duration
    }

    private fun setPosition(seconds: Int) {
        mExoPlayer?.seekTo(seconds * 1000L)
        mSeekBar.progress = seconds
        mCurrTimeView.text = seconds.getFormattedDuration()

        if (!mIsPlaying) {
            mPositionAtPause = mExoPlayer?.currentPosition ?: 0L
        }
    }

    private fun setupVideoDuration() {
        ensureBackgroundThread {
            mDuration = context?.getDuration(mMedium.path) ?: 0

            activity?.runOnUiThread {
                setupTimeHolder()
                setPosition(0)
            }
        }
    }

    private fun videoPrepared() {
        if (mDuration == 0) {
            mDuration = (mExoPlayer!!.duration / 1000).toInt()
            setupTimeHolder()
            setPosition(mCurrTime)

            if (mIsFragmentVisible && (mConfig.autoplayVideos)) {
                playVideo()
            }
        }

        if (mPositionWhenInit != 0 && !mWasPlayerInited) {
            setPosition(mPositionWhenInit)
            mPositionWhenInit = 0
        }

        mIsPlayerPrepared = true
        if (mPlayOnPrepared && !mIsPlaying) {
            if (mPositionAtPause != 0L) {
                mExoPlayer?.seekTo(mPositionAtPause)
                mPositionAtPause = 0L
            }
            playVideo()
        }
        mWasPlayerInited = true
        mPlayOnPrepared = false
    }

    private fun videoCompleted() {
        if (!isAdded || mExoPlayer == null) {
            return
        }

        mCurrTime = (mExoPlayer!!.duration / 1000).toInt()
        if (listener?.videoEnded() == false && mConfig.loopVideos) {
            playVideo()
        } else {
            mSeekBar.progress = mSeekBar.max
            mCurrTimeView.text = mDuration.getFormattedDuration()
            pauseVideo()
        }
    }

    private fun cleanup() {
        pauseVideo()
        releaseExoPlayer()

        if (mWasFragmentInit) {
            mCurrTimeView.text = 0.getFormattedDuration()
            mSeekBar.progress = 0
            mTimerHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun releaseExoPlayer() {
        mIsPlayerPrepared = false
        mExoPlayer?.apply {
            stop()
            release()
        }
        mExoPlayer = null
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        mExoPlayer?.setVideoSurface(Surface(mTextureView.surfaceTexture))
    }

    private fun setVideoSize() {
        if (activity == null || mConfig.openVideosOnSeparateScreen) {
            return
        }

        val videoProportion = mVideoSize.x.toFloat() / mVideoSize.y.toFloat()
        val display = requireActivity().windowManager.defaultDisplay
        val screenWidth: Int
        val screenHeight: Int

        val realMetrics = DisplayMetrics()
        display.getRealMetrics(realMetrics)
        screenWidth = realMetrics.widthPixels
        screenHeight = realMetrics.heightPixels

        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()

        mTextureView.layoutParams.apply {
            if (videoProportion > screenProportion) {
                width = screenWidth
                height = (screenWidth.toFloat() / videoProportion).toInt()
            } else {
                width = (videoProportion * screenHeight.toFloat()).toInt()
                height = screenHeight
            }
            mTextureView.layoutParams = this
        }
    }
}
