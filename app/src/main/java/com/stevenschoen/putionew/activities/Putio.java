package com.stevenschoen.putionew.activities;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.files.NewFilesFragment;
import com.stevenschoen.putionew.fragments.Account;
import com.stevenschoen.putionew.fragments.Transfers;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;
import com.stevenschoen.putionew.tv.TvActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class Putio extends BaseCastActivity implements Transfers.Callbacks {

    public static final int TAB_ACCOUNT = 0;
    public static final int TAB_FILES = 1;
    public static final int TAB_TRANSFERS = 2;

	public static final String FRAGTAG_ACCOUNT = "account";
	public static final String FRAGTAG_FILES = "files";
	public static final String FRAGTAG_TRANSFERS = "transfers";

    private boolean init = false;

    private SharedPreferences sharedPrefs;

    public static final String checkCacheSizeIntent = "com.stevenschoen.putionew.checkcachesize";
    public static final String fileDownloadUpdateIntent = "com.stevenschoen.putionew.filedownloadupdate";
    public static final String noNetworkIntent = "com.stevenschoen.putionew.nonetwork";

	private AHBottomNavigation bottomNavView;

    private View buttonAddTransfer;
	private boolean showingAddTransfer = true;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UIUtils.isTV(this)) {
            Intent tvIntent = new Intent(this, TvActivity.class);
            startActivity(tvIntent);
            finish();
        }

        PutioApplication application = (PutioApplication) getApplication();
        if (application.isLoggedIn()) {
            init(savedInstanceState);
        } else {
            Intent setupIntent = new Intent(this, Login.class);
            startActivity(setupIntent);

            finish();
            return;
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (getIntent() != null) {
            handleIntent(getIntent());
        }

        IntentFilter checkCacheSizeIntentFilter = new IntentFilter(
                Putio.checkCacheSizeIntent);
        IntentFilter noNetworkIntentFilter = new IntentFilter(
                Putio.noNetworkIntent);

        registerReceiver(checkCacheSizeReceiver, checkCacheSizeIntentFilter);
        registerReceiver(noNetworkReceiver, noNetworkIntentFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (init) {
            int goToTab = intent.getIntExtra("goToTab", -1);
            if (goToTab != -1) {
                selectTab(goToTab, true);
            }

            if (intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SEARCH) && getFilesFragment() != null) {
                    String query = intent.getStringExtra(SearchManager.QUERY);
//                    getFilesFragment().initSearch(query);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            outState.putInt("currentTab", bottomNavView.getCurrentItem());
		} catch (NullPointerException e) { }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_putio, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent settingsIntent = new Intent(Putio.this, Preferences.class);
                Putio.this.startActivity(settingsIntent);
                return true;
            case R.id.menu_logout:
                logOut();
                return true;
            case R.id.menu_about:
                Intent aboutIntent = new Intent(Putio.this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void init(Bundle savedInstanceState) {
        init = true;

        initCast();

        setContentView(R.layout.main);

		if (savedInstanceState == null) {
			Account accountFragment = (Account) Fragment.instantiate(this, Account.class.getName());
			NewFilesFragment filesFragment = NewFilesFragment.Companion.newInstance(null);
			Transfers transfersFragment = (Transfers) Fragment.instantiate(this, Transfers.class.getName());
			getSupportFragmentManager().beginTransaction()
					.add(R.id.main_content_holder, accountFragment, FRAGTAG_ACCOUNT)
					.detach(accountFragment)
					.add(R.id.main_content_holder, filesFragment, FRAGTAG_FILES)
					.detach(filesFragment)
					.add(R.id.main_content_holder, transfersFragment, FRAGTAG_TRANSFERS)
					.detach(transfersFragment)
					.commitNow();
		}

        try {
            ((PutioApplication) getApplication()).buildUtils();
        } catch (PutioUtils.NoTokenException e) {
            e.printStackTrace();
        }

		setupLayout();

        int navItem = TAB_FILES;
        if (savedInstanceState != null) {
            navItem = savedInstanceState.getInt("currentTab", -1);
        }
        selectTab(navItem, false);

        buttonAddTransfer = findViewById(R.id.button_addtransfer);
        buttonAddTransfer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addTransferActivityIntent = new Intent(Putio.this, AddTransfers.class)
						.putExtra(AddTransfers.EXTRA_STARTING_FOLDER, getFilesFragment().getCurrentFolder());
                Bundle options = ActivityOptionsCompat.makeScaleUpAnimation(buttonAddTransfer,
                        0, 0,
                        buttonAddTransfer.getWidth(), buttonAddTransfer.getHeight()).toBundle();
                ActivityCompat.startActivity(Putio.this, addTransferActivityIntent, options);
            }
        });
		if (getFilesFragment().isSelecting()) {
			hideAddTransferFab(false);
		}
    }

	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		if (fragment.getTag().equals(FRAGTAG_FILES)) {
			((NewFilesFragment) fragment).setCallbacks(new NewFilesFragment.Callbacks() {
				@Override
				public void onSelectionStarted() {
					if (init) {
						hideAddTransferFab(true);
					}
				}

				@Override
				public void onSelectionEnded() {
					showAddTransferFab(true);
				}
			});
		}
	}

	private void showAddTransferFab(boolean animate) {
		if (!showingAddTransfer) {
			showingAddTransfer = true;
			if (animate) {
				buttonAddTransfer.animate()
						.scaleX(1f)
						.scaleY(1f);
			} else {
				buttonAddTransfer.setScaleX(1f);
				buttonAddTransfer.setScaleY(1f);
			}
			buttonAddTransfer.setEnabled(true);
			buttonAddTransfer.setFocusable(true);
			buttonAddTransfer.setClickable(true);
		}
	}

	private void hideAddTransferFab(boolean animate) {
		if (showingAddTransfer) {
			showingAddTransfer = false;
			if (animate) {
				buttonAddTransfer.animate()
						.scaleX(0f)
						.scaleY(0f);
			} else {
				buttonAddTransfer.setScaleX(0f);
				buttonAddTransfer.setScaleY(0f);
			}
			buttonAddTransfer.setEnabled(false);
			buttonAddTransfer.setFocusable(false);
			buttonAddTransfer.setClickable(false);
		}
	}

	public void logOut() {
        sharedPrefs.edit().remove("token").commit();
        finish();
        startActivity(getIntent());
    }

    private void setupLayout() {
        initCastBar();

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

		bottomNavView = (AHBottomNavigation) findViewById(R.id.main_bottom_nav);
		bottomNavView.setDefaultBackgroundColor(Color.parseColor("#F8F8F8"));
		bottomNavView.setAccentColor(Color.BLACK);
		bottomNavView.setInactiveColor(Color.parseColor("#80000000"));
		bottomNavView.addItem(new AHBottomNavigationItem(getString(R.string.account), R.drawable.ic_nav_account));
		bottomNavView.addItem(new AHBottomNavigationItem(getString(R.string.files), R.drawable.ic_nav_files));
		bottomNavView.addItem(new AHBottomNavigationItem(getString(R.string.transfers), R.drawable.ic_nav_transfers));
		bottomNavView.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
			@Override
			public boolean onTabSelected(int position, boolean wasSelected) {
				switch (position) {
					case TAB_ACCOUNT:
					case TAB_FILES:
					case TAB_TRANSFERS: showFragment(position, true); return true;
				}
				return false;
			}
		});
    }

    public void showFilesAndHighlightFile(long parentId, long id) {
        selectTab(TAB_FILES, true);
//        getFilesFragment().highlightFile(parentId, id);
    }

	private Account getAccountFragment() {
		return (Account) getSupportFragmentManager().findFragmentByTag(FRAGTAG_ACCOUNT);
	}

    private NewFilesFragment getFilesFragment() {
        return (NewFilesFragment) getSupportFragmentManager().findFragmentByTag(FRAGTAG_FILES);
    }

    private Transfers getTransfersFragment() {
		return (Transfers) getSupportFragmentManager().findFragmentByTag(FRAGTAG_TRANSFERS);
    }

    @Override
    public void onTransferSelected(PutioTransfer transfer) {
        showFilesAndHighlightFile(transfer.saveParentId, transfer.fileId);
    }

    private BroadcastReceiver checkCacheSizeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            checkCacheSize();
        }
    };

    private BroadcastReceiver noNetworkReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            getTransfersFragment().setHasNetwork(false);
        }
    };

	@Override
    public void onBackPressed() {
        if (bottomNavView.getCurrentItem() == TAB_FILES) {
			if (!getFilesFragment().goBack()) {
				super.onBackPressed();
			}
		} else {
            super.onBackPressed();
        }
    }

	private void showFragment(int position, boolean animate) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (animate) {
			ft.setCustomAnimations(R.anim.bottomnav_enter, R.anim.bottomnav_exit);
		}
		switch (position) {
			case TAB_ACCOUNT: {
				ft.attach(getAccountFragment());
				ft.detach(getFilesFragment());
				ft.detach(getTransfersFragment());
			} break;
			case TAB_FILES: {
				ft.detach(getAccountFragment());
				ft.attach(getFilesFragment());
				ft.detach(getTransfersFragment());
			} break;
			case TAB_TRANSFERS: {
				ft.detach(getAccountFragment());
				ft.detach(getFilesFragment());
				ft.attach(getTransfersFragment());
			} break;
		}
		ft.commit();
	}

    private void selectTab(int position, boolean animate) {
        if (bottomNavView.getCurrentItem() != position) {
            bottomNavView.setCurrentItem(position, false);
			showFragment(position, animate);
        }
    }

    @Override
    protected void onDestroy() {
		if (init) {
			unregisterReceiver(checkCacheSizeReceiver);
			unregisterReceiver(noNetworkReceiver);
		}

        super.onDestroy();
    }

    public void checkCacheSize() {
        int maxSize = sharedPrefs.getInt("maxCacheSizeMb", 20);
        File cache = getCacheDir();
        if (FileUtils.sizeOf(cache) >= (FileUtils.ONE_MB * maxSize)) {
            File[] cacheFiles = cache.listFiles();
			for (File file : cacheFiles) {
				if (!file.getName().equals("0")) {
					FileUtils.deleteQuietly(file);
				}
			}
        }
    }
}