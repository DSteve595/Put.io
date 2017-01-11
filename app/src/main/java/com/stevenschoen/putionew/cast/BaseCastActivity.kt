package com.stevenschoen.putionew.cast

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.ViewGroup
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.images.WebImage
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.files.PutioSubtitle
import org.apache.commons.io.FilenameUtils

abstract class BaseCastActivity : AppCompatActivity(), PutioApplication.CastCallbacks {

    companion object {
        const val FRAGTAG_CAST_MINI_CONTROLLER = "mini_cast"
    }

    protected val castContext: CastContext?
        get() = CastContext.getSharedInstance(this)
    protected val castSession: CastSession?
        get() = castContext?.sessionManager?.currentCastSession

    open val castMiniControllerContainerId: Int? = null
    var inflatedCastMiniController = false

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        castMiniControllerContainerId?.let { containerId ->
            if (CastOptionsProvider.isCastSdkAvailable(this@BaseCastActivity)) {
                if (!inflatedCastMiniController) {
                    val holder = findViewById(containerId) as ViewGroup
                    layoutInflater.inflate(R.layout.cast_mini_controller_putio, holder)
                    inflatedCastMiniController = true
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (CastOptionsProvider.isCastSdkAvailable(this)) {
            menuInflater.inflate(R.menu.menu_cast, menu)
            CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.menu_cast)
        }

        return true
    }

    override fun load(file: PutioFile, url: String, utils: PutioUtils) {
        if (castContext != null && castSession != null && castSession!!.remoteMediaClient != null && castSession!!.isConnected) {
            fun play(subtitles: List<PutioSubtitle>? = null) {
                val metaData = MediaMetadata(
                        if (file.isVideo)
                            MediaMetadata.MEDIA_TYPE_MOVIE
                        else
                            MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
                val title = FilenameUtils.removeExtension(file.name).let {
                    if (it.length <= 18) {
                        it
                    } else {
                        it.substring(0, 19)
                    }
                }
                metaData.putString(MediaMetadata.KEY_TITLE, title)
                file.screenshot?.let { metaData.addImage(WebImage(Uri.parse(it))) }

                val mediaInfo = MediaInfo.Builder(url)
                        .setContentType(file.contentType)
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setMetadata(metaData)
                        .apply {
                            setMediaTracks(subtitles.orEmpty().mapIndexed { i, sub ->
                                MediaTrack.Builder(i.toLong(), MediaTrack.TYPE_TEXT)
                                        .setContentId(sub.getUrl(PutioSubtitle.FORMAT_WEBVTT, file.id, utils.tokenWithStuff))
                                        .setContentType("text/vtt")
                                        .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                                        .setName(sub.name)
                                        .setLanguage(sub.language)
                                        .build()
                            })
                        }
                        .build()
                castSession!!.remoteMediaClient.load(mediaInfo, true);
            }

            if (file.isVideo) {
//                utils.restInterface.subtitles(file.id)
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe { response ->
//                            Log.d("asdf", "subtitles 1 url: ${response.subtitles.first().getUrl(PutioSubtitle.FORMAT_WEBVTT, file.id, utils.tokenWithStuff)}")
//                            play(response.subtitles)
//                        }
                play()
            } else {
                play()
            }
        } else {
            utils.getStreamUrlAndPlay(this, file, url)
        }
    }
}