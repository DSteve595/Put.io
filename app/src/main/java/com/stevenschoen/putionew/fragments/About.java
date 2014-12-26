package com.stevenschoen.putionew.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.activities.Putio;

public class About extends Fragment {

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

        TextView textVersion = (TextView) view.findViewById(R.id.text_about_version);
        try {
            String version = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), 0).versionName;
            textVersion.setText(getString(R.string.version_x, version));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
		
		return view;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(getActivity(), Putio.class);
			NavUtils.navigateUpTo(getActivity(), homeIntent);
			return true;
		}
		return (super.onOptionsItemSelected(menuItem));
	}
}