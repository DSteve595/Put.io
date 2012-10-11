package com.stevenschoen.putio.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.stevenschoen.putio.R;

public final class Preferences extends SherlockPreferenceActivity implements
		OnSharedPreferenceChangeListener {

	// Check /res/xml/preferences.xml file for this preference
	private static final String PREFERENCE_KEY = "maxCacheSizeMb";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		addPreferencesFromResource(R.xml.preferences);

		// Register for changes (for example only)
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(PREFERENCE_KEY)) {
			sendBroadcast(new Intent(Putio.CUSTOM_INTENT2));
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(this, Putio.class);
			homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
			return true;
		}
		return (super.onOptionsItemSelected(menuItem));
	}

	@Override
	protected void onDestroy() {
		// Unregister from changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}
}