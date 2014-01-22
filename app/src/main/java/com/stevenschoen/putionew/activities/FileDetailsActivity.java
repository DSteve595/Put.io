package com.stevenschoen.putionew.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.stevenschoen.putionew.PutioFileData;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.fragments.FileDetails;

public class FileDetailsActivity extends BaseCastActivity {
	
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
			Bundle fileDetailsBundle = new Bundle();
			fileDetailsBundle.putParcelable("fileData", fileData);
			fileDetailsFragment = (FileDetails) FileDetails.instantiate(
					this, FileDetails.class.getName(), fileDetailsBundle);
			fragmentTransaction.add(R.id.DetailsHolder, fileDetailsFragment);
			fragmentTransaction.commit();
		} else {
			fileDetailsFragment = (FileDetails) getSupportFragmentManager().findFragmentById(R.id.DetailsHolder);
		}
		
		setTitle(fileData.name);
		
		showCastBar(false);
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
		
		return super.onOptionsItemSelected(menuItem);
	}
}