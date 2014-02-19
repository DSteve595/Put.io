/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.google.sample.castcompanionlibrary.notification;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGD;
import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.sample.castcompanionlibrary.R;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.castcompanionlibrary.cast.player.VideoCastControllerActivity;
import com.google.sample.castcompanionlibrary.utils.LogUtils;
import com.google.sample.castcompanionlibrary.utils.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A service to provide status bar Notifications when we are casting. For JB+ versions, notification
 * area provides a play/pause toggle and an "x" button to disconnect but that for GB, we do not show
 * that due to the framework limitations.
 */
public class VideoCastNotificationService extends Service {

    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.google.sample.castcompanionlibrary.action.toggleplayback";
    public static final String ACTION_STOP =
            "com.google.sample.castcompanionlibrary.action.stop";
    public static final String ACTION_VISIBILITY =
            "com.google.sample.castcompanionlibrary.action.notificationvisibility";
    private static int NOTIFICATION_ID = 1;

    private static final String TAG = LogUtils.makeLogTag(VideoCastNotificationService.class);
    private String mApplicationId;
    private Bitmap mVideoArtBitmap;
    private Uri mVideoArtUri;
    private boolean isPlaying;
    private Class<?> mTargetActivity;
    private int mStatus;
    private Notification mNotification;
    private boolean mVisible;
    boolean mIsIcsOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    private BroadcastReceiver mBroadcastReceiver;
    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mConsumer;

    @Override
    public void onCreate() {
        super.onCreate();
        readPersistedData();
        mCastManager = VideoCastManager.initialize(this, mApplicationId, mTargetActivity, null);
        if (!mCastManager.isConnected()) {
            mCastManager.reconnectSessionIfPossible(this, false);
        }
        mConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onApplicationDisconnected(int errorCode) {
                LOGD(TAG, "onApplicationDisconnected() was reached");
                stopSelf();
            }

            @Override
            public void onRemoteMediaPlayerStatusUpdated() {
                int mediaStatus = mCastManager.getPlaybackStatus();
                VideoCastNotificationService.this.onRemoteMediaPlayerStatusUpdated(mediaStatus);
            }

        };
        mCastManager.addVideoCastConsumer(mConsumer);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {

            String action = intent.getAction();
            if (ACTION_TOGGLE_PLAYBACK.equals(action) && mIsIcsOrAbove) {
                LOGD(TAG, "onStartCommand(): Action: ACTION_TOGGLE_PLAYBACK");
                togglePlayback();
            } else if (ACTION_STOP.equals(action) && mIsIcsOrAbove) {
                LOGD(TAG, "onStartCommand(): Action: ACTION_STOP");
                stopApplication();
            } else if (ACTION_VISIBILITY.equals(action)) {
                mVisible = intent.getBooleanExtra("visible", false);
                LOGD(TAG, "onStartCommand(): Action: ACTION_VISIBILITY " + mVisible);
                if (mVisible && null != mNotification) {
                    startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    stopForeground(true);
                }
            } else {
                LOGD(TAG, "onStartCommand(): Action: none");
            }

        } else {
            LOGD(TAG, "onStartCommand(): Intent was null");
        }

