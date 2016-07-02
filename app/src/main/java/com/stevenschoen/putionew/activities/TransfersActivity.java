package com.stevenschoen.putionew.activities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;

import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.PutioUploadInterface;
import com.stevenschoen.putionew.model.responses.PutioTransferFileUploadResponse;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.functions.Action1;

public class TransfersActivity extends BottomSheetActivity {

	SharedPreferences sharedPrefs;

	PutioUtils utils;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		utils = ((PutioApplication) getApplication()).getPutioUtils();
		
		if (getIntent().getExtras() != null && getIntent().getIntExtra("mode", 0) != 0) {
			int mode = getIntent().getIntExtra("mode", 0);
			switch (mode) {
			case AddTransfers.TYPE_URL:
				addTransferUrl(
						getIntent().getStringExtra("url"),
						getIntent().getBooleanExtra("extract", false),
						getIntent().getLongExtra("saveParentId", 0));
                break;
			case AddTransfers.TYPE_FILE:
				addTransferFile(
						(Uri) getIntent().getParcelableExtra("torrenturi"),
						getIntent().getLongExtra("parentId", 0)
				);
                break;
			}
			
			finish();
		} else {
			setContentView(R.layout.transfersactivity);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_transfersdialog);
            toolbar.inflateMenu(R.menu.menu_transfersdialog);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.menu_close:
                            finish();
                            return true;
                    }
                    return false;
                }
            });
            toolbar.setNavigationOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent putioIntent = new Intent(TransfersActivity.this, Putio.class);
                    putioIntent.putExtra("goToTab", Putio.TAB_TRANSFERS);
					View content = findViewById(android.R.id.content);
					Bundle options = ActivityOptionsCompat.makeScaleUpAnimation(
							content,
							0,
							0,
							content.getWidth(),
							content.getHeight())
							.toBundle();
					startActivity(putioIntent, options);
                    finish();
                }
            });
		}
	}

	private void addTransferUrl(String url, boolean extract, long saveParentId) {
		final UploadNotif notif = new UploadNotif();
		notif.start();
		utils.getRestInterface().addTransferUrl(url, extract, saveParentId)
				.subscribe(new Action1<PutioTransfer>() {
					@Override
					public void call(PutioTransfer transfer) {
						notif.succeeded();
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						notif.failed();
						throwable.printStackTrace();
						Toast.makeText(TransfersActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
					}
				});
	}

	private void addTransferFile(Uri torrentUri, long parentId) {
		final UploadNotif notif = new UploadNotif();
		notif.start();

		PutioUploadInterface uploadInterface = utils.makePutioRestInterface(PutioUtils.uploadBaseUrl).create(PutioUploadInterface.class);

		File file;
		if (torrentUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
			file = new File(getCacheDir(), "upload.torrent");
			ContentResolver cr = getContentResolver();
			try {
				FileUtils.copyInputStreamToFile(cr.openInputStream(torrentUri), file);
			} catch (IOException e) {
				e.printStackTrace();
				notif.failed();
				return;
			}
		} else {
			file = new File(torrentUri.getPath());
		}
		RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-bittorrent"), file);
		uploadInterface.uploadFile(requestBody, parentId)
				.subscribe(new Action1<PutioTransferFileUploadResponse>() {
					@Override
					public void call(PutioTransferFileUploadResponse putioTransferFileUploadResponse) {
						notif.succeeded();
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						notif.failed();
						throwable.printStackTrace();
						Toast.makeText(TransfersActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
					}
				});
	}

	public class UploadNotif {
		private NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		protected void start() {
			NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(TransfersActivity.this);
			notifBuilder.setOngoing(true)
					.setCategory(NotificationCompat.CATEGORY_PROGRESS)
					.setPriority(NotificationCompat.PRIORITY_LOW)
					.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
					.setContentTitle(getString(R.string.notification_title_uploading_torrent))
					.setSmallIcon(R.drawable.ic_notificon_transfer)
					.setTicker(getString(R.string.notification_ticker_uploading_torrent))
					.setProgress(1, 0, true);
			Notification notif = notifBuilder.build();
			notif.ledARGB = Color.parseColor("#FFFFFF00");
			try {
				notifManager.notify(1, notif);
			} catch (IllegalArgumentException e) {
				notifBuilder.setContentIntent(PendingIntent.getActivity(
						TransfersActivity.this, 0, new Intent(TransfersActivity.this, Putio.class), 0));
				notif = notifBuilder.build();
				notif.ledARGB = Color.parseColor("#FFFFFF00");
				notifManager.notify(1, notif);
			}
		}

		protected void succeeded() {
			NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(TransfersActivity.this);
			notifBuilder.setOngoing(false)
					.setAutoCancel(true)
					.setCategory(NotificationCompat.CATEGORY_PROGRESS)
					.setPriority(NotificationCompat.PRIORITY_LOW)
					.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
					.setContentTitle(getString(R.string.notification_title_uploaded_torrent))
					.setContentText(getString(R.string.notification_body_uploaded_torrent))
					.setSmallIcon(R.drawable.ic_notificon_transfer);
			Intent viewTransfersIntent = new Intent(TransfersActivity.this, TransfersActivity.class);
			viewTransfersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			notifBuilder.setContentIntent(PendingIntent.getActivity(
					TransfersActivity.this, 0, viewTransfersIntent, PendingIntent.FLAG_CANCEL_CURRENT));
//				notifBuilder.addAction(R.drawable.ic_notif_watch, "Watch", null); TODO
			notifBuilder.setTicker(getString(R.string.notification_ticker_uploaded_torrent))
					.setProgress(0, 0, false);
			Notification notif = notifBuilder.build();
			notif.ledARGB = Color.parseColor("#FFFFFF00");
			notifManager.notify(1, notif);
		}

		protected void failed() {
			NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(TransfersActivity.this);
			notifBuilder.setOngoing(false)
					.setAutoCancel(true)
					.setCategory(NotificationCompat.CATEGORY_ERROR)
					.setPriority(NotificationCompat.PRIORITY_DEFAULT)
					.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
					.setContentTitle(getString(R.string.notification_title_error))
					.setContentText(getString(R.string.notification_body_error))
					.setSmallIcon(R.drawable.ic_notificon_transfer);
			PendingIntent retryNotifIntent = PendingIntent.getActivity(
					TransfersActivity.this, 0, getIntent(), PendingIntent.FLAG_ONE_SHOT);
			notifBuilder.addAction(R.drawable.ic_notif_retry, getString(R.string.notification_button_retry), retryNotifIntent)
					.setContentIntent(retryNotifIntent)
					.setTicker(getString(R.string.notification_ticker_error));
			Notification notif = notifBuilder.build();
			notif.ledARGB = Color.parseColor("#FFFFFF00");
			notifManager.notify(1, notif);
		}
	}
}