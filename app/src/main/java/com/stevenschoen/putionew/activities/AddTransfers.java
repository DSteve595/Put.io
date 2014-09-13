package com.stevenschoen.putionew.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
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
import com.stevenschoen.putionew.fragments.Files;

import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;

public class AddTransfers extends Activity implements Files.Callbacks, DestinationFilesDialog.Callbacks {
	private SectionsPagerAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;
	private PagerTitleStrip mPagerTitleStrip;
	
	private int fragmentType;
	
	private AddTransferUrl urlFragment;
	private AddTransferFile fileFragment;

	private SharedPreferences sharedPrefs;

    private int mDestinationFolderId = 0;
    private Button buttonDestination;
    private DestinationFilesDialog mFilesDialogFragment;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.Putio_Dialog);
		setContentView(R.layout.dialog_addtransfer);
		
		if (getIntent().getAction() != null) {
			switch (getIntent().getScheme()) {
				case "http":
				case "https":
				case "magnet":
					fragmentType = PutioUtils.ADDTRANSFER_URL;
					break;
				case "file":
					fragmentType = PutioUtils.ADDTRANSFER_FILE;
					break;
			}
		}
		
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getFragmentManager());

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
		
		TextView textTitle = (TextView) findViewById(R.id.dialog_title);
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

        buttonDestination = (Button) findViewById(R.id.button_addtransfer_destination);
        buttonDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFilesDialogFragment = (DestinationFilesDialog) DestinationFilesDialog.instantiate(AddTransfers.this, DestinationFilesDialog.class.getName());
                mFilesDialogFragment.show(getFragmentManager(), "dialog");
            }
        });
	}
	
	private void addUrl() {
		if (!urlFragment.getEnteredUrls().isEmpty()) {
			Intent addTransferIntent = new Intent(AddTransfers.this, TransfersActivity.class);
			addTransferIntent.putExtra("mode", PutioUtils.ADDTRANSFER_URL);
			addTransferIntent.putExtra("url", urlFragment.getEnteredUrls());
            addTransferIntent.putExtra("extract", urlFragment.getExtract());
            addTransferIntent.putExtra("saveParentId", mDestinationFolderId);
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
                    addTransferIntent.putExtra("parentId", mDestinationFolderId);
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

    @Override
    public void onFileSelected(int id) {

    }

    @Override
    public void onSomethingSelected() {

    }

    @Override
    public void onDestinationFolderSelected() {
        mDestinationFolderId = mFilesDialogFragment.getCurrentFolderId();
        // TODO buttonDestination.setText(mFilesDialogFragment.getCurrentFolderName());
        buttonDestination.setText(Integer.toString(mDestinationFolderId));
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
						Bundle bundle = new Bundle();
						bundle.putString("url", getIntent().getDataString());
						urlFragment = (AddTransferUrl) AddTransferUrl.instantiate(
								AddTransfers.this, AddTransferUrl.class.getName(), bundle);
					}
					return urlFragment;
				} else if (fragmentType == PutioUtils.ADDTRANSFER_FILE) {
					if (fileFragment == null) {
						Bundle bundle = new Bundle();
						bundle.putString("filepath", getIntent().getDataString());
						fileFragment = (AddTransferFile) AddTransferFile.instantiate(
								AddTransfers.this, AddTransferFile.class.getName(), bundle);
					}
					return fileFragment;
				} else {
					if (urlFragment == null) {
						urlFragment = (AddTransferUrl) AddTransferUrl.instantiate(
								AddTransfers.this, AddTransferUrl.class.getName());
					}
					return urlFragment;
				}
			case 1:
				if (fileFragment == null) {
					fileFragment = (AddTransferFile) AddTransferFile.instantiate(
							AddTransfers.this, AddTransferFile.class.getName());
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
				return getString(R.string.addtransfer_type_url);
			case 1:
				return getString(R.string.addtransfer_type_file);
			}
			return null;
		}
	}
}