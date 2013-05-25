package com.stevenschoen.putionew;

import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.stevenschoen.putionew.activities.Putio;

public class PutioTransfersService extends Service {
	SharedPreferences sharedPrefs;
	
	private String token;
	private String tokenWithStuff;
	
	updateTransfersTask update = new updateTransfersTask();
	
	PutioUtils utils;

	private Handler handler;

	private Runnable updateTransfersRunnable;
	
	private PutioTransferData[] files;
	private PutioTransferData[] filesOld;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		token = sharedPrefs.getString("token", null);
		tokenWithStuff = "?oauth_token=" + token;
		
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
	
	class updateTransfersTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
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

				String string = utils.convertStreamToString(is);
				json = new JSONObject(string);
				
				array = json.getJSONArray("transfers");
				files = new PutioTransferData[array.length()];
				
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					
					int fileId = 0;
					long size = 0;
					int saveParentId = 0;
					try {
						fileId = obj.getInt("file_id");
					} catch (JSONException e) {
					}
					try {
						size = obj.getInt("size");
					} catch (JSONException e) {
					}
					try {
						saveParentId = obj.getInt("save_parent_id");
					} catch (JSONException e) {
					}
					files[i] = new PutioTransferData(
							obj.getInt("id"),
							fileId,
							size,
							obj.getString("name"),
							obj.getString("estimated_time"),
							obj.getString("created_at"),
							utils.stringToBooleanHack(obj.getString("extract")),
							obj.getLong("down_speed"),
							obj.getLong("up_speed"),
							obj.getInt("percent_done"),
							obj.getString("status"),
							obj.getString("status_message"),
							saveParentId);
				}
				
				PutioTransferData[] filesInverted = new PutioTransferData[files.length];
				boolean changed = false;
				for (int i = 0; i < files.length; i++) {
					filesInverted[i] = files[files.length - i - 1];
					if (filesOld != null && (files.length != filesOld.length || files[i].id != filesOld[i].id)) {
						changed = true;
					}
				}
				
				Intent transfersUpdateIntent = new Intent(Putio.transfersUpdateIntent);
				if (changed) {
					transfersUpdateIntent.putExtra("changed", true);
				}
				filesOld = files;
				
				transfersUpdateIntent.putExtra("transfers", filesInverted);
				sendBroadcast(transfersUpdateIntent);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
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
	public IBinder onBind(Intent arg0) {
		return null;
	}
}