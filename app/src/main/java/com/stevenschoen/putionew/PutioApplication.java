package com.stevenschoen.putionew;

import android.app.Application;

public class PutioApplication extends Application {
	private PutioUtils utils;

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			this.utils = new PutioUtils(this);
		} catch (PutioUtils.NoTokenException e) {
			e.printStackTrace();
		}
	}

	public PutioUtils getPutioUtils() {
		return this.utils;
	}
}
