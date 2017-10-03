package com.stevenschoen.putionew.files

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.getUniqueLoaderId
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp
import timber.log.Timber

class FileDetailsLoader(context: Context, private val file: PutioFile) : PutioBaseLoader(context) {

    fun checkDownload() {
        AsyncTask.execute {
            putioApp(context).fileDownloadDatabase.fileDownloadsDao().getByFileIdSynchronous(file.id)?.let {
                val isDownloaded = it.status == FileDownload.Status.Downloaded
                val isDownloading = it.status == FileDownload.Status.InProgress
                if ((isDownloaded || isDownloading) && !isFileDownloadedOrDownloading(context, it)) {
                    Timber.d("${file.name} appears to not be downloaded, marking NotDownloaded")
                    markFileNotDownloaded(context, it)
                } else if (isDownloading && isFileDownloaded(context, it)) {
                    Timber.d("${file.name} appears to be downloaded, marking Downloaded")
                    markFileDownloaded(context, it)
                }
                /*
                If, for some reason, a download finished but DownloadFinishedService didn't
                receive the event, downloads can get into a state where they're completed as far
                as the DownloadManager's concerned, but the FileDownload has no Uri. That hasn't
                come up, but if it ever does, the Uri should be fetched from the DownloadManager
                and updated (as well as its status) in the FileDownload.
                 */
            }
        }
    }

    companion object {
        fun get(loaderManager: LoaderManager, context: Context, file: PutioFile): FileDetailsLoader {
            return loaderManager.initLoader(
                    getUniqueLoaderId(FileDetailsLoader::class.java), null, object : Callbacks(context) {
                override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
                    return FileDetailsLoader(context, file)
                }
            }) as FileDetailsLoader
        }
    }
}