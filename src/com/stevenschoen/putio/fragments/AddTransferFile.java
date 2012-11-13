package com.stevenschoen.putio.fragments;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.R;

public class AddTransferFile extends SherlockFragment {
	public static AddTransferFile newInstance() {
		AddTransferFile fragment = new AddTransferFile();
		
		return fragment;
	}
	
	private File chosenFile = null;
	
	private TextView textFilename;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.addtransfer_file, container, false);
		
		Button buttonBrowse = (Button) view.findViewById(R.id.button_addtransferfile_browse);
		buttonBrowse.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getSherlockActivity(), FileChooserActivity.class);
				intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile("/"));
				startActivityForResult(intent, 0);
			}
		});
		
		textFilename = (TextView) view.findViewById(R.id.text_addtransferfile_filename);
		
		return view;
	}
	
	public File getChosenFile() {
		return chosenFile;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
	    case 0:
	        if (resultCode == Activity.RESULT_OK) {
	            /*
	             * you can use two flags included in data
	             */
	            IFileProvider.FilterMode filterMode = (IFileProvider.FilterMode)
	                data.getSerializableExtra(FileChooserActivity._FilterMode);
	            boolean saveDialog = data.getBooleanExtra(FileChooserActivity._SaveDialog, false);

	            /*
	             * a list of files will always return,
	             * if selection mode is single, the list contains one file
	             */
	            List<LocalFile> files = (List<LocalFile>)
	                data.getSerializableExtra(FileChooserActivity._Results);
	            for (File f : files) {
	                chosenFile = f;
	                textFilename.setText(f.getName());
	            }
	        }
	        break;
	    }
	}
}