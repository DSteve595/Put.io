package com.stevenschoen.putionew.transfers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import com.stevenschoen.putionew.PutioActivity;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;

public class TransfersActivity extends AppCompatActivity {

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
          Intent putioIntent = new Intent(TransfersActivity.this, PutioActivity.class);
          putioIntent.putExtra(PutioActivity.EXTRA_GO_TO_TAB, PutioActivity.TAB_TRANSFERS);
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
}
