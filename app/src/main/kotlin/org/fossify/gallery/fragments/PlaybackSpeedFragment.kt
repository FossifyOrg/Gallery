package org.fossify.gallery.fragments

import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.fossify.commons.extensions.*
import org.fossify.commons.views.MySeekBar
import org.fossify.commons.views.MyTextView
import org.fossify.gallery.R
import org.fossify.gallery.databinding.FragmentPlaybackSpeedBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.Config
import org.fossify.gallery.interfaces.PlaybackSpeedListener

class PlaybackSpeedFragment : BottomSheetDialogFragment() {
    private val MIN_PLAYBACK_SPEED = 0.25f
    private val MAX_PLAYBACK_SPEED = 3f
    private val MAX_PROGRESS = (MAX_PLAYBACK_SPEED * 100 + MIN_PLAYBACK_SPEED * 100).toInt()
    private val HALF_PROGRESS = MAX_PROGRESS / 2
    private val STEP = 0.05f

    private var seekBar: MySeekBar? = null
    private var listener: PlaybackSpeedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = requireContext().config
        val binding = FragmentPlaybackSpeedBinding.inflate(inflater, container, false)
        val background = ResourcesCompat.getDrawable(resources, org.fossify.commons.R.drawable.bottom_sheet_bg, requireContext().theme)
        (background as LayerDrawable).findDrawableByLayerId(org.fossify.commons.R.id.bottom_sheet_background)
            .applyColorFilter(requireContext().getProperBackgroundColor())

        binding.apply {
            seekBar = playbackSpeedSeekbar
            root.setBackgroundDrawable(background)
            requireContext().updateTextColors(playbackSpeedHolder)
            playbackSpeedSlow.applyColorFilter(requireContext().getProperTextColor())
            playbackSpeedFast.applyColorFilter(requireContext().getProperTextColor())
            playbackSpeedSlow.setOnClickListener { reduceSpeed() }
            playbackSpeedFast.setOnClickListener { increaseSpeed() }
            initSeekbar(playbackSpeedSeekbar, playbackSpeedLabel, config)
        }

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        return binding.root
    }

    private fun initSeekbar(seekbar: MySeekBar, speedLabel: MyTextView, config: Config) {
        val formattedValue = formatPlaybackSpeed(config.playbackSpeed)
        speedLabel.text = "${formattedValue}x"
        seekbar.max = MAX_PROGRESS

        val playbackSpeedProgress = config.playbackSpeedProgress
        if (playbackSpeedProgress == -1) {
            config.playbackSpeedProgress = HALF_PROGRESS
        }
        seekbar.progress = config.playbackSpeedProgress

        var lastUpdatedProgress = config.playbackSpeedProgress
        var lastUpdatedFormattedValue = formattedValue

        seekbar.onSeekBarChangeListener { progress ->
            val playbackSpeed = getPlaybackSpeed(progress)
            if (playbackSpeed.toString() != lastUpdatedFormattedValue) {
                lastUpdatedProgress = progress
                lastUpdatedFormattedValue = playbackSpeed.toString()
                config.playbackSpeed = playbackSpeed
                config.playbackSpeedProgress = progress

                speedLabel.text = "${formatPlaybackSpeed(playbackSpeed)}x"
                listener?.updatePlaybackSpeed(playbackSpeed)
            } else {
                seekbar.progress = lastUpdatedProgress
            }
        }
    }

    private fun getPlaybackSpeed(progress: Int): Float {
        var playbackSpeed = when {
            progress < HALF_PROGRESS -> {
                val lowerProgressPercent = progress / HALF_PROGRESS.toFloat()
                val lowerProgress = (1 - MIN_PLAYBACK_SPEED) * lowerProgressPercent + MIN_PLAYBACK_SPEED
                lowerProgress
            }

            progress > HALF_PROGRESS -> {
                val upperProgressPercent = progress / HALF_PROGRESS.toFloat() - 1
                val upperDiff = MAX_PLAYBACK_SPEED - 1
                upperDiff * upperProgressPercent + 1
            }

            else -> 1f
        }
        playbackSpeed = Math.min(Math.max(playbackSpeed, MIN_PLAYBACK_SPEED), MAX_PLAYBACK_SPEED)
        val stepMultiplier = 1 / STEP
        return Math.round(playbackSpeed * stepMultiplier) / stepMultiplier
    }

    private fun reduceSpeed() {
        var currentProgress = seekBar?.progress ?: return
        val currentSpeed = requireContext().config.playbackSpeed
        while (currentProgress > 0) {
            val newSpeed = getPlaybackSpeed(--currentProgress)
            if (newSpeed != currentSpeed) {
                seekBar!!.progress = currentProgress
                break
            }
        }
    }

    private fun increaseSpeed() {
        var currentProgress = seekBar?.progress ?: return
        val currentSpeed = requireContext().config.playbackSpeed
        while (currentProgress < MAX_PROGRESS) {
            val newSpeed = getPlaybackSpeed(++currentProgress)
            if (newSpeed != currentSpeed) {
                seekBar!!.progress = currentProgress
                break
            }
        }
    }

    private fun formatPlaybackSpeed(value: Float) = String.format("%.2f", value)

    fun setListener(playbackSpeedListener: PlaybackSpeedListener) {
        listener = playbackSpeedListener
    }
}
