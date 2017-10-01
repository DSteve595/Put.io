package com.stevenschoen.putionew.files

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import com.stevenschoen.putionew.analytics
import com.stevenschoen.putionew.putioApp

class DownloadFinishedService : JobIntentService() {

    companion object {
        private const val EXTRA_DOWNLOAD_ID = "dlid"

        private const val DOWNLOAD_FINISHED_JOB_ID = 1

        fun receiveDownloadFinished(context: Context, downloadId: Long) {
            enqueueWork(context, DownloadFinishedService::class.java, DOWNLOAD_FINISHED_JOB_ID,
                    Intent(context, DownloadFinishedService::class.java)
                            .putExtra(EXTRA_DOWNLOAD_ID, downloadId))
        }
    }

    override fun onHandleWork(intent: Intent) {
        val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1).takeIf { it != -1L } ?: return
        val downloadManager = getSystemService(Activity.DOWNLOAD_SERVICE) as DownloadManager
        val query = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        if (query.moveToFirst()) {
            val downloadStatus = query.getInt(query.getColumnIndex(DownloadManager.COLUMN_STATUS))
            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                val fileDownloads = putioApp.fileDownloadDatabase.fileDownloadsDao()
                fileDownloads.update(fileDownloads.getByDownloadIdSynchronous(downloadId)!!.apply {
                    status = FileDownload.Status.Downloaded
                    uri = downloadManager.getUriForDownloadedFile(downloadId).toString()
                })
            }
        }

        putioApp.fileDownloadDatabase.fileDownloadsDao()
                .getByDownloadId(downloadId)
                .map { it.fileId }
                .flatMapSingle(putioApp.putioUtils!!.restInterface::file)
                .map { it.file }
                .subscribe(analytics::logDownloadFinished)
    }
}

class DownloadFinishedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1).takeIf { it != -1L }
        downloadId?.let {
            DownloadFinishedService.receiveDownloadFinished(context, it)
        }
    }
}