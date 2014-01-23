package com.stevenschoen.putionew.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;

public class TransfersActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getIntent().getExtras() != null && getIntent().getIntExtra("mode", 0) != 0) {
			int mode = getIntent().getIntExtra("mode", 0);
			switch (mode) {
			case PutioUtils.ADDTRANSFER_URL:
                PutioUtils.addTransfersAsync(
					this,
					mode,
					getIntent(),
					getIntent().getStringExtra("url"),
					null);
                break;
			case PutioUtils.ADDTRANSFER_FILE:
                PutioUtils.addTransfersAsync(
					this,
					mode,
					getIntent(),
					null,
                    (Uri) getIntent().getParcelableExtra("torrenturi"));
                break;
			}
			
			finish();
		} else {
			setContentView(R.layout.transfersactivity);
			
			TextView textTitle = (TextView) findViewById(R.id.transfersactivity_text_title);
			textTitle.setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf"));
			
			ImageButton buttonMaximize = (ImageButton) findViewById(R.id.transfersactivity_button_maximize);
			buttonMaximize.setOnClickListener(new OnClickListener() {
				@SuppressLint("NewApi")
				@Override
				public void onClick(View v) {
					Intent putioIntent = new Intent(TransfersActivity.this, Putio.class);
					if (UIUtils.hasHoneycomb()) {
                        View content = findViewById(android.R.id.content);
						Bundle options = ActivityOptionsCompat.makeScaleUpAnimation(
                                content,
                                content.getLeft(),
                                content.getTop(),
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