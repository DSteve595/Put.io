package com.stevenschoen.putionew;

import android.app.Application;

public class PutioApplication extends Application {
	private PutioUtils utils;

	@Override
	public void onCreate() {
		super.onCreate();

		buildUtils();
	}

	public void buildUtils() {
		try {
			this.utils = new PutioUtils(this);
		} catch (PutioUtils.NoTokenException e) {
//			User not logged in yet
		}
	}

	public PutioUtils getPutioUtils() {
		return this.utils;
	}
}
