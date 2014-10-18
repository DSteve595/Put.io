package com.stevenschoen.putionew.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.commonsware.cwac.mediarouter.MediaRouteActionProvider;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.common.images.WebImage;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.castcompanionlibrary.widgets.MiniController;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.files.PutioFileData;
import com.stevenschoen.putionew.model.files.PutioSubtitle;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCastActivity extends Activity implements PutioApplication.CastCallbacks {

    private VideoCastManager videoCastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PutioApplication application = (PutioApplication) getApplication();
        videoCastManager = application.getVideoCastManager();

        if (shouldUpdateCastContext()) {
            videoCastManager.setContext(this);
        }
    }

    protected VideoCastManager getCastManager() {
        return videoCastManager;
    }

    public abstract boolean shouldUpdateCastContext();

    protected void initCastBar() {
        MiniController castBar = (MiniController) findViewById(R.id.castbar_holder);
        if (castBar != null && videoCastManager != null) {
            videoCastManager.addMiniController(castBar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cast, menu);

        MenuItem buttonMediaRoute = menu.findItem(R.id.menu_cast);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider)
                buttonMediaRoute.getActionProvider();
        PutioApplication application = (PutioApplication) getApplication();
        mediaRouteActionProvider.setRouteSelector(application.getMediaRouteSelector());

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
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

    @Override
    protected void onResume() {
        super.onResume();

        if (shouldUpdateCastContext()) {
            videoCastManager.setContext(this);
        }

        if (videoCastManager != null) {
            videoCastManager.incrementUiCounter();
        }
    }

    @Override
    protected void onPause() {
        if (videoCastManager != null) {
            videoCastManager.decrementUiCounter();
        }
        super.onPause();
    }

    public void load(PutioFileData file, String url, PutioUtils utils) {
        if (videoCastManager == null || !videoCastManager.isConnected()) {
            utils.getStreamUrlAndPlay(this, file, url);
        } else {
            GetSubtitlesAndPlayParams params = new GetSubtitlesAndPlayParams(this, videoCastManager, utils, file, url);
            new GetSubtitlesAndPlayTask(params).execute();
        }
    }

    static class GetSubtitlesAndPlayTask extends AsyncTask<Void, Void, MediaInfo> {
        private GetSubtitlesAndPlayParams params;

        private boolean shouldGetSubtitles;
        private Dialog gettingStreamDialog;

        public GetSubtitlesAndPlayTask(GetSubtitlesAndPlayParams params) {
            this.params = params;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            shouldGetSubtitles = (params.file.contentType.contains("video"));
            if (shouldGetSubtitles) {
                gettingStreamDialog = PutioUtils.PutioDialog(params.context,
                        params.context.getString(R.string.gettingstreamurltitle),
                        R.layout.dialog_loading);
                gettingStreamDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        GetSubtitlesAndPlayTask.this.cancel(true);
                    }
                });
                gettingStreamDialog.show();
            }
        }

        @Override
        protected MediaInfo doInBackground(Void... nothing) {
            MediaMetadata metaData = new MediaMetadata(params.file.contentType.contains("video") ?
                    MediaMetadata.MEDIA_TYPE_MOVIE : MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
            metaData.putString(MediaMetadata.KEY_TITLE, FilenameUtils.removeExtension(params.file.name));
            if (params.file.icon != null) metaData.addImage(new WebImage(Uri.parse(params.file.icon)));
            if (params.file.screenshot != null) metaData.addImage(new WebImage(Uri.parse(params.file.screenshot)));

            List<PutioSubtitle> subtitles = params.utils.getRestInterface().subtitles(params.file.id).getSubtitles();
            List<MediaTrack> tracks = new ArrayList<>();
            for (int i = 0; i < subtitles.size(); i++) {
                PutioSubtitle subtitle = subtitles.get(i);
                tracks.add(new MediaTrack.Builder(i, MediaTrack.TYPE_TEXT)
                        .setName(subtitle.getLanguage() + " - " + subtitle.getName())
                        .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                        .setContentId(subtitle.getUrl(PutioSubtitle.FORMAT_WEBVTT, params.file.id, params.utils.tokenWithStuff))
//                        .setLanguage(subtitle.getLanguage())
                        .build());
            }

            for (MediaTrack track : tracks) {
                Log.d("asdf", "url: " + track.getContentId());
            }

            return new MediaInfo.Builder(params.url)
                    .setContentType(params.file.contentType)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMediaTracks(tracks)
                    .setMetadata(metaData)
                    .build();
        }

        @Override
        protected void onPostExecute(MediaInfo mediaInfo) {
            if (shouldGetSubtitles && gettingStreamDialog != null) {
                gettingStreamDialog.dismiss();
            }

            try {
                if (mediaInfo != null
                        && params.videoCastManager != null && params.videoCastManager.isConnected()) {
                    params.videoCastManager.loadMedia(mediaInfo, true, 0);
                }
            } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
                e.printStackTrace();
            }
        }
    }

    static class GetSubtitlesAndPlayParams {
        Context context;
        VideoCastManager videoCastManager;
        PutioUtils utils;
        PutioFileData file;
        String url;

        GetSubtitlesAndPlayParams(Context context, VideoCastManager videoCastManager,
                                  PutioUtils utils, PutioFileData file, String url) {
            this.context = context;
            this.videoCastManager = videoCastManager;
            this.utils = utils;
            this.file = file;
            this.url = url;
        }
    }
}