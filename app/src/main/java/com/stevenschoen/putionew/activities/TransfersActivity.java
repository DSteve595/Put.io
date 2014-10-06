package com.stevenschoen.putionew.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.model.PutioRestInterface;

public class TransfersActivity extends FragmentActivity {

	SharedPreferences sharedPrefs;

	PutioUtils utils;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.utils = ((PutioApplication) getApplication()).getPutioUtils();
		
		if (getIntent().getExtras() != null && getIntent().getIntExtra("mode", 0) != 0) {
			int mode = getIntent().getIntExtra("mode", 0);
			switch (mode) {
			case AddTransfers.TYPE_URL:
				utils.getJobManager().addJobInBackground(new PutioRestInterface.PostAddTransferJob(
						utils,
                        getIntent().getStringExtra("url"),
                        getIntent().getBooleanExtra("extract", false),
                        getIntent().getIntExtra("saveParentId", 0),
                        this, getIntent()));
                break;
			case AddTransfers.TYPE_FILE:
				utils.getJobManager().addJobInBackground(new PutioRestInterface.PostUploadFileJob(
						utils, this, getIntent(),
                        (Uri) getIntent().getParcelableExtra("torrenturi"),
                        getIntent().getIntExtra("parentId", 0)));
                break;
			}
			
			finish();
		} else {
			setContentView(R.layout.transfersactivity);

            ImageButton buttonMaximize = (ImageButton) findViewById(R.id.transfersactivity_button_maximize);
			buttonMaximize.setOnClickListener(new OnClickListener() {
				@SuppressLint("NewApi")
				@Override
				public void onClick(View v) {
					Intent putioIntent = new Intent(TransfersActivity.this, Putio.class);
                    putioIntent.putExtra("goToTab", Putio.TAB_TRANSFERS);
					if (UIUtils.hasJellyBean()) {
                        View content = findViewById(android.R.id.content);
						Bundle options = ActivityOptionsCompat.makeScaleUpAnimation(
                                content,
                                0,
                                0,
                                content.getWidth(),
                                content.getHeight())
                                .toBundle();
						startActivity(putioIntent, options);
					} else {
						startActivity(putioIntent);
					}
					finish();
				}
			});
			
			ImageButton buttonClose = (ImageButton) findViewById(R.id.transfersactivity_button_close);
			buttonClose.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
		}
	}
}