package com.stevenschoen.putionew.tv;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.stevenschoen.putionew.LoginActivity;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;

/**
 * Created by simonreggiani on 15-01-10.
 */
public class TvActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PutioApplication application = (PutioApplication) getApplication();
        if (application.isLoggedIn()) {
            init();
        } else {
            Intent setupIntent = new Intent(this, LoginActivity.class);
            startActivityForResult(setupIntent, 0);
        }

    }

    private void init() {
        setContentView(R.layout.tv_activity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                ((PutioApplication) getApplication()).buildUtils();
            } catch (PutioUtils.NoTokenException e) {
                e.printStackTrace();
            }

            init();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.tv_grid_fragment);
        if (fragment != null && fragment instanceof TvGridFragment) {
            TvGridFragment tvGridFragment = (TvGridFragment) fragment;
            if (tvGridFragment.isRootFolder()) {
                super.onBackPressed();
            } else {
                tvGridFragment.goBack();
            }
        }
    }
}
