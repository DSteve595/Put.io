package com.stevenschoen.putionew.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.stevenschoen.putionew.R;

public class AddTransferUrl extends Fragment {

	private EditText textUrls;
    private CheckBox checkBoxExtract;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.addtransfer_url, container, false);

		textUrls = (EditText) view.findViewById(R.id.edittext_addtransfer_urls);
		try {
			textUrls.setText(getArguments().getString("url"));
		} catch (NullPointerException e) { }

        checkBoxExtract = (CheckBox) view.findViewById(R.id.checkbox_addtransfer_extract);

        return view;
	}

	public String getEnteredUrls() {
		return textUrls.getText().toString();
	}

    public boolean getExtract() {
        return checkBoxExtract.isChecked();
    }
}