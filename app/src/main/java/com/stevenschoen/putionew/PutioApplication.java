package com.stevenschoen.putionew;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.media.MediaRouteSelector;

import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.stevenschoen.putionew.model.files.PutioFileData;

public class PutioApplication extends Application {
	private PutioUtils utils;
    private VideoCastManager videoCastManager;

	@Override
	public void onCreate() {
		super.onCreate();

        try {
            buildUtils();
        } catch (PutioUtils.NoTokenException e) {
            // User is not logged in
        }
        buildVideoCastManager();
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
        this.utils = new PutioUtils(this);
    }

	public PutioUtils getPutioUtils() {
		return utils;
	}

    public void buildVideoCastManager() {
        videoCastManager = VideoCastManager.initialize(this, PutioUtils.CAST_APPLICATION_ID, null, null);
        videoCastManager.enableFeatures(
                VideoCastManager.FEATURE_NOTIFICATION |
                        VideoCastManager.FEATURE_LOCKSCREEN |
                        VideoCastManager.FEATURE_DEBUGGING
        );
        videoCastManager.setStopOnDisconnect(true);
    }

    public VideoCastManager getVideoCastManager() {
        return videoCastManager;
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return videoCastManager.getMediaRouteSelector();
    }

    public interface CastCallbacks {
        public void load(PutioFileData file, String url, PutioUtils utils);
    }
}