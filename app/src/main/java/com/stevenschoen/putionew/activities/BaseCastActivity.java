package com.stevenschoen.putionew.activities;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.castcompanionlibrary.widgets.MiniController;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.files.PutioFileData;

import org.apache.commons.io.FilenameUtils;

public abstract class BaseCastActivity extends ActionBarActivity implements PutioApplication.CastCallbacks {

    private VideoCastManager videoCastManager;
	private MiniController castBar;

    private boolean initCast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initCast = false;

        PutioApplication application = (PutioApplication) getApplication();
        if (application.isLoggedIn()) {
            initCast();
        }
    }

    protected VideoCastManager getCastManager() {
        return videoCastManager;
    }

    public abstract boolean shouldUpdateCastContext();

    protected void initCast() {
        if (!initCast) {
            PutioApplication application = (PutioApplication) getApplication();
            videoCastManager = application.getVideoCastManager();

            if (shouldUpdateCastContext()) {
                videoCastManager.setContext(this);
            }

            supportInvalidateOptionsMenu();

            initCast = true;
        }
    }

    protected void initCastBar() {
        castBar = (MiniController) findViewById(R.id.castbar_holder);
        if (castBar != null && videoCastManager != null) {
            videoCastManager.addMiniController(castBar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (initCast) {
            getMenuInflater().inflate(R.menu.cast, menu);

            MenuItem buttonMediaRoute = menu.findItem(R.id.menu_cast);
            MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider)
                    MenuItemCompat.getActionProvider(buttonMediaRoute);
            PutioApplication application = (PutioApplication) getApplication();
            mediaRouteActionProvider.setRouteSelector(application.getMediaRouteSelector());
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (initCast) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    try {
                        if (videoCastManager != null && videoCastManager.isRemoteMoviePlaying()) {
                            videoCastManager.updateVolume(1);
                            return true;
                        }
                    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                        return super.onKeyDown(keyCode, event);
                    }
                    return super.onKeyDown(keyCode, event);
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    try {
                        if (videoCastManager != null && videoCastManager.isRemoteMoviePlaying()) {
                            videoCastManager.updateVolume(1);
                            return true;
                        }
                    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                        return super.onKeyDown(keyCode, event);
                    }
                    return super.onKeyDown(keyCode, event);
                default:
                    return super.onKeyDown(keyCode, event);
            }
        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (initCast) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    try {
                        return videoCastManager != null && videoCastManager.isRemoteMoviePlaying();
                    } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                        return super.onKeyUp(keyCode, event);
                    }
                default:
                    return super.onKeyUp(keyCode, event);
            }
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (initCast) {
            if (shouldUpdateCastContext()) {
                videoCastManager.setContext(this);
            }

            if (videoCastManager != null) {
                videoCastManager.incrementUiCounter();
            }
        }
    }

    @Override
    protected void onPause() {
        if (initCast) {
            if (videoCastManager != null) {
                videoCastManager.decrementUiCounter();
            }
        }

        super.onPause();
    }

    public void load(PutioFileData file, String url, PutioUtils utils) {
        if (videoCastManager == null || !videoCastManager.isConnected()) {
            utils.getStreamUrlAndPlay(this, file, url);
        } else {
            MediaMetadata metaData = new MediaMetadata(file.isVideo() ?
                    MediaMetadata.MEDIA_TYPE_MOVIE : MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
            metaData.putString(MediaMetadata.KEY_TITLE, FilenameUtils.removeExtension(file.name));
            if (file.icon != null) metaData.addImage(new WebImage(Uri.parse(file.icon)));
            if (file.screenshot != null) metaData.addImage(new WebImage(Uri.parse(file.screenshot)));

            String subtitleUrl = PutioUtils.baseUrl + "files/" + file.id + "/subtitles/default" +
					utils.tokenWithStuff + "&format=webvtt";

			MediaInfo mediaInfo = new MediaInfo.Builder(url)
                    .setContentType(file.contentType)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(metaData)
                    .build();
            try {
                videoCastManager.loadMedia(mediaInfo, true, 0);
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                e.printStackTrace();
            }
        }
    }
}