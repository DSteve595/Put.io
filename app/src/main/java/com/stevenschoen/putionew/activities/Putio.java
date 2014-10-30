package com.stevenschoen.putionew.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioNotification;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.SlidingTabLayout;
import com.stevenschoen.putionew.SwipeDismissTouchListener;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.fragments.Account;
import com.stevenschoen.putionew.fragments.FileDetails;
import com.stevenschoen.putionew.fragments.Files;
import com.stevenschoen.putionew.fragments.Transfers;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFileData;
import com.stevenschoen.putionew.model.transfers.PutioTransferData;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

public class Putio extends BaseCastActivity implements
        Files.Callbacks, FileDetails.Callbacks, Transfers.Callbacks, DestinationFilesDialog.Callbacks {

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

    private Account accountFragment;
    private Files filesFragment;
    private FileDetails fileDetailsFragment;
    private Transfers transfersFragment;

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
                selectTab(goToTab);
            }

            if (intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SEARCH) && filesFragment != null) {
                    String query = intent.getStringExtra(SearchManager.QUERY);
                    filesFragment.initSearch(query);
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

        MenuItem buttonSettings = menu.findItem(R.id.menu_settings);
        buttonSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                Intent settingsIntent = new Intent(Putio.this, Preferences.class);
                Putio.this.startActivity(settingsIntent);
                return false;
            }
        });

        MenuItem buttonLogout = menu.findItem(R.id.menu_logout);
        buttonLogout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                logOut();
                return false;
            }
        });

        MenuItem buttonAbout = menu.findItem(R.id.menu_about);
        buttonAbout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                Intent aboutIntent = new Intent(Putio.this, AboutActivity.class);
                startActivity(aboutIntent);
                return false;
            }
        });

        return true;
    }

    @Override
    public void onDestinationFolderSelected(PutioFileData folder) {
        filesFragment.onDestinationFolderSelected(folder);
    }

    public class SectionsPagerAdapter extends PagerAdapter {
        FragmentManager fm;

        int currentPosition;

        public SectionsPagerAdapter(FragmentManager fm) {
            super();
            this.fm = fm;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = null;
            boolean newFragment = true;
            switch (position) {
                case TAB_ACCOUNT:
                    fragment = fm.findFragmentByTag(makeFragmentTag(container.getId(), position));
                    if (fragment == null) {
                        fragment = Account.instantiate(Putio.this, Account.class.getName());
                    } else {
                        newFragment = false;
                    }
                    accountFragment = (Account) fragment;
                    break;
                case TAB_FILES:
                    fragment = fm.findFragmentByTag(makeFragmentTag(container.getId(), position));
                    if (fragment == null) {
                        fragment = Files.instantiate(Putio.this, Files.class.getName());
                    } else {
                        newFragment = false;
                    }
                    filesFragment = (Files) fragment;

                    if (UIUtils.isTablet(Putio.this)) {
                        View filesView = getLayoutInflater().inflate(R.layout.tablet_files, container);
                        FragmentTransaction ft = fm.beginTransaction();
                        if (newFragment) {
                            ft.add(R.id.fragment_files, fragment, makeFragmentTag(container.getId(), position));
                        } else {
                            ft.attach(fragment);
                        }
                        ft.commit();
                        return filesView;
                    }
                    break;
                case TAB_TRANSFERS:
                    fragment = fm.findFragmentByTag(makeFragmentTag(container.getId(), position));
                    if (fragment == null) {
                        fragment = Transfers.instantiate(Putio.this, Transfers.class.getName());
                    } else {
                        newFragment = false;
                    }
                    transfersFragment = (Transfers) fragment;
                    break;
            }

            if (isFragment(position)) {
                FragmentTransaction ft = fm.beginTransaction();
                if (newFragment) {
                    ft.add(container.getId(), fragment, makeFragmentTag(container.getId(), position));
                } else {
                    ft.attach(fragment);
                }
                ft.commit();

                if (position != currentPosition) {
                    fragment.setMenuVisibility(false);
                    fragment.setUserVisibleHint(false);
                }
                return fragment;
            }

            return null;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (isFragment(position)) {
                Fragment fragment = fm.findFragmentByTag(makeFragmentTag(container.getId(), position));
                FragmentTransaction ft = fm.beginTransaction();
                ft.detach(fragment);
                ft.commit();
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            currentPosition = position;

            if (isFragment(position)) {
                Fragment fragment = fm.findFragmentByTag(makeFragmentTag(container.getId(), position));
                if (fragment != null) {
                    fragment.setMenuVisibility(false);
                    fragment.setUserVisibleHint(false);
                }
            }
        }

        private boolean isFragment(int position) {
            switch (position) {
                case TAB_ACCOUNT:
                    return true;
                case TAB_FILES:
                    return !UIUtils.isTablet(Putio.this);
                case TAB_TRANSFERS:
                    return true;
            }

            return false;
        }

        private String makeFragmentTag(int viewId, int position) {
            return "putio_fragment_" + viewId + "_" + position;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            if (o instanceof Fragment) {
                return (view == ((Fragment) o).getView());
            } else if (o instanceof View) {
                return (view.getId() == R.id.layout_main_root);
            }

            return false;
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
        selectTab(navItem);

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

        if (UIUtils.hasLollipop()) {
            PutioUtils.setupFab(buttonAddTransfer);
        }

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
                                    findViewById(R.id.layout_main_root);
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

                            if (UIUtils.hasHoneycomb()) {
                                final LayoutTransition transitioner = new LayoutTransition();
                                ll.setLayoutTransition(transitioner);
                            }

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setOffscreenPageLimit(3);
        pager.setAdapter(mSectionsPagerAdapter);

        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == TAB_ACCOUNT) {
                    float factorMoved = 1f - positionOffset;
                    int width = pager.getChildAt(TAB_ACCOUNT).getWidth();
                    float move = width * factorMoved;
                    buttonAddTransfer.setTranslationX(move);
                } else {
                    buttonAddTransfer.setTranslationX(0);
                }
            }
            @Override
            public void onPageSelected(int i) { }
            @Override
            public void onPageScrollStateChanged(int i) { }
        });
    }

    public void showFilesAndHighlightFile(int parentId, int id) {
        selectTab(TAB_FILES);
        filesFragment.highlightFile(parentId, id);
    }

    @Override
    public void onFileSelected(int id) {
        if (UIUtils.isTablet(this)) {
            Bundle fileDetailsBundle = new Bundle();
            fileDetailsBundle.putParcelable("fileData", filesFragment.getFileAtPosition(id));
            fileDetailsFragment = (FileDetails) FileDetails.instantiate(
                    this, FileDetails.class.getName(), fileDetailsBundle);

            getFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_from_bottom,
                            R.animator.slide_out_right)
                    .replace(R.id.fragment_details, fileDetailsFragment).commit();
            buttonAddTransfer.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onSomethingSelected() {
        if (UIUtils.isTablet(this)) {
            if (fileDetailsFragment != null && fileDetailsFragment.isAdded()) {
                removeFD(true);
            }
        }
    }

    @Override
    public void onFDCancelled() {
        removeFD(R.animator.slide_out_right);
    }

    @Override
    public void onFDFinished() {
        removeFD(R.animator.slide_out_left);
    }

    @Override
    public void onTransferSelected(PutioTransferData transfer) {
        showFilesAndHighlightFile(transfer.saveParentId, transfer.fileId);
    }

    private boolean isFDShown() {
        return (fileDetailsFragment != null && fileDetailsFragment.isAdded());
    }

    private void removeFD(int exitAnim) {
        if (isFDShown()) {
            filesFragment.setFileChecked(fileDetailsFragment.getFileId(), false);
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_from_bottom, exitAnim)
                    .remove(fileDetailsFragment)
                    .commit();
        }
    }

    private void removeFD(boolean askIfSave) {
        if (askIfSave)
            if (isFDShown() && !fileDetailsFragment.getOldFilename().equals(fileDetailsFragment.getNewFilename())) {
                final Dialog confirmChangesDialog = utils.confirmChangesDialog(this, fileDetailsFragment.getOldFilename());
                confirmChangesDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        removeFD(R.animator.slide_out_left);
                    }
                });

                confirmChangesDialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        removeFD(R.animator.slide_out_right);
                    }
                });

                Button apply = (Button) confirmChangesDialog.findViewById(R.id.button_confirm_apply);
                apply.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        utils.getJobManager().addJobInBackground(new PutioRestInterface.PostRenameFileJob(
                                utils,
                                fileDetailsFragment.getFileId(),
                                fileDetailsFragment.getNewFilename()));
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
            }
        else {
            removeFD(R.animator.slide_out_right);
        }
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
            transfersFragment.setHasNetwork(false);
        }
    };

	@Override
    public void onBackPressed() {
        if (UIUtils.isTablet(this)) {
            if (getSupportActionBar().getSelectedNavigationIndex() == TAB_FILES) {
                if (filesFragment.goBack()) {
                    if (isFDShown()) {
                        removeFD(true);
                    }
                } else {
                    super.onBackPressed();
                }
            } else {
                super.onBackPressed();
            }
        } else {
            if (pager.getCurrentItem() == TAB_FILES && !filesFragment.goBack()) {
                super.onBackPressed();
            }
        }
    }

    private void selectTab(int position) {
        if (position != -1 && pager.getCurrentItem() != position) {
            pager.setCurrentItem(position, false);
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