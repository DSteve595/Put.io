package com.stevenschoen.putionew.files

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.getUniqueLoaderId
import com.stevenschoen.putionew.log
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp

class NewFileDetailsLoader(context: Context, private val file: PutioFile) : PutioBaseLoader(context) {

    fun checkDownload() {
        AsyncTask.execute {
            putioApp(context).fileDownloadDatabase.fileDownloadsDao().getByFileIdSynchronous(file.id)?.let {
                if ((it.status == FileDownload.Status.Downloaded || it.status == FileDownload.Status.InProgress)
                        && !isFileDownloadedOrDownloading(context, it)) {
                    log("${file.name} appears to not be downloaded, marking NotDownloaded")
                    markFileNotDownloaded(context, it)
                }
            }
        }
    }

    companion object {
        fun get(loaderManager: LoaderManager, context: Context, file: PutioFile): NewFileDetailsLoader {
            return loaderManager.initLoader(
                    getUniqueLoaderId(NewFileDetailsLoader::class.java), null, object : Callbacks(context) {
                override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
                    return NewFileDetailsLoader(context, file)
                }
            }) as NewFileDetailsLoader
        }
    }
}