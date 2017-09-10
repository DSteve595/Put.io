package com.stevenschoen.putionew.files

import android.app.Activity
import android.app.DownloadManager
import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stevenschoen.putionew.putioApp

class DownloadFinishedService : IntentService("downloadfinishedreceiver") {

    companion object {
        const val EXTRA_DOWNLOAD_ID = "dlid"
    }

    override fun onHandleIntent(intent: Intent?) {
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1).takeIf { it != -1L } ?: return
        val downloadManager = getSystemService(Activity.DOWNLOAD_SERVICE) as DownloadManager
        val query = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        if (query.moveToFirst()) {
            val downloadStatus = query.getInt(query.getColumnIndex(DownloadManager.COLUMN_STATUS))
            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                val fileDownloads = putioApp.fileDownloadDatabase.fileDownloadsDao()
                fileDownloads.update(fileDownloads.getByDownloadIdSynchronous(downloadId).apply {
                    status = FileDownload.Status.Downloaded
                    uri = downloadManager.getUriForDownloadedFile(downloadId).toString()
                })
            }
        }
    }
}

class DownloadFinishedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1).takeIf { it != -1L }
        downloadId?.let {
            context.startService(Intent(context, DownloadFinishedService::class.java)
                    .putExtra(DownloadFinishedService.EXTRA_DOWNLOAD_ID, it))
        }
    }
}