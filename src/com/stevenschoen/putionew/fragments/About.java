package com.stevenschoen.putionew.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.activities.Putio;

public class About extends SherlockFragment {
	public static About newInstance() {
		About fragment = new About();
		
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.about, container, false);
		
		ImageView imageLogo = (ImageView) view.findViewById(R.id.image_about_logo);
		imageLogo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent goToSiteIntent = new Intent(Intent.ACTION_VIEW);
				goToSiteIntent.setData(Uri.parse("http://put.io/"));
				startActivity(goToSiteIntent);
			}
		});
		
		return view;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(getSherlockActivity(), Putio.class);
			homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
			return true;
		}
		return (super.onOptionsItemSelected(menuItem));
	}
}