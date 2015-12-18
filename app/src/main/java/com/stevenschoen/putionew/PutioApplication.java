package com.stevenschoen.putionew;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.media.MediaRouteSelector;

import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.stevenschoen.putionew.activities.BaseCastActivity;
import com.stevenschoen.putionew.model.files.PutioFile;

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
        utils = new PutioUtils(this);
    }

	public PutioUtils getPutioUtils() {
		return utils;
	}

    public void buildVideoCastManager() {
        CastConfiguration.Builder castConfigBuilder = new CastConfiguration.Builder(PutioUtils.CAST_APPLICATION_ID)
                .enableNotification()
                .enableLockScreen();
        if (BuildConfig.DEBUG) castConfigBuilder.enableDebug();

        videoCastManager = VideoCastManager.initialize(this, castConfigBuilder.build());
        videoCastManager.setVolumeStep(BaseCastActivity.VOLUME_INCREMENT);
        videoCastManager.setStopOnDisconnect(true);
    }

    public VideoCastManager getVideoCastManager() {
        return videoCastManager;
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return videoCastManager.getMediaRouteSelector();
    }

    public interface CastCallbacks {
        void load(PutioFile file, String url, PutioUtils utils);
    }
}