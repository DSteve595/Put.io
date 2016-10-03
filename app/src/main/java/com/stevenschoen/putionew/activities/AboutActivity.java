package com.stevenschoen.putionew.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.stevenschoen.putionew.PutioActivity;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;

public class AboutActivity extends BaseCastActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aboutactivity);
		if (UIUtils.isTablet(this)) {
			getWindow().setLayout((int) PutioUtils.pxFromDp(this, 380), (int) PutioUtils.pxFromDp(this, 500));
		} else {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            initCastBar();
		}
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(this, PutioActivity.class);
            NavUtils.navigateUpTo(this, homeIntent);
			return true;
		}
		return (super.onOptionsItemSelected(menuItem));
	}
}