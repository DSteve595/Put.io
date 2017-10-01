package com.stevenschoen.putionew

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.stevenschoen.putionew.model.files.PutioFile

class Analytics(context: Context) {

    object Event {
        const val VIEW_FOLDER = "view_folder"
        const val DOWNLOAD_ITEM = "download_item"
        const val FINISH_DOWNLOAD = "finish_download"
        const val OPEN_DOWNLOADED_FILE = "open_downloaded_file"
        const val OPEN_DOWNLOADED_VIDEO = "open_downloaded_video"
        const val STREAM_VIDEO = "stream_video"
    }

    object Param {
        const val CONTENT_SIZE = "content_size"
        const val IS_VIDEO = "is_video"
        const val MP4_SELECTED = "mp4_selected"
    }

    private val putioApp = putioApp(context)

    fun logViewedFile(file: PutioFile) {
        makeFileBundle(file)
                .log(FirebaseAnalytics.Event.VIEW_ITEM)
    }

    fun logBrowsedToFolder(folder: PutioFile) {
        makeFolderBundle(folder)
                .log(Event.VIEW_FOLDER)
    }

    fun logSearched(query: String) {
        Bundle().putString(FirebaseAnalytics.Param.SEARCH_TERM, query)
                .log(FirebaseAnalytics.Event.SEARCH)
    }

    fun logStartedFileDownload(file: PutioFile) {
        makeFileBundle(file)
                .log(Event.DOWNLOAD_ITEM)
    }

    fun logStartedVideoDownload(video: PutioFile, mp4: Boolean) {
        makeFileBundle(video)
                .putBoolean(Param.MP4_SELECTED, mp4)
                .log(Event.DOWNLOAD_ITEM)
    }

    fun logDownloadFinished(file: PutioFile) {
        makeFileBundle(file)
                .log(Event.FINISH_DOWNLOAD)
    }

    fun logOpenedDownloadedFile(file: PutioFile) {
        makeFileBundle(file)
                .log(Event.OPEN_DOWNLOADED_FILE)
    }

    fun logOpenedDownloadedVideo(video: PutioFile, mp4: Boolean) {
        makeFileBundle(video)
                .putBoolean(Param.MP4_SELECTED, mp4)
                .log(Event.OPEN_DOWNLOADED_VIDEO)
    }

    fun logStreamedVideo(video: PutioFile, mp4: Boolean) {
        makeFileBundle(video)
                .putBoolean(Param.MP4_SELECTED, mp4)
                .log(Event.STREAM_VIDEO)
    }

    private fun makeFileBundle(file: PutioFile) = Bundle().apply { addBasicFileInfo(file) }
    private fun makeFolderBundle(folder: PutioFile) = Bundle().apply { addIdAndName(folder) }

    private fun Bundle.addBasicFileInfo(file: PutioFile) {
        addIdAndName(file)
        putString(FirebaseAnalytics.Param.CONTENT_TYPE, file.contentType)
        file.size?.let { putLong(Param.CONTENT_SIZE, it) }
        putBoolean(Param.IS_VIDEO, file.isVideo)
    }

    private fun Bundle.addIdAndName(file: PutioFile) {
        putLong(FirebaseAnalytics.Param.ITEM_ID, file.id)
        putString(FirebaseAnalytics.Param.ITEM_NAME, file.name)
    }

    private fun Bundle.log(event: String) = putioApp.firebaseAnalytics.logEvent(event, this)
}

val Context.analytics
    get() = Analytics(this)
val Fragment.analytics
    get() = Analytics(context)

private val Context.firebaseAnalytics
    get() = FirebaseAnalytics.getInstance(this)!!