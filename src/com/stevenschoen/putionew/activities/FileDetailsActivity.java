package com.stevenschoen.putionew.activities;

import org.apache.commons.io.FilenameUtils;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.cast.MediaRouteHelper;
import com.stevenschoen.putionew.PutioFileData;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.cast.CastService;
import com.stevenschoen.putionew.cast.CastService.CastCallbacks;
import com.stevenschoen.putionew.cast.CastService.CastServiceBinder;
import com.stevenschoen.putionew.cast.CastService.CastUpdateListener;
import com.stevenschoen.putionew.fragments.FileDetails;

public class FileDetailsActivity extends ActionBarActivity implements CastCallbacks, CastUpdateListener {
	
	CastService castService;
	MediaRouteActionProvider mediaRouteActionProvider;
	
	private FileDetails fileDetailsFragment;

	@SuppressLint("InlinedApi")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		PutioFileData fileData = (PutioFileData) getIntent().getExtras().getParcelable("fileData");
		
		setContentView(R.layout.filedetailsphone);
		
		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			Bundle fileDetailsBundle = new Bundle();
			fileDetailsBundle.putParcelable("fileData", fileData);
			fileDetailsFragment = (FileDetails) FileDetails.instantiate(
					this, FileDetails.class.getName(), fileDetailsBundle);
			fragmentTransaction.add(R.id.DetailsHolder, fileDetailsFragment);
			fragmentTransaction.commit();
		} else {
			fileDetailsFragment = (FileDetails) getSupportFragmentManager().findFragmentById(R.id.DetailsHolder);
		}
		
		getSupportActionBar().setTitle(fileData.name);
		
//		Intent castServiceIntent = new Intent(this, CastService.class);
//		startService(castServiceIntent);
//		bindService(castServiceIntent, castServiceConnection, Service.BIND_IMPORTANT);
		
		showCastBar(false);
	}
	
	private void initCast() {
		MediaRouteHelper.registerMinimalMediaRouteProvider(castService.getCastContext(), castService);
		supportInvalidateOptionsMenu();
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(menuItem);
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
	    super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (castService != null) {
			castService.removeListener(FileDetailsActivity.this);
			unbindService(castServiceConnection);
		}
	}
	
	private void showCastBar(boolean show) {
		View castBar = findViewById(R.id.castbar_holder);
		if (show) {
			castBar.setVisibility(View.VISIBLE);
		} else {
			castBar.setVisibility(View.GONE);
		}
	}
	
	private View getCastBar() {
		return findViewById(R.id.castbar_holder);
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
            
            castService.addListener(FileDetailsActivity.this);
        }

		@Override
		public void onServiceDisconnected(ComponentName name) {
			castService = null;
		}
    };

	@Override
	public void load(PutioFileData file, String url) {
		if (castService == null || castService.getSelectedDevice() == null) {
			PutioUtils.getStreamUrlAndPlay(this, file, url);
		} else {
			castService.loadAndPlayMedia(FilenameUtils.removeExtension(file.name), url);
			showCastBar(true);
		}
	}

	@Override
	public void onInvalidate() {
		if (castService.hasMediaLoaded()) {
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
}