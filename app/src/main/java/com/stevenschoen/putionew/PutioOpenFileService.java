package com.stevenschoen.putionew;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.IBinder;

import com.stevenschoen.putionew.activities.FileFinished;

public class PutioOpenFileService extends Service {
	DownloadManager downloadManager;
	long[] downloadIds;
	int id;
	String filename;
	int mode;
	
	IntentFilter fileDownloadUpdateIntentFilter = new IntentFilter(PutioActivity.fileDownloadUpdateIntent);

	@Override
	public void onCreate() {
		super.onCreate();

		downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.downloadIds = intent.getExtras().getLongArray("downloadIds");
		this.id = intent.getExtras().getInt("id");
		this.filename = intent.getExtras().getString("filename");
		this.mode = intent.getExtras().getInt("mode");
		
		IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		registerReceiver(downloadReceiver, intentFilter);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return Service.START_STICKY;
	}
	
	private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
            if (mode != PutioUtils.ACTION_NOTHING) {
                for (long downloadId : downloadIds) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor cursor = downloadManager.query(query);

                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor
                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = cursor.getInt(columnIndex);
                        int columnReason = cursor
                                .getColumnIndex(DownloadManager.COLUMN_REASON);
                        int reason = cursor.getInt(columnReason);

                        switch (status) {
                            case DownloadManager.STATUS_SUCCESSFUL:
                                Intent finishedIntent = new Intent(PutioOpenFileService.this, FileFinished.class);
                                finishedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                finishedIntent.putExtra("downloadId", downloadId);
                                finishedIntent.putExtra("id", id);
                                finishedIntent.putExtra("filename", filename);
                                finishedIntent.putExtra("mode", mode);
                                startActivity(finishedIntent);
                                break;
                            case DownloadManager.STATUS_FAILED:
                                break;
                            case DownloadManager.STATUS_PAUSED:
                                break;
                            case DownloadManager.STATUS_PENDING:
                                break;
                            case DownloadManager.STATUS_RUNNING:
                                break;
                        }
                    }
                }
            }
		}

	};

	@Override
	public void onDestroy() {
		unregisterReceiver(downloadReceiver);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}