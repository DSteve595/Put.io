package com.stevenschoen.putionew.cast;

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
}