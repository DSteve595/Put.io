package com.stevenschoen.putio.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.R;

public class AddTransferFile extends SherlockFragment {
	public static AddTransferFile newInstance() {
		AddTransferFile fragment = new AddTransferFile();
		
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.addtransfer_file, container, false);
		
		return view;
	}
}