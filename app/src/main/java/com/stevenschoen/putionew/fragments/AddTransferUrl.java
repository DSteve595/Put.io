package com.stevenschoen.putionew.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.activities.DestinationFilesDialog;

public class AddTransferUrl extends Fragment {

	private EditText textUrls;
    private CheckBox checkBoxExtract;
    private Button buttonDestination;
    private DestinationFilesDialog mFilesDialogFragment;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.addtransfer_url, container, false);
		
		textUrls = (EditText) view.findViewById(R.id.edittext_addtransfer_urls);
		try {
			textUrls.setText(getArguments().getString("url"));
		} catch (NullPointerException e) { }

        checkBoxExtract = (CheckBox) view.findViewById(R.id.checkbox_addtransfer_extract);

        buttonDestination = (Button) view.findViewById(R.id.button_addtransfer_destination);
        buttonDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFilesDialogFragment = (DestinationFilesDialog) DestinationFilesDialog.instantiate(getActivity(), DestinationFilesDialog.class.getName());
                mFilesDialogFragment.show(getFragmentManager(), "dialog");
            }
        });

        return view;
	}
	
	public String getEnteredUrls() {
		return textUrls.getText().toString();
	}

    public boolean getExtract() {
        return checkBoxExtract.isChecked();
    }

    public int onDestinationFolderSelected() {
        int selectedFolderId = mFilesDialogFragment.getCurrentFolderId();
        // TODO buttonDestination.setText(mFilesDialogFragment.getCurrentFolderName());
        buttonDestination.setText(Integer.toString(selectedFolderId));
        return selectedFolderId;
    }

    public void onSomethingSelected() {

    }
}