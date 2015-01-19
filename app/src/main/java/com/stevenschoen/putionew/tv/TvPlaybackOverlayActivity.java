/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.stevenschoen.putionew.tv;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.VideoView;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.files.PutioFile;

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
public class TvPlaybackOverlayActivity extends Activity implements
        TvPlaybackOverlayFragment.OnPlayPauseClickedListener {

    public static final String ARG_FILE = "ARG_FILE";

    private static final String TAG = "PlaybackOverlayActivity";

    private static final double MEDIA_HEIGHT = 0.95;
    private static final double MEDIA_WIDTH = 0.95;

    private VideoView mVideoView;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;
    private PutioFile mFile;

    public static void launch(Activity activity, PutioFile file) {
        Intent intent = new Intent(activity, TvPlaybackOverlayActivity.class);
        intent.putExtra(ARG_FILE, file);
        activity.startActivity(intent);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFile = getIntent().getParcelableExtra(ARG_FILE);

        setContentView(R.layout.tv_playback_activity);
        loadViews();
        setupCallbacks();
    }

    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.videoView);
        PutioUtils utils = ((PutioApplication) getApplication()).getPutioUtils();
        String streamUrl = mFile.getStreamUrl(utils, !mFile.isMp4());
        mVideoView.setVideoPath(streamUrl);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVideoView.suspend();
    }

    /**
     * Implementation of OnPlayPauseClickedListener
     */
    public void onFragmentPlayPause(int position, Boolean playPause) {

        if (position == 0 || mPlaybackState == PlaybackState.IDLE) {
            setupCallbacks();
            mPlaybackState = PlaybackState.IDLE;
        }

        if (playPause && mPlaybackState != PlaybackState.PLAYING) {
            mPlaybackState = PlaybackState.PLAYING;
            if (position > 0) {
                mVideoView.seekTo(position);
                mVideoView.start();
            }
        } else {
            mPlaybackState = PlaybackState.PAUSED;
            mVideoView.pause();
        }
    }

    /**
     * Implementation of OnPlayPauseClickedListener
     */
    public void onFragmentFfwRwd(int position) {

        Log.d(TAG, "seek current time: " + position);
        if (mPlaybackState == PlaybackState.PLAYING) {
            if (position > 0) {
                mVideoView.seekTo(position);
                mVideoView.start();
            }
        }
    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
//                String msg = "";
//                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
//                    msg = getString(R.string.video_error_media_load_timeout);
//                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
//                    msg = getString(R.string.video_error_server_inaccessible);
//                } else {
//                    msg = getString(R.string.video_error_unknown_error);
//                }
                mVideoView.stopPlayback();
                mPlaybackState = PlaybackState.IDLE;
                return false;
            }
        });


        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    mVideoView.start();
                }
            }
        });


        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlaybackState = PlaybackState.IDLE;
            }
        });

    }

    /*
     * List of various states that we can be in
     */
    public static enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE;
    }

}
