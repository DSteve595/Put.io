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

package com.google.sample.castcompanionlibrary.cast.player;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGD;
import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.sample.castcompanionlibrary.R;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.castcompanionlibrary.utils.LogUtils;
import com.google.sample.castcompanionlibrary.utils.Utils;

/**
 * This class provides an {@link Activity} that clients can easily add to their applications to
 * provide an out-of-the-box remote player when a video is casting to a cast device.
 * {@link VideoCastManager} can manage the lifecycle and presentation of this activity.
 * <p>
 * This activity provides a number of controllers for managing the playback of the remote content:
 * play/pause and seekbar.
 */
public class VideoCastControllerActivity extends ActionBarActivity {

    private static final String TAG = LogUtils.makeLogTag(VideoCastControllerActivity.class);
    private VideoCastManager mCastManager;
    private View mPageView;
    private ImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private SeekBar mSeekbar;
    private TextView mLine1;
    private TextView mLine2;
    private MediaInfo mSelectedMedia;
    private int mPlaybackState = MediaStatus.PLAYER_STATE_IDLE;
    private boolean mShouldStartPlayback;
    private Timer mSeekbarTimer;
    private final Handler mHandler = new Handler();
    private MyCastConsumer mCastConsumer;
    private ProgressBar mLoading;
    private AsyncTask<String, Void, Bitmap> mImageAsyncTask;
    private float mVolumeIncrement;
    private View mControllers;
    private boolean mIsFresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsFresh = true;
        setContentView(R.layout.cast_activity);
        mVolumeIncrement = Utils.getFloatFromPreference(
                this, VideoCastManager.PREFS_KEY_VOLUME_INCREMENT);
        try {
            mCastManager = VideoCastManager.getInstance(this);
        } catch (CastException e) {
            // logged already
        }
        loadViews();
        setupActionBar();
        Bundle extras = getIntent().getExtras();
        Bundle mediaWrapper = extras.getBundle("media");
        if (null == extras || null == mediaWrapper) {
            finish();
        }
        mShouldStartPlayback = extras.getBoolean("shouldStart");
        mCastConsumer = new MyCastConsumer();

        mSelectedMedia = Utils.toMediaInfo(mediaWrapper);

        try {
            if (mShouldStartPlayback) {
                // need to start remote playback
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                updatePlayButton(mPlaybackState);
                mediaWrapper.getInt("startPoint", 0);
                mCastManager.loadMedia(mSelectedMedia, true, mediaWrapper.getInt("startPoint", 0));
            } else {
                // we don't change the status of remote playback
                if (mCastManager.isRemoteMoviePlaying()) {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PLAYING;
                } else {
                    mPlaybackState = MediaStatus.PLAYER_STATE_PAUSED;
                }
                updatePlayButton(mPlaybackState);
            }
        } catch (Exception e) {
            LOGE(TAG, "Failed to get playback and media information", e);
            finish();
        }
        updateMetadata();

        setupSeekBar();

        mPlayPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                LOGD(TAG, "isConnected returning: " + mCastManager.isConnected());
                try {
                    togglePlayback();
                } catch (TransientNetworkDisconnectionException e) {
                    LOGE(TAG, "Failed to toggle playback due to temporary network issue", e);
                    Utils.showErrorDialog(VideoCastControllerActivity.this,
                            R.string.failed_no_connection_trans);
                } catch (NoConnectionException e) {
                    LOGE(TAG, "Failed to toggle playback due to network issues", e);
                    Utils.showErrorDialog(VideoCastControllerActivity.this,
                            R.string.failed_no_connection);
                } catch (Exception e) {
                    LOGE(TAG, "Failed to toggle playback due to other issues", e);
                    Utils.showErrorDialog(VideoCastControllerActivity.this,
                            R.string.failed_perform_action);
                }
            }
        });

        restartTrickplayTimer();
    }

    private void setupSeekBar() {
        mSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                try {
                    if (mPlaybackState == MediaStatus.PLAYER_STATE_PLAYING) {
                        mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                        updatePlayButton(mPlaybackState);
                        mCastManager.play(seekBar.getProgress());
                    } else if (mPlaybackState == MediaStatus.PLAYER_STATE_PAUSED) {
                        mCastManager.seek(seekBar.getProgress());
                    }
                    restartTrickplayTimer();
                } catch (Exception e) {
                    LOGE(TAG, "Failed to complete seek", e);
                    finish();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopTrickplayTimer();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                mStart.setText(Utils.formatMillis(progress));
            }
        });
    }

    private void togglePlayback() throws CastException, TransientNetworkDisconnectionException,
            NoConnectionException {
        switch (mPlaybackState) {
            case MediaStatus.PLAYER_STATE_PAUSED:
                mCastManager.play();
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                restartTrickplayTimer();
                break;
            case MediaStatus.PLAYER_STATE_PLAYING:
                mCastManager.pause();
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                mCastManager.loadMedia(mSelectedMedia, true, 0);
                mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                restartTrickplayTimer();
                break;

            default:
                break;
        }
        updatePlayButton(mPlaybackState);
    }

    private void updatePlayButton(int state) {
        switch (state) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_pause_dark));
                mLine2.setText(getString(R.string.casting_to_device,
                        mCastManager.getDeviceName()));
                mControllers.setVisibility(View.VISIBLE);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                mControllers.setVisibility(View.VISIBLE);
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_play_dark));
                mLine2.setText(getString(R.string.casting_to_device,
                        mCastManager.getDeviceName()));
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                mLoading.setVisibility(View.INVISIBLE);
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_av_play_dark));
                mLine2.setText(getString(R.string.casting_to_device,
                        mCastManager.getDeviceName()));
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                mLoading.setVisibility(View.VISIBLE);
                mLine2.setText(getString(R.string.loading));
                break;
            default:
                break;
        }
    }

    class MyCastConsumer extends VideoCastConsumerImpl {

        @Override
        public void onDisconnected() {
            finish();
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            finish();
        }

        @Override
        public void onRemoteMediaPlayerMetadataUpdated() {
            try {
                mSelectedMedia = mCastManager.getRemoteMediaInformation();
                updateMetadata();
            } catch (TransientNetworkDisconnectionException e) {
                LOGE(TAG, "Failed to update the metadata due to network issues", e);
            } catch (NoConnectionException e) {
                LOGE(TAG, "Failed to update the metadata due to network issues", e);
            }
        }

        @Override
        public void onRemoteMediaPlayerStatusUpdated() {
            int mediaStatus = mCastManager.getPlaybackStatus();
            LOGD(TAG, "onRemoteMediaPlayerStatusUpdated(), status: " + mediaStatus);
            switch (mediaStatus) {
                case MediaStatus.PLAYER_STATE_PLAYING:
                    if (mPlaybackState != MediaStatus.PLAYER_STATE_PLAYING) {
                        mPlaybackState = MediaStatus.PLAYER_STATE_PLAYING;
                        updatePlayButton(mPlaybackState);
                    }
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED:
                    if (mPlaybackState != MediaStatus.PLAYER_STATE_PAUSED) {
                        mPlaybackState = MediaStatus.PLAYER_STATE_PAUSED;
                        updatePlayButton(mPlaybackState);
                    }
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    if (mPlaybackState != MediaStatus.PLAYER_STATE_BUFFERING) {
                        mPlaybackState = MediaStatus.PLAYER_STATE_BUFFERING;
                        updatePlayButton(mPlaybackState);
                    }
                    break;
                case MediaStatus.PLAYER_STATE_IDLE:
                    if (mCastManager.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                        finish();
                    }
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            updateControlersStatus(false);
        }

        @Override
        public void onConnectivityRecovered() {
            updateControlersStatus(true);
        }

    }

    private void updateMetadata() {
        MediaMetadata mm = mSelectedMedia.getMetadata();
        mLine1.setText(mm.getString(MediaMetadata.KEY_TITLE));
        if (null != mImageAsyncTask) {
            mImageAsyncTask.cancel(true);
        }
        mImageAsyncTask = new AsyncTask<String, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(String... params) {
                String uri = params[0];
                try {
                    URL imgUrl = new URL(uri);
                    return BitmapFactory.decodeStream(imgUrl.openStream());
                } catch (Exception e) {
                    LOGE(TAG, "Failed to load the image with url: " +
                            uri, e);

                }
                return null;
            }

            @SuppressWarnings("deprecation")
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (null != bitmap) {
                    mPageView.setBackgroundDrawable(new BitmapDrawable(getResources(), bitmap));
                }
            }
        };

        mImageAsyncTask.execute(Utils.getImageUrl(mSelectedMedia, 1));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.cast_player_menu, menu);
        mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mVolumeIncrement == Float.MIN_VALUE) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            onVolumeChange(mVolumeIncrement);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            onVolumeChange(-(double) mVolumeIncrement);
        } else {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    private void onVolumeChange(double volumeIncrement) {
        if (mCastManager == null) {
            return;
        }
        try {
            mCastManager.incrementVolume(volumeIncrement);
        } catch (Exception e) {
            LOGE(TAG, "onVolumeChange() Failed to change volume", e);
            Utils.showErrorDialog(VideoCastControllerActivity.this,
                    R.string.failed_setting_volume);
        }
    }

    private class UpdateSeekbarTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    int currentPos = 0;
                    if (mPlaybackState == MediaStatus.PLAYER_STATE_BUFFERING) {
                        return;
                    }
                    if (!mCastManager.isConnected()) {
                        return;
                    }
                    try {
                        double duration = mCastManager.getMediaDuration();
                        if (duration > 0) {
                            try {
                                currentPos = (int) mCastManager.getCurrentMediaPosition();
                                updateSeekbar(currentPos, (int) duration);
                            } catch (Exception e) {
                                LOGE(TAG, "Failed to get current media position");
                            }
                        }
                    } catch (TransientNetworkDisconnectionException e) {
                        LOGE(TAG, "Failed to update the progress bar due to network issues", e);
                    } catch (NoConnectionException e) {
                        LOGE(TAG, "Failed to update the progress bar due to network issues", e);
                    }

                }
            });
        }
    }

    private void updateSeekbar(int position, int duration) {
        mSeekbar.setProgress(position);
        mSeekbar.setMax(duration);
        mStart.setText(Utils.formatMillis(position));
        mEnd.setText(Utils.formatMillis(duration));
    }

    private void stopTrickplayTimer() {
        LOGD(TAG, "Stopped TrickPlay Timer");
        if (null != mSeekbarTimer) {
            mSeekbarTimer.cancel();
        }
    }

    private void restartTrickplayTimer() {
        stopTrickplayTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        LOGD(TAG, "Restarted TrickPlay Timer");
    }

    @Override
    protected void onDestroy() {
        LOGD(TAG, "onDestroy is called");
        stopTrickplayTimer();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        LOGD(TAG, "onResume() was called");
        try {
            mCastManager = VideoCastManager.getInstance(VideoCastControllerActivity.this);
            boolean shouldFinish = !mCastManager.isConnected()
                    || (mCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_IDLE
                            && mCastManager.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED
                            && !mIsFresh);
            if (shouldFinish) {
                finish();
            }
        } catch (CastException e) {
            // logged already
        }

        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.incrementUiCounter();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        mCastManager.decrementUiCounter();
        mIsFresh = false;
        super.onPause();
    }

    private void loadViews() {
        mPageView = findViewById(R.id.pageView);
        mPlayPause = (ImageView) findViewById(R.id.imageView1);
        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        mLine1 = (TextView) findViewById(R.id.textView1);
        mLine2 = (TextView) findViewById(R.id.textView2);
        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        mControllers = findViewById(R.id.controllers);
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(" "); // without a title, the "<" won't show
        getSupportActionBar().setBackgroundDrawable(
                getResources().getDrawable(R.drawable.actionbar_bg_gradient_light));
    }

    private void updateControlersStatus(boolean enabled) {
        mControllers.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
    }

}
