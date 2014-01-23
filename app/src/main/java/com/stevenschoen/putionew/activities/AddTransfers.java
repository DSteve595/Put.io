package com.stevenschoen.putionew.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.fragments.AddTransferFile;
import com.stevenschoen.putionew.fragments.AddTransferUrl;

import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;

public class AddTransfers extends FragmentActivity {
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	PagerTitleStrip mPagerTitleStrip;
	
	int fragmentType;
	
	AddTransferUrl urlFragment;
	AddTransferFile fileFragment;
	
	SharedPreferences sharedPrefs;
	
	PutioUtils utils;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Putio_Dialog);
		setContentView(R.layout.dialog_addtransfer);
		
		if (getIntent().getAction() != null) {
			if (getIntent().getScheme().matches("magnet")) {
				fragmentType = PutioUtils.ADDTRANSFER_URL;
			} else if (getIntent().getScheme().matches("file")) {
				fragmentType = PutioUtils.ADDTRANSFER_FILE;
			}
		}
		
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		mViewPager = (ViewPager) findViewById(R.id.addtransfer_pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		mPagerTitleStrip = (PagerTitleStrip) findViewById(R.id.addtransfer_pager_title_strip);
		mPagerTitleStrip.setTextColor(Color.BLACK);
		if (fragmentType != 0) {
			mPagerTitleStrip.setVisibility(View.GONE);
			findViewById(R.id.titleDivider).setVisibility(View.VISIBLE);
		}
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String token = sharedPrefs.getString("token", null);
		if (token == null || token.isEmpty()) {
			Intent putioActivity = new Intent(this, Putio.class);
			startActivity(putioActivity);
			finish();
		}
		utils = new PutioUtils(token, sharedPrefs);
		
		TextView textTitle = (TextView) findViewById(R.id.dialog_title);
		Typeface robotoLight = Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf");
		textTitle.setTypeface(robotoLight);
		textTitle.setText(getString(R.string.addtransferstitle));
		
		Button addButton = (Button) findViewById(R.id.button_addtransfer_add);
		addButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (fragmentType == 0) {
					switch (mViewPager.getCurrentItem()) {
					case 0: addUrl(); break;
					case 1: addFile(); break;
					}
				} else if (mSectionsPagerAdapter.getItem(0) instanceof AddTransferUrl) {
					addUrl();
				} else if (mSectionsPagerAdapter.getItem(0) instanceof AddTransferFile) {
					addFile();
				}
			}
		});
		
		Button cancelButton = (Button) findViewById(R.id.button_addtransfer_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
	}
	
	private void addUrl() {
		if (!urlFragment.getEnteredUrls().isEmpty()) {
			Intent addTransferIntent = new Intent(AddTransfers.this, TransfersActivity.class);
			addTransferIntent.putExtra("mode", PutioUtils.ADDTRANSFER_URL);
			addTransferIntent.putExtra("url", urlFragment.getEnteredUrls());
			startActivity(addTransferIntent);
			finish();
		} else {
			Toast.makeText(AddTransfers.this, getString(R.string.nothingenteredtofetch), Toast.LENGTH_LONG).show();
		}
	}
	
	private void addFile() {
		if (fileFragment != null && fileFragment.getChosenTorrentUri() != null) {
            try {
                long size = getContentResolver()
                        .openFileDescriptor(fileFragment.getChosenTorrentUri(), "r").getStatSize();

                if (size <= FileUtils.ONE_MB) {
                    Intent addTransferIntent = new Intent(AddTransfers.this, TransfersActivity.class);
                    addTransferIntent.putExtra("mode", PutioUtils.ADDTRANSFER_FILE);
                    addTransferIntent.putExtra("torrenturi", fileFragment.getChosenTorrentUri());
                    startActivity(addTransferIntent);
                    finish();
                } else {
                    Toast.makeText(AddTransfers.this, getString(R.string.filetoobig), Toast.LENGTH_LONG).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
		} else {
			Toast.makeText(AddTransfers.this, getString(R.string.nothingenteredtofetch), Toast.LENGTH_LONG).show();
		}
	}
	
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				if (fragmentType == PutioUtils.ADDTRANSFER_URL) {
					if (urlFragment == null) {
						urlFragment = AddTransferUrl.newInstance();
						Bundle bundle = new Bundle();
						bundle.putString("url", getIntent().getDataString());
						urlFragment.setArguments(bundle);
					}
					return urlFragment;
				} else if (fragmentType == PutioUtils.ADDTRANSFER_FILE) {
					if (fileFragment == null) {
						fileFragment = AddTransferFile.newInstance();
						Bundle bundle = new Bundle();
						bundle.putString("filepath", getIntent().getDataString());
						fileFragment.setArguments(bundle);
					}
					return fileFragment;
				} else {
					if (urlFragment == null) {
						urlFragment = AddTransferUrl.newInstance();
					}
					return urlFragment;
				}
			case 1:
				if (fileFragment == null) {
					fileFragment = AddTransferFile.newInstance();
				}
				return fileFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			if (fragmentType != 0) {
				return 1;
			}
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return "URLs";
			case 1:
				return "Torrent file";
			}
			return null;
		}
	}
}