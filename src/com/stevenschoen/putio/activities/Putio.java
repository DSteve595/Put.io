package com.stevenschoen.putio.activities;

import java.io.File;

import org.apache.commons.io.FileUtils;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.stevenschoen.putio.PutioFileUtils;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.UIUtils;
import com.stevenschoen.putio.activities.setup.Setup;
import com.stevenschoen.putio.fragments.FileDetails;
import com.stevenschoen.putio.fragments.Files;
import com.stevenschoen.putio.fragments.Transfers;

public class Putio extends SherlockFragmentActivity implements
		ActionBar.TabListener, Files.Callbacks, FileDetails.Callbacks {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;
	
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
	int requestCode;
	
	SharedPreferences sharedPrefs;
	
	Bundle savedInstanceState;

	private ActionBar actionBar;
	
	public static final String CUSTOM_INTENT1 = "com.stevenschoen.putio.invalidatelist";
	public static final String CUSTOM_INTENT2 = "com.stevenschoen.putio.checkcachesize";
	public static final String CUSTOM_INTENT3 = "com.stevenschoen.putio.filedownloadupdate";
	
	Files filesFragment;
	FileDetails fileDetailsFragment;
	SherlockFragment transfersFragment;
	
	private String titleFiles;
	private String titleTransfers;
	private String[] titles;

	private View tabletFilesView;
	private View tabletTransfersView;
	private int filesId;
	private int fileDetailsId;
	
	PutioFileUtils utils;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		this.savedInstanceState = savedInstanceState;
		
		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		if (UIUtils.isTablet(this)) {
			actionBar.setDisplayShowTitleEnabled(false);
		}
		
		titleFiles = getString(R.string.files).toUpperCase();
		titleTransfers = getString(R.string.transfers).toUpperCase();
		titles = new String[] {titleFiles, titleTransfers};
		
		if (!sharedPrefs.getBoolean("loggedIn", false)) {
			Intent setupIntent = new Intent(this, Setup.class);
			startActivityForResult(setupIntent, requestCode);
		} else {
			init();
		}
		
		IntentFilter intentFilter1 = new IntentFilter(
				Putio.CUSTOM_INTENT1);
		IntentFilter intentFilter2 = new IntentFilter(
				Putio.CUSTOM_INTENT2);
		IntentFilter intentFilter3 = new IntentFilter(
				Putio.CUSTOM_INTENT3);
		
		registerReceiver(invalidateReceiver, intentFilter1);
		registerReceiver(checkCacheSizeReceiver, intentFilter2);
		if (UIUtils.isTablet(this)) {
			registerReceiver(fileDownloadUpdateReceiver, intentFilter3);
		}
		
		sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		if (!UIUtils.isTablet(this)) {
			mViewPager.setCurrentItem(tab.getPosition());
		} else {
			switch (tab.getPosition()) {
				case 0: setContentView(tabletFilesView); break;
				case 1: setContentView(tabletTransfersView); break;
			}
		}
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem buttonSettings = menu.add("Settings");
		buttonSettings.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		buttonSettings.setIcon(android.R.drawable.ic_menu_preferences);
		buttonSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				Intent settingsIntent = new Intent(Putio.this, Preferences.class);
				Putio.this.startActivity(settingsIntent);
				return false;
			}
		});
		
		MenuItem buttonLogout = menu.add("Log out");
		buttonLogout.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		buttonLogout.setIcon(android.R.drawable.ic_menu_preferences);
		buttonLogout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				logOut();
				return false;
			}
		});
		
		return true;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		FragmentManager fm;
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			
			this.fm = fm;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				filesFragment = Files.newInstance();
				return filesFragment;
			case 1:
				transfersFragment = Transfers.newInstance();
				return transfersFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return titleFiles;
			case 1:
				return titleTransfers;
			}
			return null;
		}
	}
	
	private void init() {
		String token = sharedPrefs.getString("token", null);
		utils = new PutioFileUtils(token);
		
		if (!UIUtils.isTablet(this)) {
			setupPhoneLayout();
		} else {
			setupTabletLayout();
		}
	}
	
	public void logOut() {
		sharedPrefs.edit().remove("token").remove("loggedIn").commit();
		Intent setupIntent = new Intent(Putio.this, Setup.class);
		Putio.this.startActivity(setupIntent);
		finish();
	}
	
	private void setupPhoneLayout() {
		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());
		
		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab.
		// We can also use ActionBar.Tab#select() to do this if we have a
		// reference to the
		// Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter.
			// Also specify this Activity object, which implements the
			// TabListener interface, as the
			// listener for when this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}
	
	private void setupTabletLayout() {
		tabletFilesView = getLayoutInflater().inflate(R.layout.tablet_files, null);
		filesId = tabletFilesView.findViewById(R.id.fragment_files).getId();
		fileDetailsId = tabletFilesView.findViewById(R.id.fragment_details).getId();
		
		if (savedInstanceState == null) {
			filesFragment = Files.newInstance();
			getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(R.anim.slide_in_left,
							R.anim.slide_out_right)
					.add(filesId, filesFragment)
					.add(fileDetailsId, new SherlockFragment()).commit();
		} else {
			filesFragment = (Files) getSupportFragmentManager().findFragmentById(R.id.fragment_files);
		}
		
		tabletTransfersView = getLayoutInflater().inflate(R.layout.tablet_transfers, null);

		for (int i = 0; i < 2; i++) {
			actionBar.addTab(actionBar.newTab()
					.setText(titles[i])
					.setTabListener(this));
		}
	}
	
	@Override
	public void onFileSelected(int id) {
		fileDetailsFragment = new FileDetails(filesFragment.getFileAtId(id));
		
		getSupportFragmentManager()
				.beginTransaction()
				.setCustomAnimations(R.anim.slide_in_left,
						R.anim.slide_out_right)
				.replace(fileDetailsId, fileDetailsFragment).commit();
	}
	
	@Override
	public void onSomethingSelected() {
		if (fileDetailsFragment != null) {
			if (fileDetailsFragment.isAdded()) {
				removeFD(true);
			}
		}
	}
	
	@Override
	public void onFDCancelled() {
		removeFD(R.anim.slide_out_right);
	}

	@Override
	public void onFDFinished() {
		removeFD(R.anim.slide_out_left);
	}
	
	private void removeFD(int exitAnim) {
		getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(R.anim.slide_in_left, exitAnim)
				.remove(fileDetailsFragment).commit();
	}
	
	private void removeFD(boolean askIfSave) {
		if (askIfSave && !fileDetailsFragment.getOldFilename().equals(fileDetailsFragment.getNewFilename())) {
			final Dialog confirmChangesDialog = utils.confirmChangesDialog(this, fileDetailsFragment.getOldFilename());
			confirmChangesDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					removeFD(R.anim.slide_out_left);
				}
			});
			
			confirmChangesDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					removeFD(R.anim.slide_out_right);
				}
			});
			
			Button apply = (Button) confirmChangesDialog.findViewById(R.id.button_confirm_apply);
			apply.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					utils.saveFileToServer(Putio.this,
							fileDetailsFragment.getId(),
							fileDetailsFragment.getOldFilename(),
							fileDetailsFragment.getNewFilename());
					confirmChangesDialog.dismiss();
				}
			});
			
			Button cancel = (Button) confirmChangesDialog.findViewById(R.id.button_confirm_cancel);
			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					confirmChangesDialog.cancel();
				}
			});
			
			confirmChangesDialog.show();
		} else {
			removeFD(R.anim.slide_out_right);
		}
	}
	
	private BroadcastReceiver invalidateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
