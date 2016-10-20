package com.stevenschoen.putionew.activities

import android.net.Uri
import android.os.Bundle
import android.view.Menu
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

abstract class BaseCastActivity : BottomSheetActivity(), PutioApplication.CastCallbacks {

    private var initCast: Boolean = false

    protected val castContext: CastContext?
        get() = CastContext.getSharedInstance(this)
    protected val castSession: CastSession?
        get() = castContext!!.sessionManager.currentCastSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initCast = false

        val application = application as PutioApplication
        if (application.isLoggedIn) {
            initCast()
        }
    }

    protected fun initCast() {
        if (!initCast) {
            supportInvalidateOptionsMenu()

            initCast = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (initCast) {
            menuInflater.inflate(R.menu.menu_cast, menu)

            CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.menu_cast)
            return true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()

        if (initCast) {
            castContext?.let { it.onActivityResumed(this) }
        }
    }

    override fun onPause() {
        if (initCast) {
            castContext?.let { it.onActivityPaused(this) }
        }

        super.onPause()
    }

    override fun load(file: PutioFile, url: String, utils: PutioUtils) {
        if (castContext == null || !castContext!!.isAppVisible) {
            utils.getStreamUrlAndPlay(this, file, url)
        } else {
            fun play(subtitles: List<PutioSubtitle>? = null) {
                val metaData = MediaMetadata(if (file.isVideo)
                    MediaMetadata.MEDIA_TYPE_MOVIE
                else
                    MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
                val title = FilenameUtils.removeExtension(file.name).let {
                    if (it.length < 18) {
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
                            subtitles?.let {
                                setMediaTracks(subtitles.mapIndexed { i, sub ->
                                    MediaTrack.Builder(i.toLong(), MediaTrack.TYPE_TEXT)
                                            .setContentId(sub.getUrl(PutioSubtitle.FORMAT_WEBVTT, file.id, utils.tokenWithStuff))
                                            .setContentType("text/vtt")
                                            .setSubtype(MediaTrack.SUBTYPE_CAPTIONS)
                                            .setName(sub.name)
                                            .setLanguage(sub.language)
                                            .build()
                                })
                            }
                        }
                        .build()
                castSession!!.remoteMediaClient.load(mediaInfo, true);
            }

            if (file.isVideo) {
//                utils.restInterface.subtitles(file.id)
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe { response ->
//                            play(response.subtitles)
//                        }
                play()
            } else {
                play()
            }
        }
    }
}