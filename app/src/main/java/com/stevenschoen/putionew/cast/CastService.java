package com.stevenschoen.putionew.cast;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.stevenschoen.putionew.PutioFileData;
import com.stevenschoen.putionew.PutioUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CastService extends Service {

    private final IBinder binder = new CastServiceBinder();
    private TimerTask stopTask;

    public VideoCastManager videoCastManager;
    private MediaInfo mediaInfo;

    @Override
    public IBinder onBind(Intent intent) {
        if (stopTask != null) stopTask.cancel();
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        videoCastManager = VideoCastManager.initialize(this, PutioUtils.CAST_APPLICATION_ID, null, null);
        videoCastManager.enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
                VideoCastManager.FEATURE_LOCKSCREEN |
                VideoCastManager.FEATURE_DEBUGGING);
        videoCastManager.setContext(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public void loadAndPlayMedia(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
        try {
            videoCastManager.loadMedia(mediaInfo, true, 0);
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }
    }

    public boolean isCasting() {
        return videoCastManager.isConnected();
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return videoCastManager.getMediaRouteSelector();
    }

    public boolean isPlaying() {
        try {
            return videoCastManager.isRemoteMoviePlaying();
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!isCasting()) {
            stopTask = new TimerTask() {
                @Override
                public void run() {
                    if (!isCasting()) {
                        stopSelf();
                    }
                }
            };
            new Timer().schedule(stopTask, 5000);
        }
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        if (stopTask != null) stopTask.cancel();
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        try {
            videoCastManager.stopApplication();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public interface CastCallbacks {
        public void load(PutioFileData file, String url);
    }

    public interface CastUpdateListener {
        public void onInvalidate();

        public void onMediaPlay();

        public void onMediaPause();
    }

    public class CastServiceBinder extends Binder {
        public CastService getService() {
            return CastService.this;
        }
    }
}