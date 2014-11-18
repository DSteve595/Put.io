package com.stevenschoen.putionew.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stevenschoen.putionew.PublicFragmentPagerAdapter;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioNotification;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.SlidingTabLayout;
import com.stevenschoen.putionew.SwipeDismissTouchListener;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.fragments.Account;
import com.stevenschoen.putionew.fragments.Files;
import com.stevenschoen.putionew.fragments.FilesAndFileDetails;
import com.stevenschoen.putionew.fragments.Transfers;
import com.stevenschoen.putionew.model.files.PutioFile;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

public class Putio extends BaseCastActivity implements FilesAndFileDetails.Callbacks,
        Transfers.Callbacks, DestinationFilesDialog.Callbacks {

    public static final int TAB_ACCOUNT = 0;
    public static final int TAB_FILES = 1;
    public static final int TAB_TRANSFERS = 2;

    private boolean init = false;

    private SlidingTabLayout tabs;
    private ViewPager pager;

    int requestCode;

    private SharedPreferences sharedPrefs;

    private Bundle savedInstanceState;

    public static final String checkCacheSizeIntent = "com.stevenschoen.putionew.checkcachesize";
    public static final String fileDownloadUpdateIntent = "com.stevenschoen.putionew.filedownloadupdate";
    public static final String noNetworkIntent = "com.stevenschoen.putionew.nonetwork";

    private View buttonAddTransfer;

    private PutioNotification[] notifs;

    private PutioUtils utils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PutioApplication application = (PutioApplication) getApplication();
        if (application.isLoggedIn()) {
            init();
        } else {
            Intent setupIntent = new Intent(this, Login.class);
            startActivityForResult(setupIntent, requestCode);
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        this.savedInstanceState = savedInstanceState;

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
                    getFilesFragment().initSearch(query);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            outState.putInt("currentTab", pager.getCurrentItem());
		} catch (NullPointerException e) { }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.putio, menu);

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

    @Override
    public void onDestinationFolderSelected(PutioFile folder) {
        getFilesFragment().onDestinationFolderSelected(folder);
    }

    public class PutioMainPagerAdapter extends PublicFragmentPagerAdapter {

        public PutioMainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_ACCOUNT:
                    return Fragment.instantiate(Putio.this, Account.class.getName());
                case TAB_FILES:
                    if (UIUtils.isTablet(Putio.this)) {
                        FilesAndFileDetails filesAndFileDetailsFragment = (FilesAndFileDetails)
                                Fragment.instantiate(Putio.this, FilesAndFileDetails.class.getName());
                        filesAndFileDetailsFragment.setCallbacks(Putio.this);
                        return filesAndFileDetailsFragment;
                    } else {
                        return Fragment.instantiate(Putio.this, Files.class.getName());
                    }
                case TAB_TRANSFERS:
                    return Fragment.instantiate(Putio.this, Transfers.class.getName());
            }

            return null;
        }

        @Override
        public float getPageWidth(int position) {
            if (position == TAB_ACCOUNT && UIUtils.isTablet(Putio.this)) {
                return 0.5f;
            }
            return super.getPageWidth(position);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_ACCOUNT:
                    return getString(R.string.account);
                case TAB_FILES:
                    return getString(R.string.files);
                case TAB_TRANSFERS:
                    return getString(R.string.transfers);
            }
            return null;
        }
    }

    private void init() {
        init = true;

        initCast();

        setContentView(R.layout.main);

        try {
            ((PutioApplication) getApplication()).buildUtils();
        } catch (PutioUtils.NoTokenException e) {
            e.printStackTrace();
        }
        this.utils = ((PutioApplication) getApplication()).getPutioUtils();

        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setSelectedIndicatorColors(getResources().getColor(R.color.putio_dark));
        tabs.setDistributeEvenly(true);

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
                Intent addTransferActivityIntent = new Intent(Putio.this, AddTransfers.class);
                Bundle options = ActivityOptions.makeScaleUpAnimation(buttonAddTransfer,
                        0, 0,
                        buttonAddTransfer.getWidth(), buttonAddTransfer.getHeight()).toBundle();
                startActivity(addTransferActivityIntent, options);
            }
        });

        PutioUtils.setupFab(buttonAddTransfer);

        class NotificationTask extends AsyncTask<Void, Void, PutioNotification[]> {

            @Override
            protected PutioNotification[] doInBackground(Void... nothing) {
                try {
                    InputStream is = utils.getNotificationsJsonData();
                    String string = PutioUtils.convertStreamToString(is);

                    JSONObject json = new JSONObject(string);

                    JSONArray notifications = json.getJSONArray("notifications");
                    notifs = new PutioNotification[notifications.length()];
                    for (int i = 0; i < notifications.length(); i++) {
                        JSONObject obj = notifications.getJSONObject(i);
//						prime numbers yay
                        boolean show = sharedPrefs.getInt("readNotifs", 1) % obj.getInt("id") != 0;
                        notifs[i] = new PutioNotification(obj.getInt("id"), obj.getString("text"),
                                show);
                    }
                    return notifs;
                } catch (Exception e) {
					e.printStackTrace();
                    return null;
                }
            }

            @SuppressLint("NewApi")
            @Override
            protected void onPostExecute(final PutioNotification[] result) {
                if (result != null) {
                    for (int i = 0; i < result.length; i++) {
                        if (result[i].show) {
                            final ViewGroup ll = (ViewGroup) getWindow().getDecorView().
                                    findViewById(R.id.layout_root);
                            final View notifView = getLayoutInflater().inflate(R.layout.notification, null);
                            TextView textNotifTitle = (TextView) notifView.findViewById(
                                    R.id.text_main_notificationtitle);
                            TextView textNotifBody = (TextView) notifView.findViewById(
                                    R.id.text_main_notificationbody);
                            textNotifBody.setText(result[i].text);
                            ImageButton buttonNotifDismiss = (ImageButton) notifView.findViewById(
                                    R.id.button_main_closenotification);

                            final int ii = i;

                            buttonNotifDismiss.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    notifView.animate()
                                            .translationX(notifView.getWidth())
                                            .alpha(0)
                                            .setDuration(getResources().getInteger(
                                                    android.R.integer.config_shortAnimTime))
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    sharedPrefs.edit().putInt("readNotifs",
                                                            sharedPrefs.getInt("readNotifs", 1) * result[ii].id).commit();
                                                    ll.removeView(notifView);
                                                    result[ii].show = false;
                                                    NotificationTask.this.onPostExecute(result);
                                                }
                                            });
                                }
                            });

                            notifView.setOnTouchListener(new SwipeDismissTouchListener(
                                    notifView,
                                    null,
                                    new SwipeDismissTouchListener.OnDismissCallback() {

                                        @Override
                                        public void onDismiss(View view, Object token) {
                                            sharedPrefs.edit().putInt("readNotifs",
                                                    sharedPrefs.getInt("readNotifs", 1) * result[ii].id).commit();
                                            ll.removeView(notifView);
                                            result[ii].show = false;
                                            NotificationTask.this.onPostExecute(result);
                                        }
                                    }));

                            LayoutTransition transitioner = new LayoutTransition();
                            ll.setLayoutTransition(transitioner);

                            ll.addView(notifView, 0);
                            break;
                        }
                    }
                }
            }
        }
        new NotificationTask().execute();
    }

    public void logOut() {
        sharedPrefs.edit().remove("token").commit();
        finish();
        startActivity(getIntent());
    }

    private void setupLayout() {
        initCastBar();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        PutioMainPagerAdapter adapter = new PutioMainPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setOffscreenPageLimit(3);
        pager.setAdapter(adapter);

        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == TAB_ACCOUNT) {
                    float factorMoved = 1f - positionOffset;
                    int width = pager.getChildAt(TAB_FILES).getWidth();
                    float move = width * factorMoved;
                    buttonAddTransfer.setTranslationX(move);
                } else {
                    buttonAddTransfer.setTranslationX(0);
                }
            }

            @Override
            public void onPageSelected(int i) {
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    public void showFilesAndHighlightFile(int parentId, int id) {
        selectTab(TAB_FILES, true);
        getFilesFragment().highlightFile(parentId, id);
    }

    private FilesAndFileDetails getFilesAndFileDetailsFragment() {
        if (UIUtils.isTablet(this)) {
            PutioMainPagerAdapter adapter = (PutioMainPagerAdapter) pager.getAdapter();
            return (FilesAndFileDetails) adapter.getFragmentAtPosition(pager, TAB_FILES);
        } else {
            throw new IllegalStateException("getFilesAndFileDetailsFragment() called, but view is not tablet");
        }
    }

    private Files getFilesFragment() {
        PutioMainPagerAdapter adapter = (PutioMainPagerAdapter) pager.getAdapter();
        if (UIUtils.isTablet(Putio.this)) {
            return getFilesAndFileDetailsFragment().getFilesFragment();
        } else {
            return (Files) adapter.getFragmentAtPosition(pager, TAB_FILES);
        }
    }

    private Transfers getTransfersFragment() {
        PutioMainPagerAdapter adapter = (PutioMainPagerAdapter) pager.getAdapter();

        return (Transfers) adapter.getFragmentAtPosition(pager, TAB_TRANSFERS);
    }

    @Override
    public void filesRequestAttention() {
        selectTab(TAB_FILES, true);
    }

    @Override
    public void onTransferSelected(PutioTransfer transfer) {
        showFilesAndHighlightFile(transfer.saveParentId, transfer.fileId);
    }

    @Override
    public boolean shouldUpdateCastContext() {
        return true;
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
        if (pager.getCurrentItem() == TAB_FILES) {
            if (getFilesFragment().goBack()) {
                if (UIUtils.isTablet(Putio.this)) {
                    getFilesAndFileDetailsFragment().closeDetails();
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    private void selectTab(int position, boolean animate) {
        if (position != -1 && pager.getCurrentItem() != position) {
            pager.setCurrentItem(position, animate);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(checkCacheSizeReceiver);
        unregisterReceiver(noNetworkReceiver);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            init();
        } else {
            finish();
        }
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