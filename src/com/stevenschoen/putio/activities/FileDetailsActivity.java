package com.stevenschoen.putio.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.stevenschoen.putio.PutioFileData;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.fragments.FileDetails;

public class FileDetailsActivity extends SherlockFragmentActivity {
	private FileDetails fileDetailsFragment;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		PutioFileData fileData = (PutioFileData) getIntent().getExtras().getParcelable("fileData");
		
		setContentView(R.layout.filedetailsphone);
		
		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			fileDetailsFragment = new FileDetails(fileData);
			fragmentTransaction.add(R.id.DetailsHolder, fileDetailsFragment);
			fragmentTransaction.commit();
		} else {
			fileDetailsFragment = (FileDetails) getSupportFragmentManager().findFragmentById(R.id.DetailsHolder);
		}
		
		getSupportActionBar().setTitle(fileDetailsFragment.getOldFilename());
		
		IntentFilter intentFilter3 = new IntentFilter(
				Putio.CUSTOM_INTENT3);
		registerReceiver(fileDownloadUpdateReceiver, intentFilter3);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(getApplicationContext(), Putio.class);
			homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
			return true;
		}
		return (super.onOptionsItemSelected(menuItem));
	}
	
	private BroadcastReceiver fileDownloadUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("asdf", Integer.toString(intent.getExtras().getInt("id")));
			Log.d("asdf", Integer.toString(intent.getExtras().getInt("percent")));
			if (fileDetailsFragment.getFileId() == intent.getExtras().getInt("id")) {
//				fileDetailsFragment.updatePercent(intent.getExtras().getInt("percent"));
			}
		}
	};
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
	    super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(fileDownloadUpdateReceiver);
	}
}