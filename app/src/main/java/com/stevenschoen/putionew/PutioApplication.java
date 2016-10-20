package com.stevenschoen.putionew;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.stevenschoen.putionew.model.files.PutioFile;

public class PutioApplication extends Application {
	private PutioUtils utils;

	@Override
	public void onCreate() {
		super.onCreate();

        try {
            buildUtils();
        } catch (PutioUtils.NoTokenException e) {
            // User is not logged in
        }
	}

    public static PutioApplication get(Context context) {
        return (PutioApplication) context.getApplicationContext();
    }

    public boolean isLoggedIn() {
        if (utils != null) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            String token = sharedPrefs.getString("token", null);
            if (token != null && !token.isEmpty()) {
                return true;
            }
        } else {
            try {
                buildUtils();
                return isLoggedIn();
            } catch (PutioUtils.NoTokenException e) { }
        }

        return false;
    }

	public void buildUtils() throws PutioUtils.NoTokenException {
        utils = new PutioUtils(this);
    }

	public PutioUtils getPutioUtils() {
		return utils;
	}

    public interface CastCallbacks {
        void load(PutioFile file, String url, PutioUtils utils);
    }
}