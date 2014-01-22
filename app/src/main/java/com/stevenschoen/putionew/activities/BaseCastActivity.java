package com.stevenschoen.putionew.activities;

import org.apache.commons.io.FilenameUtils;
import com.google.cast.MediaRouteHelper;
import com.stevenschoen.putionew.PutioFileData;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.cast.CastService;
import com.stevenschoen.putionew.cast.CastService.CastCallbacks;
import com.stevenschoen.putionew.cast.CastService.CastServiceBinder;
import com.stevenschoen.putionew.cast.CastService.CastUpdateListener;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class BaseCastActivity extends ActionBarActivity implements CastCallbacks, CastUpdateListener {
	
	CastService castService;
	MediaRouteActionProvider mediaRouteActionProvider;
	
	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent castServiceIntent = new Intent(this, CastService.class);
		startService(castServiceIntent);
		bindService(castServiceIntent, castServiceConnection, Service.BIND_IMPORTANT);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.cast, menu);
		
		MenuItem buttonMediaRoute = menu.findItem(R.id.menu_cast);
		mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(buttonMediaRoute);
		if (castService != null) {
			mediaRouteActionProvider.setRouteSelector(castService.getMediaRouteSelector());
		}
		
		return true;
	}
	
	protected void initCast() {
		MediaRouteHelper.registerMinimalMediaRouteProvider(castService.getCastContext(), castService);
		supportInvalidateOptionsMenu();
	}
	
	protected void showCastBar(boolean show) {
		View castBar = getCastBar();
		if (show) {
			castBar.setVisibility(View.VISIBLE);
		} else {
			castBar.setVisibility(View.GONE);
		}
	}
	
	private View getCastBar() {
		return findViewById(R.id.castbar_holder);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
        		if (castService != null && castService.isPlaying()) {
            		castService.volumeUp();
            		return true;
        		}
            	return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
        		if (castService != null && castService.isPlaying()) {
            		castService.volumeDown();
            		return true;
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
            	return castService != null && castService.isPlaying();
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            	return castService != null && castService.isPlaying();
            default:
            	return super.onKeyUp(keyCode, event);
        }
	}
	
	protected void invalidateCast() {
		if (castService != null && castService.hasMediaLoaded()) {
			if (castService.isPlaying()) {
				showPause();
			} else {
				showPlay();
			}
			updateTitle();
			
			showCastBar(true);
		} else {
			showCastBar(false);
		}
	}
	
	public void showPlay() {
		showCastBar(true);
		ImageButton button = (ImageButton) getCastBar().findViewById(R.id.button_cast_playpause);
		button.setImageResource(R.drawable.ic_cast_play);
		button.setOnClickListener(onClickPlay);
	}

	public void showPause() {
		showCastBar(true);
		ImageButton button = (ImageButton) getCastBar().findViewById(R.id.button_cast_playpause);
		button.setImageResource(R.drawable.ic_cast_pause);
		button.setOnClickListener(onClickPause);
	}
	
	public void updateTitle() {
		showCastBar(true);
		TextView textTitle = (TextView) getCastBar().findViewById(R.id.text_cast_title);
		textTitle.setText(castService.getMessageStream().getTitle());
	}
	
	private ServiceConnection castServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        	CastServiceBinder binder = (CastServiceBinder) service;
            castService = binder.getService();
            initCast();
            
            castService.addListener(BaseCastActivity.this);
        }

		@Override
		public void onServiceDisconnected(ComponentName name) {
			castService = null;
		}
    };

	@Override
	public void load(PutioFileData file, String url) {
		if (castService == null || !castService.isCasting()) {
			PutioUtils.getStreamUrlAndPlay(this, file, url);
		} else {
			castService.loadAndPlayMedia(FilenameUtils.removeExtension(file.name), url);
			showCastBar(true);
		}
	}
	
	@Override
	public void onInvalidate() {
		invalidateCast();
	}
	
	@Override
	public void onMediaPlay() {
		showPause();
	}

	@Override
	public void onMediaPause() {
		showPlay();
	}
	
	public OnClickListener onClickPlay = new OnClickListener() {
		@Override
		public void onClick(View v) {
			castService.mediaPlay();
		}
	};
	
	public OnClickListener onClickPause = new OnClickListener() {
		@Override
		public void onClick(View v) {
			castService.mediaPause();
		}
	};
	
	protected void onDestroy() {
		if (castService != null) {
			castService.removeListener(this);
			unbindService(castServiceConnection);
		}
		
		super.onDestroy();
	};
}