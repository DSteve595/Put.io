package com.stevenschoen.putionew

import android.app.DownloadManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import com.stevenschoen.putionew.files.FileFinishedActivity

class PutioOpenFileService : Service() {

    companion object {
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }

    lateinit var downloadManager: DownloadManager

    var downloadId: Long = -1

    override fun onCreate() {
        super.onCreate()

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        this.downloadId = intent.extras.getLong(EXTRA_DOWNLOAD_ID, -1)

        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadReceiver, intentFilter)

        return Service.START_STICKY
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
                        val uri = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))
                        val type = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))

                        val finishedIntent = Intent(this@PutioOpenFileService, FileFinishedActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(FileFinishedActivity.EXTRA_NAME, title)
                            putExtra(FileFinishedActivity.EXTRA_URI, uri)
                            putExtra(FileFinishedActivity.EXTRA_MEDIA_TYPE, type)
                        }
                        startActivity(finishedIntent)

                        stopSelf()
                    }
                    DownloadManager.STATUS_FAILED -> { }
                    DownloadManager.STATUS_PAUSED -> { }
                    DownloadManager.STATUS_PENDING -> { }
                    DownloadManager.STATUS_RUNNING -> { }
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(downloadReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}