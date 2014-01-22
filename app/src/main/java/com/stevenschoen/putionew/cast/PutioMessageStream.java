package com.stevenschoen.putionew.cast;

import org.json.JSONObject;

import android.util.Log;

import com.google.cast.MediaProtocolMessageStream;

public class PutioMessageStream extends MediaProtocolMessageStream {
	OnStatusUpdateListener statusUpdateListener;
	
	@Override
	protected void onStatusUpdated() {
		super.onStatusUpdated();
		
		if (statusUpdateListener != null) {
			statusUpdateListener.onStatusUpdated();
		}
	}
	
	public void setOnStatusUpdateListener(OnStatusUpdateListener listener) {
		this.statusUpdateListener = listener;
	}
	
	public static interface OnStatusUpdateListener {
		public void onStatusUpdated();
	}
	
	@Override
	protected void onError(String errorDomain, long errorCode, JSONObject errorInfo) {
		super.onError(errorDomain, errorCode, errorInfo);
		
		if (errorDomain != null) {
			Log.d("asdf", "errorDomain: " + errorDomain);
		}
		Log.d("asdf", "errorCode: " + errorCode);
		
		try {
			Log.d("asdf", "errorInfo: " + errorInfo.toString(3));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}