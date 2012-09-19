package com.stevenschoen.putio.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.R;

public final class Transfers extends SherlockFragment {
	public static Transfers newInstance() {
		Transfers fragment = new Transfers();
		
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.transfers, container, false);
		
		return view;
	}
}