        return Service.START_FLAG_REDELIVERY;
    }

    private void setupNotification(final MediaInfo info, final boolean visible)
            throws TransientNetworkDisconnectionException, NoConnectionException {
        if (null == info) {
            return;
        }
        try {
            MediaMetadata mm = info.getMetadata();
            Uri uri = null;
            if (!mm.getImages().isEmpty()) {
                uri = mm.getImages().get(0).getUrl();
            }
            if (null == uri) {
                build(info, null, isPlaying, mTargetActivity);
                if (visible) {
                    startForeground(NOTIFICATION_ID, mNotification);
                }
            } else if (null != mVideoArtBitmap && null != mVideoArtUri &&
                    mVideoArtUri.equals(uri)) {
                build(info, mVideoArtBitmap, isPlaying, mTargetActivity);
                if (visible) {
                    startForeground(NOTIFICATION_ID, mNotification);
                }
            } else {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        URL imgUrl = null;
                        try {
                            MediaMetadata mm = info.getMetadata();
                            mVideoArtUri = mm.getImages().get(0).getUrl();
                            imgUrl = new URL(mVideoArtUri.toString());
                            mVideoArtBitmap = BitmapFactory.decodeStream(imgUrl.openStream());
                            build(info, mVideoArtBitmap, isPlaying, mTargetActivity);
                            if (visible) {
                                startForeground(NOTIFICATION_ID, mNotification);
                            }
                        } catch (MalformedURLException e) {
                            LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                                    imgUrl + ", using the default one", e);
                        } catch (IOException e) {
                            LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                                    imgUrl + ", using the default one", e);
                        } catch (CastException e) {
                            LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                                    imgUrl + ", using the default one", e);
                        } catch (TransientNetworkDisconnectionException e) {
                            LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                                    imgUrl + " due to network issues, using the default one", e);
                        } catch (NoConnectionException e) {
                            LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                                    imgUrl + " due to network issues, using the default one", e);
                        }

                    }
                }).start();
            }
        } catch (CastException e) {
            // already logged
        }
    }

    /**
     * Removes the existing notification.
     */
    private void removeNotification() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).
                cancel(NOTIFICATION_ID);
    }

    private void onRemoteMediaPlayerStatusUpdated(int mediaStatus) {
        mStatus = mediaStatus;
        LOGD(TAG, "onRemoteMediaPlayerMetadataUpdated() reached with status: " + mStatus);
        try {
            switch (mediaStatus) {
                case MediaStatus.PLAYER_STATE_BUFFERING: // (== 4)
                    isPlaying = false;
                    setupNotification(mCastManager.getRemoteMediaInformation(), mVisible);
                    break;
                case MediaStatus.PLAYER_STATE_PLAYING: // (== 2)
                    isPlaying = true;
                    setupNotification(mCastManager.getRemoteMediaInformation(), mVisible);
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED: // (== 3)
                    isPlaying = false;
                    setupNotification(mCastManager.getRemoteMediaInformation(), mVisible);
                    break;
                case MediaStatus.PLAYER_STATE_IDLE: // (== 1)
                    isPlaying = false;
                    stopForeground(true);
                    break;
                case MediaStatus.PLAYER_STATE_UNKNOWN: // (== 0)
                    isPlaying = false;
                    stopForeground(true);
                    break;
                default:
                    break;
            }
        } catch (TransientNetworkDisconnectionException e) {
            LOGE(TAG, "Failed to update the playback status due to network issues", e);
        } catch (NoConnectionException e) {
            LOGE(TAG, "Failed to update the playback status due to network issues", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        LOGD(TAG, "onDestroy was called");
        removeNotification();
        if (null != mBroadcastReceiver) {
            unregisterReceiver(mBroadcastReceiver);
        }
        if (null != mCastManager && null != mConsumer) {
            mCastManager.removeVideoCastConsumer(mConsumer);
            mCastManager = null;
        }
    }

    /*
     * Build the RemoteViews for the notification. We also need to add the appropriate "back stack"
     * so when user goes into the CastPlayerActivity, she can have a meaningful "back" experience.
     */
    private RemoteViews build(MediaInfo info, Bitmap bitmap, boolean isPlaying,
            Class<?> targetActivity) throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        Bundle mediaWrapper = Utils.fromMediaInfo(mCastManager.getRemoteMediaInformation());
        Intent contentIntent = null;
        if (null == mTargetActivity) {
            mTargetActivity = VideoCastControllerActivity.class;
        }
        contentIntent = new Intent(this, mTargetActivity);

        contentIntent.putExtra("media", mediaWrapper);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(mTargetActivity);

        stackBuilder.addNextIntent(contentIntent);
        stackBuilder.editIntentAt(1).putExtra("media", mediaWrapper);

        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT);

        MediaMetadata mm = info.getMetadata();

        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.custom_notification);
        if (mIsIcsOrAbove) {
            addPendingIntents(rv, isPlaying);
        }
        if (null != bitmap) {
            rv.setImageViewBitmap(R.id.iconView, bitmap);
        }
        rv.setTextViewText(R.id.titleView, mm.getString(MediaMetadata.KEY_TITLE));
        String castingTo = getResources().getString(R.string.casting_to_device,
                mCastManager.getDeviceName());
        rv.setTextViewText(R.id.subTitleView, castingTo);
        mNotification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_action_notification)
                .setContentIntent(resultPendingIntent)
                .setContent(rv)
                .setAutoCancel(false)
                .setOngoing(true)
                .build();

        // to get around a bug in GB version, we add the following line
        // see https://code.google.com/p/android/issues/detail?id=30495
        mNotification.contentView = rv;

        return rv;
    }

    private void addPendingIntents(RemoteViews rv, boolean isPlaying) {
        Intent playbackIntent = new Intent(ACTION_TOGGLE_PLAYBACK);
        playbackIntent.setPackage(getPackageName());
        PendingIntent playbackPendingIntent = PendingIntent
                .getBroadcast(this, 0, playbackIntent, 0);

        Intent stopIntent = new Intent(ACTION_STOP);
        stopIntent.setPackage(getPackageName());
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        rv.setOnClickPendingIntent(R.id.playPauseView, playbackPendingIntent);
        rv.setOnClickPendingIntent(R.id.removeView, stopPendingIntent);

        if (isPlaying) {
            rv.setImageViewResource(R.id.playPauseView, R.drawable.ic_av_pause_sm_dark);
        } else {
            rv.setImageViewResource(R.id.playPauseView, R.drawable.ic_av_play_sm_dark);
        }
    }

    private void togglePlayback() {
        try {
            mCastManager.togglePlayback();
        } catch (Exception e) {
            LOGE(TAG, "Failed to toggle the playback", e);
        }
    }

    /*
     * We try to disconnect application but even if that fails, we need to remove notification since
     * that is the only way to get rid of it without going to the application
     */
    private void stopApplication() {
        try {
            LOGD(TAG, "Calling stopApplication");
            mCastManager.disconnect();
        } catch (Exception e) {
            LOGE(TAG, "Failed to disconnect application", e);
        }
        stopSelf();
    }

    /*
     * Reads application ID and target activity from preference storage.
     */
    private void readPersistedData() {
        mApplicationId = Utils.getStringFromPreference(
                this, VideoCastManager.PREFS_KEY_APPLICATION_ID);
        String targetName = Utils.getStringFromPreference(
                this, VideoCastManager.PREFS_KEY_CAST_ACTIVITY_NAME);
        try {
            if (null != targetName) {
                mTargetActivity = Class.forName(targetName);
            } else {
                mTargetActivity = VideoCastControllerActivity.class;
            }

        } catch (ClassNotFoundException e) {
            LOGE(TAG, "Failed to find the targetActivity class", e);
        }
    }
}
