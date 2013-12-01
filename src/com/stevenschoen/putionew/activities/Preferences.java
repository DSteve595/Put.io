package com.stevenschoen.putionew.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.stevenschoen.putionew.R;

public final class Preferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	// Check /res/xml/preferences.xml file for this preference
	private static final String PREFERENCE_KEY = "maxCacheSizeMb";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);

		// Register for changes (for example only)
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(PREFERENCE_KEY)) {
			sendBroadcast(new Intent(Putio.checkCacheSizeIntent));
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
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