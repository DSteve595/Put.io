package com.stevenschoen.putionew.tv

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import org.apache.commons.io.FilenameUtils

class TvPlayerActivity : FragmentActivity() {

    companion object {
        const val EXTRA_VIDEO = "video"
        const val EXTRA_USE_MP4 = "use_mp4"
    }

    val video by lazy { intent.getParcelableExtra<PutioFile>(EXTRA_VIDEO) }
    val useMp4 by lazy { intent.getBooleanExtra(EXTRA_USE_MP4, false) }

    lateinit var player: SimpleExoPlayer
    lateinit var playerView: SimpleExoPlayerView
    var rewindView: View? = null
    var ffwdView: View? = null

    var controlsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.tv_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)

        playerView = findViewById(R.id.tv_player_exoplayer) as SimpleExoPlayerView
        playerView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "Put.io-for-Android"), bandwidthMeter)
        val extractorsFactory = DefaultExtractorsFactory()
        val url = video.getStreamUrl(PutioApplication.get(this).putioUtils, useMp4)
        val videoSource = ExtractorMediaSource(Uri.parse(url), dataSourceFactory, extractorsFactory, null, null)

        player.playWhenReady = true
        player.prepare(videoSource)

        playerView.post {
            val titleView = findViewById(R.id.tv_player_title) as TextView
            titleView.text = FilenameUtils.removeExtension(video.name)
            rewindView = findViewById(R.id.exo_rew)
            ffwdView = findViewById(R.id.exo_ffwd)
        }

        playerView.setControllerVisibilityListener { visibility ->
            controlsVisible = visibility == View.VISIBLE
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!controlsVisible) {
                    playerView.showController()
                    return true
                } else {
                    return super.onKeyDown(keyCode, event)
                }
            }
            else -> return super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                player.playWhenReady = true
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                player.playWhenReady = false
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                player.playWhenReady = !player.playWhenReady
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                playerView.showController()
                rewindView?.callOnClick()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                playerView.showController()
                ffwdView?.callOnClick()
                return true
            }
            else -> return super.onKeyUp(keyCode, event)
        }
    }

    override fun onBackPressed() {
        if (controlsVisible) {
            playerView.hideController()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        player.release()
    }
}