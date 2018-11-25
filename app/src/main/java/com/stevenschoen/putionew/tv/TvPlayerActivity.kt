package com.stevenschoen.putionew.tv

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp
import org.apache.commons.io.FilenameUtils

class TvPlayerActivity : FragmentActivity() {

  companion object {
    const val EXTRA_VIDEO = "video"
    const val EXTRA_USE_MP4 = "use_mp4"
  }

  private val video by lazy { intent.getParcelableExtra<PutioFile>(EXTRA_VIDEO)!! }
  private val useMp4 by lazy { intent.getBooleanExtra(EXTRA_USE_MP4, false) }

  private lateinit var player: SimpleExoPlayer
  private lateinit var playerView: PlayerView
  private var rewindView: View? = null
  private var ffwdView: View? = null

  private var controlsVisible = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.tv_player)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val bandwidthMeter = DefaultBandwidthMeter()
    val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
    val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

    player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)

    playerView = findViewById(R.id.tv_player_exoplayer)
    playerView.player = player

    val dataSourceFactory = DefaultDataSourceFactory(
        this,
        Util.getUserAgent(this, "Put.io-for-Android"), bandwidthMeter
    )
    val url = video.getStreamUrl(putioApp.putioUtils!!, useMp4)
    val videoSource = ExtractorMediaSource.Factory(dataSourceFactory::createDataSource).createMediaSource(Uri.parse(url))

    player.playWhenReady = true
    player.prepare(videoSource)

    playerView.post {
      val titleView = findViewById<TextView>(R.id.tv_player_title)
      titleView.text = FilenameUtils.removeExtension(video.name)
      rewindView = findViewById(R.id.exo_rew)
      ffwdView = findViewById(R.id.exo_ffwd)
    }

    playerView.setControllerVisibilityListener { visibility ->
      controlsVisible = visibility == View.VISIBLE
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    return when (keyCode) {
      KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
        if (!controlsVisible) {
          playerView.showController()
          true
        } else {
          super.onKeyDown(keyCode, event)
        }
      }
      else -> super.onKeyDown(keyCode, event)
    }
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    return when (keyCode) {
      KeyEvent.KEYCODE_MEDIA_PLAY -> {
        player.playWhenReady = true
        true
      }
      KeyEvent.KEYCODE_MEDIA_PAUSE -> {
        player.playWhenReady = false
        true
      }
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
        player.playWhenReady = !player.playWhenReady
        true
      }
      KeyEvent.KEYCODE_MEDIA_REWIND -> {
        playerView.showController()
        rewindView?.callOnClick()
        true
      }
      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
        playerView.showController()
        ffwdView?.callOnClick()
        true
      }
      else -> super.onKeyUp(keyCode, event)
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
