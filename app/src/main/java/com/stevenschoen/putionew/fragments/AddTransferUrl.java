package com.stevenschoen.putionew.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.stevenschoen.putionew.R;

public class AddTransferUrl extends Fragment {

	private EditText textUrls;

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