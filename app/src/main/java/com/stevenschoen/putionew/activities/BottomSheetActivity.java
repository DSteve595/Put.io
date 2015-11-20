package com.stevenschoen.putionew.activities;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.stevenschoen.putionew.R;

public class BottomSheetActivity extends AppCompatActivity {
	@Override
	public void setContentView(int layoutResID) {
		View sheet = getLayoutInflater().inflate(R.layout.bottomsheet, null);
		super.setContentView(sheet);
		getLayoutInflater().inflate(layoutResID, (ViewGroup) sheet, true);
	}
}
