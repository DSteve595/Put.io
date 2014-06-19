package com.stevenschoen.putionew;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.stevenschoen.putionew.activities.Putio;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.responses.TransfersListResponse;
import com.stevenschoen.putionew.model.transfers.PutioTransferData;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PutioTransfersService extends Service {
	
	private final IBinder binder = new TransfersServiceBinder();
	private TimerTask stopTask;

	public class TransfersServiceBinder extends Binder {
		public PutioTransfersService getService() {
			return PutioTransfersService.this;
		}
	}
	
	SharedPreferences sharedPrefs;

	private PutioUtils utils;

	private Handler handler;

	private Runnable updateTransfersRunnable;

	private List<PutioTransferData> transfers;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		this.utils = ((PutioApplication) getApplication()).getPutioUtils();

		utils.getEventBus().register(this);
		
		handler = new Handler();
		updateTransfersRunnable = new Runnable() {
			@Override
			public void run() {
				utils.getJobManager().addJobInBackground(new PutioRestInterface.GetTransfersJob(utils));

				if (!utils.isConnected(PutioTransfersService.this)) {
					Intent noNetworkIntent = new Intent(Putio.noNetworkIntent);
					noNetworkIntent.putExtra("from", "transfers");
					sendBroadcast(noNetworkIntent);
				}
				
				handler.postDelayed(this, 8000);
			}
		};
		handler.post(updateTransfersRunnable);
	}

	public void onEvent(TransfersListResponse result) {
		transfers = result.getTransfers();
		Collections.reverse(transfers);

		Intent transfersAvailableIntent = new Intent(Putio.transfersAvailableIntent);
		sendBroadcast(transfersAvailableIntent);
	}

	public List<PutioTransferData> getTransfers() {
		return transfers;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		handler.removeCallbacks(updateTransfersRunnable);
		utils.getEventBus().unregister(this);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (stopTask != null) stopTask.cancel();
		return binder;
	}
	
	@Override
	public void onRebind(Intent intent) {
		if (stopTask != null) stopTask.cancel();
		super.onRebind(intent);
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		stopTask = new TimerTask() {
			@Override
			public void run() {
				stopSelf();
			}
		};
		new Timer().schedule(stopTask, 5000);
		
		return true;
	}
}