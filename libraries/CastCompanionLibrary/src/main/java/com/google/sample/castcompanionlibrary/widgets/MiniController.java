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

package com.google.sample.castcompanionlibrary.widgets;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.MediaStatus;
import com.google.sample.castcompanionlibrary.R;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.OnFailedListener;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.net.URL;

/**
 * A compound component that provides a superset of functionalities required for the global access
 * requirement. This component provides an image for the album art, a play/pause button, a seekbar
 * for trick-play with current time and duration and a mute/unmute button. Clients can add this
 * compound component to their layout xml and register that with the instance of {@link CastMnager}
 * by using the following pattern:<br/>
 * 
 * <pre>
 * mMiniController = (MiniController) findViewById(R.id.miniController1);
 * mCastManager.addMiniController(mMiniController);
 * mMiniController.setOnMiniControllerChangedListener(mCastManager);
 * </pre>
 * 
 * Then the {@link VideoCastManager} will manage the behavior, including its state and metadata and
 * interactions.
 */
public class MiniController extends RelativeLayout implements IMiniController {

    private static final String TAG = "MiniController";
    protected ImageView mIcon;
    protected TextView mTitle;
    protected TextView mSubTitle;
    protected ImageView mPlayPause;
    protected ProgressBar mLoading;
    public static final int PLAYBACK = 1;
    public static final int PAUSE = 2;
    public static final int IDLE = 3;
    private OnMiniControllerChangedListener listener;
    private Uri mIconUri;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private View mContainer;

    /**
     * @param context
     * @param attrs
     */
    public MiniController(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.mini_controller, this);
        mPauseDrawable = getResources().getDrawable(R.drawable.ic_av_pause_light);
        mPlayDrawable = getResources().getDrawable(R.drawable.ic_av_play_light);
        loadViews();
        setupCallbacks();
    }

    /**
     * Sets the listener that should be notified when a relevant event is fired from this component.
     * Clients can register the {@link VideoCastManager} instance to be the default listener so it
     * can control the remote media playback.
     * 
     * @param listener
     */
    @Override
    public void setOnMiniControllerChangedListener(OnMiniControllerChangedListener listener) {
        if (null != listener) {
            this.listener = listener;
        }
    }

    /**
     * Removes the listener that was registered by {@link setOnMiniControllerChangedListener}
     * 
     * @param listener
     */
    public void removeOnMiniControllerChangedListener(OnMiniControllerChangedListener listener) {
        if (null != listener && this.listener == listener) {
            this.listener = null;
        }
    }

    private void setupCallbacks() {

        mPlayPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (null != listener) {
                    setLoadingVisibility(true);
                    try {
                        listener.onPlayPauseClicked(v);
                    } catch (CastException e) {
                        listener.onFailed(R.string.failed_perform_action, -1);
                    } catch (TransientNetworkDisconnectionException e) {
                        listener.onFailed(R.string.failed_no_connection_trans, -1);
                    } catch (NoConnectionException e) {
                        listener.onFailed(R.string.failed_no_connection, -1);
                    }
                }
            }
        });

        mContainer.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (null != listener) {
                    setLoadingVisibility(false);
                    try {
                        listener.onTargetActivityInvoked(mIcon.getContext());
                    } catch (Exception e) {
                        listener.onFailed(R.string.failed_perform_action, -1);
                    }
                }

            }
        });
    }

    /**
     * Constructor
     * 
     * @param context
     */
    public MiniController(Context context) {
        super(context);
        loadViews();
    }

    private void setIcon(Bitmap bm) {
        mIcon.setImageBitmap(bm);
    }

    @Override
    public void setIcon(Uri uri) {
        if (null != mIconUri && mIconUri.equals(uri)) {
            return;
        }

        mIconUri = uri;
        new Thread(new Runnable() {
            Bitmap bm = null;

            @Override
            public void run() {
                try {
                    URL imgUrl = new URL(mIconUri.toString());
                    bm = BitmapFactory.decodeStream(imgUrl.openStream());
                } catch (Exception e) {
                    LOGE(TAG, "setIcon(): Failed to load the image with url: " +
                            mIconUri + ", using the default one", e);
                    bm = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);
                }
                mIcon.post(new Runnable() {

                    @Override
                    public void run() {
                        setIcon(bm);
                    }
                });

            }
        }).start();
    }

    @Override
    public void setTitle(String title) {
        mTitle.setText(title);
    }

    @Override
    public void setSubTitle(String subTitle) {
        mSubTitle.setText(subTitle);
    }

    @Override
    public void setPlaybackStatus(int state) {

        if (state == MediaStatus.PLAYER_STATE_PLAYING) {
            mPlayPause.setVisibility(View.VISIBLE);
            mPlayPause.setImageDrawable(mPauseDrawable);
            setLoadingVisibility(false);
        } else if (state == MediaStatus.PLAYER_STATE_PAUSED) {
            mPlayPause.setVisibility(View.VISIBLE);
            mPlayPause.setImageDrawable(mPlayDrawable);
            setLoadingVisibility(false);
        } else if (state == MediaStatus.PLAYER_STATE_IDLE) {
            mPlayPause.setVisibility(View.INVISIBLE);
            setLoadingVisibility(false);
        } else if (state == MediaStatus.PLAYER_STATE_BUFFERING) {
            mPlayPause.setVisibility(View.INVISIBLE);
            setLoadingVisibility(true);
        } else {
            mPlayPause.setVisibility(View.INVISIBLE);
            setLoadingVisibility(false);
        }

    }

    @Override
    public boolean isVisible() {
        return isShown();
    }

    private void loadViews() {
        mIcon = (ImageView) findViewById(R.id.iconView);
        mTitle = (TextView) findViewById(R.id.titleView);
        mSubTitle = (TextView) findViewById(R.id.subTitleView);
        mPlayPause = (ImageView) findViewById(R.id.playPauseView);
        mLoading = (ProgressBar) findViewById(R.id.loadingView);
        mContainer = findViewById(R.id.bigContainer);
    }

    private void setLoadingVisibility(boolean show) {
        mLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * The interface for a listener that will be called when user interacts with the
     * {@link MiniController}, like clicking on the play/pause button, etc.
     */
    public interface OnMiniControllerChangedListener extends OnFailedListener {

        /**
         * Notification that user has clicked on the Play/Pause button
         * 
         * @param v
         * @throws TransientNetworkDisconnectionException
         * @throws NoConnectionException
         * @throws CastException
         */
        public void onPlayPauseClicked(View v) throws CastException,
                TransientNetworkDisconnectionException, NoConnectionException;

        /**
         * Notification that the user has clicked on the album art
         * 
         * @param context
         * @throws NoConnectionException
         * @throws TransientNetworkDisconnectionException
         */
        public void onTargetActivityInvoked(Context context)
                throws TransientNetworkDisconnectionException, NoConnectionException;

    }

}
