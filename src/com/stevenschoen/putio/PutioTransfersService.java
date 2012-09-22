package com.stevenschoen.putio;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class PutioTransfersService extends Service {
	PutioUtils utils;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		utils = new PutioUtils(null, null);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}