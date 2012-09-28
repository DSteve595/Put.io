package com.stevenschoen.putio.activities;

import java.io.FileNotFoundException;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.stevenschoen.putio.PutioUtils;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.UIUtils;

public class FileFinished extends SherlockActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(R.style.Putio_Dialog);
		setContentView(R.layout.filefinished);
		
		Typeface robotoLight = Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf");
		
		TextView textTitle = (TextView) findViewById(R.id.dialog_title);
		textTitle.setText(getString(R.string.downloadfinishedtitle));
		textTitle.setTypeface(robotoLight);
		
		TextView textBody = (TextView) findViewById(R.id.text_downloadfinished_body);
		textBody.setText(String.format(getString(R.string.downloadfinishedbody), getIntent().getExtras().getString("filename")));
		
		Button buttonOpen = (Button) findViewById(R.id.button_filefinished_open);
		buttonOpen.setOnClickListener(new OnClickListener() {

			@TargetApi(11)
			@Override
			public void onClick(View v) {
				DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
				long downloadId = getIntent().getExtras().getLong("downloadId");
				if (UIUtils.hasHoneycomb()) {
					Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
					Log.d("asdf", uri.getPath());
					PutioUtils.openDownloadedUri(uri, FileFinished.this);
					
				} else {
					try {
						downloadManager.openDownloadedFile(downloadId);
					} catch (FileNotFoundException e) {
						Toast.makeText(FileFinished.this, getString(R.string.cantopenbecausefuckyou), Toast.LENGTH_LONG).show();
					}
				}
				
				finish();
			}
		});

		Button buttonOk = (Button) findViewById(R.id.button_filefinished_ok);
		buttonOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}