//			if (!UIUtils.isTablet(context)) {
				filesFragment.invalidateList();
//			}
		}
	};
	
	private BroadcastReceiver checkCacheSizeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			checkCacheSize();
		}
	};
	
	private BroadcastReceiver fileDownloadUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (fileDetailsFragment.getFileId() == intent.getExtras().getInt("id")) {
				fileDetailsFragment.updatePercent(intent.getExtras().getInt("percent"));
			}
		}
	};
	
	@Override
	public void onBackPressed() {
		if (UIUtils.isTablet(this)) {
			if (filesFragment.currentFolderId == 0) {
				super.onBackPressed();
			} else {
				filesFragment.goBack();
			}
		} else {
			if (hasWindowFocus()) {
				if (filesFragment.currentFolderId == 0) {
					super.onBackPressed();
				} else {
					filesFragment.goBack();
				}
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(invalidateReceiver);
		unregisterReceiver(checkCacheSizeReceiver);
		if (UIUtils.isTablet(this)) {
			unregisterReceiver(fileDownloadUpdateReceiver);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
//			Log.d("asdf", "ok");
			init();
		} else {
//			Log.d("asdf", "not ok, closing");
//			Log.d("asdf", Integer.toString(resultCode));
			finish();
		}
	}

	public void checkCacheSize() {
		int maxSize = sharedPrefs.getInt("maxCacheSizeMb", 20);
		File cache = getCacheDir();
		if (FileUtils.sizeOf(cache) >= (FileUtils.ONE_MB * maxSize)) {
			File[] cached = cache.listFiles();
			
			for (int i = 0; i < cached.length; i++) {
				if (!cached[i].getName().matches("0")) {
					FileUtils.deleteQuietly(cached[i]);
				}
			}
		}
	}
}