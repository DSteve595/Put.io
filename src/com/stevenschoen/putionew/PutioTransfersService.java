package com.stevenschoen.putionew;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.stevenschoen.putionew.activities.Putio;

public class PutioTransfersService extends Service {
	private final IBinder binder = new TransfersBinder();
	private TimerTask stopTask;

	public class TransfersBinder extends Binder {
		public PutioTransfersService getService() {
			return PutioTransfersService.this;
		}
	}
	
	SharedPreferences sharedPrefs;
	
	private String token;
	
	updateTransfersTask update = new updateTransfersTask();
	boolean paused = false;
	
	PutioUtils utils;

	private Handler handler;

	private Runnable updateTransfersRunnable;
	
	private PutioTransferData[] transfersInverted;
	private PutioTransferData[] transfers;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		token = sharedPrefs.getString("token", null);
		
		utils = new PutioUtils(token, sharedPrefs);
		
		handler = new Handler();
		updateTransfersRunnable = new Runnable() {

			@Override
			public void run() {
				if ((update.getStatus() == AsyncTask.Status.FINISHED || update.getStatus() == AsyncTask.Status.PENDING)
						&& utils.isConnected(PutioTransfersService.this)) {
					update = new updateTransfersTask();
					update.execute();
				}
				
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
	
	class updateTransfersTask extends AsyncTask<Void, Void, PutioTransferData[]> {

		@Override
		protected PutioTransferData[] doInBackground(Void... params) {
			JSONObject json;
			JSONArray array;
			
			InputStream is = null;
			
			try {
				try {
					is = utils.getTransfersListJsonData();
				} catch (SocketTimeoutException e) {
					Intent noNetworkIntent = new Intent(Putio.noNetworkIntent);
					noNetworkIntent.putExtra("from", "transfers");
					sendBroadcast(noNetworkIntent);
					
					return null;
				}

				String string = PutioUtils.convertStreamToString(is);
				json = new JSONObject(string);
				
				array = json.getJSONArray("transfers");
				transfers = new PutioTransferData[array.length()];
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					
					int fileId = 0;
					try {
						fileId = obj.getInt("file_id");
					} catch (JSONException e) { }
					
					long size = 0;
					try {
						size = obj.getInt("size");
					} catch (JSONException e) { }
					
					int percentDone = obj.getInt("percent_done");
					
					String status = obj.getString("status");
					if (status.matches("COMPLETED") || status.matches("SEEDING")) percentDone = 100;
					
					int saveParentId = 0;
					try {
						saveParentId = obj.getInt("save_parent_id");
					} catch (JSONException e) { }
					
					transfers[i] = new PutioTransferData(
							obj.getInt("id"),
							fileId,
							size,
							obj.getString("name"),
							obj.getString("estimated_time"),
							obj.getString("created_at"),
							utils.stringToBooleanHack(obj.getString("extract")),
							obj.getLong("down_speed"),
							obj.getLong("up_speed"),
							percentDone,
							status,
							obj.getString("status_message"),
							saveParentId);
				}
				
				return transfers;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(PutioTransferData[] result) {
			super.onPostExecute(result);
			
			transfersInverted = transfers.clone();
			Collections.reverse(Arrays.asList(transfersInverted)); // this is convenient
//			Log.d("asdf", "just inited, inverted length " + transfersInverted.length);
//			for (int i = 0; i < result.length; i++) {
//				transfersInverted[i] = result[result.length - i - 1];
//			}
			
			Intent transfersAvailableIntent = new Intent(Putio.transfersAvailableIntent);
			sendBroadcast(transfersAvailableIntent);
		}
	}
	
	public PutioTransferData[] getTransfers() {
		return transfersInverted;
	}
	
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		handler.removeCallbacks(updateTransfersRunnable);
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