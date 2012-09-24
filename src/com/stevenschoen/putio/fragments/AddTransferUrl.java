package com.stevenschoen.putio.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.R;

public class AddTransferUrl extends SherlockFragment {
	private EditText textUrls;

	public static AddTransferUrl newInstance() {
		AddTransferUrl fragment = new AddTransferUrl();
		
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.addtransfer_url, container, false);
		
		textUrls = (EditText) view.findViewById(R.id.edittext_addtransfer_urls);
		try {
			textUrls.setText(getArguments().getString("url"));
		} catch (NullPointerException e) {
			
		}
		return view;
	}
	
	public String getEnteredUrls() {
		return textUrls.getText().toString();
	}
}