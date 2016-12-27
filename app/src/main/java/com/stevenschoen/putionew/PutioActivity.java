package com.stevenschoen.putionew;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.stevenschoen.putionew.cast.BaseCastActivity;
import com.stevenschoen.putionew.files.FilesFragment;
import com.stevenschoen.putionew.fragments.Account;
import com.stevenschoen.putionew.model.files.PutioFile;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;
import com.stevenschoen.putionew.transfers.AddTransferActivity;
import com.stevenschoen.putionew.transfers.Transfers;
import com.stevenschoen.putionew.tv.TvActivity;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PutioActivity extends BaseCastActivity {

    public static final int TAB_ACCOUNT = 0;
    public static final int TAB_FILES = 1;
    public static final int TAB_TRANSFERS = 2;

	public static final String FRAGTAG_ACCOUNT = "account";
	public static final String FRAGTAG_FILES = "files";
	public static final String FRAGTAG_TRANSFERS = "transfers";

    private boolean init = false;

    private SharedPreferences sharedPrefs;

    public static final String checkCacheSizeIntent = "com.stevenschoen.putionew.checkcachesize";
    public static final String noNetworkIntent = "com.stevenschoen.putionew.nonetwork";

	private AHBottomNavigation bottomNavView;

    private View addTransferView;
	private boolean showingAddTransferFab = true;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UIUtils.isTV(this)) {
            Intent tvIntent = new Intent(this, TvActivity.class);
            startActivity(tvIntent);
            finish();
			return;
        }

        PutioApplication application = (PutioApplication) getApplication();
        if (application.isLoggedIn()) {
            init(savedInstanceState);
        } else {
            Intent setupIntent = new Intent(this, LoginActivity.class);
            startActivity(setupIntent);

            finish();
            return;
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (getIntent() != null) {
            handleIntent(getIntent());
        }

        IntentFilter checkCacheSizeIntentFilter = new IntentFilter(
                PutioActivity.checkCacheSizeIntent);
        IntentFilter noNetworkIntentFilter = new IntentFilter(
                PutioActivity.noNetworkIntent);

        registerReceiver(checkCacheSizeReceiver, checkCacheSizeIntentFilter);
        registerReceiver(noNetworkReceiver, noNetworkIntentFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

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
                    getFilesFragment().addSearch(query);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

		outState.putInt("currentTab", bottomNavView.getCurrentItem());
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
            case R.id.menu_logout:
                logOut();
                return true;
            case R.id.menu_about:
                Intent aboutIntent = new Intent(PutioActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void init(Bundle savedInstanceState) {
        init = true;

        setContentView(R.layout.main);

		if (savedInstanceState == null) {
			Account accountFragment = (Account) Fragment.instantiate(this, Account.class.getName());
			FilesFragment filesFragment = FilesFragment.Companion.newInstance(this, null);
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

		addTransferView = findViewById(R.id.main_addtransfer);
		addTransferView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PutioFile destinationFolder = null;
				if (bottomNavView.getCurrentItem() == TAB_FILES) {
					destinationFolder = getFilesFragment().getCurrentPage().getFile();
				}
				Intent addTransferIntent = new Intent(PutioActivity.this, AddTransferActivity.class);
				if (destinationFolder != null) {
					addTransferIntent.putExtra(AddTransferActivity.EXTRA_DESTINATION_FOLDER, destinationFolder);
				}
				startActivity(addTransferIntent);
			}
		});
		updateAddTransferFab(false);
    }

	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		if (fragment.getTag() != null) {
			switch (fragment.getTag()) {
				case FRAGTAG_FILES:
					((FilesFragment) fragment).setCallbacks(new FilesFragment.Callbacks() {
						@Override
						public void onSelectionStarted() {
							if (init) {
								updateAddTransferFab(true);
							}
						}

						@Override
						public void onSelectionEnded() {
							updateAddTransferFab(true);
						}

						@Override
						public void onCurrentFileChanged() {
							updateAddTransferFab(true);
						}
					});
					break;
				case FRAGTAG_TRANSFERS: {
					((Transfers) fragment).setCallbacks(new Transfers.Callbacks() {
						@Override
						public void onTransferSelected(@NotNull PutioTransfer transfer) {
							showFilesAndHighlightFile(transfer.saveParentId, transfer.fileId);
						}
					});
				}
			}
		}
	}

	private void updateAddTransferFab(boolean animate) {
		boolean shouldShow = shouldShowAddTransferFab();
		if (shouldShow && !showingAddTransferFab) {
			showingAddTransferFab = true;
			if (animate) {
				addTransferView.animate()
						.setInterpolator(new FastOutSlowInInterpolator())
						.alpha(1f)
						.scaleX(1f)
						.scaleY(1f);
			} else {
				addTransferView.setAlpha(1f);
				addTransferView.setScaleX(1f);
				addTransferView.setScaleY(1f);
			}
			addTransferView.setEnabled(true);
			addTransferView.setFocusable(true);
			addTransferView.setClickable(true);
		} else if (!shouldShow && showingAddTransferFab) {
			showingAddTransferFab = false;
			if (animate) {
				addTransferView.animate()
						.setInterpolator(new FastOutSlowInInterpolator())
						.alpha(0f)
						.scaleX(0f)
						.scaleY(0f);
			} else {
				addTransferView.setAlpha(0f);
				addTransferView.setScaleX(0f);
				addTransferView.setScaleY(0f);
			}
			addTransferView.setEnabled(false);
			addTransferView.setFocusable(false);
			addTransferView.setClickable(false);
		}
	}

	private boolean shouldShowAddTransferFab() {
		switch (bottomNavView.getCurrentItem()) {
			case TAB_ACCOUNT: return false;
			case TAB_FILES: {
				FilesFragment filesFragment = getFilesFragment();
				if (filesFragment.isSelecting()) {
					return false;
				} else {
					FilesFragment.Page currentPage = filesFragment.getCurrentPage();
					if (currentPage == null) {
						return true;
					} else {
						if (currentPage.getType() == FilesFragment.Page.Type.Search) {
							return false;
						} else {
							if (currentPage.getFile().isFolder()) {
								return true;
							} else {
								return false;
							}
						}
					}
				}
			}
			case TAB_TRANSFERS: return true;
			default: return true;
		}
	}

	public void logOut() {
        sharedPrefs.edit().remove("token").commit();
        finish();
        startActivity(getIntent());
    }

    private void setupLayout() {
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
				bottomNavView.post(new Runnable() {
					@Override
					public void run() {
						updateAddTransferFab(true);
					}
				});
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

    private FilesFragment getFilesFragment() {
        return (FilesFragment) getSupportFragmentManager().findFragmentByTag(FRAGTAG_FILES);
    }

    private Transfers getTransfersFragment() {
		return (Transfers) getSupportFragmentManager().findFragmentByTag(FRAGTAG_TRANSFERS);
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
			if (!getFilesFragment().goBack(true)) {
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
		ft.commitNow();
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

	@Override
	public Integer getCastMiniControllerContainerId() {
		return R.id.holder_castbar;
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