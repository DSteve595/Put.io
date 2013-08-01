package com.stevenschoen.putionew.fragments;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.ipaulpro.afilechooser.utils.MimeTypes;
import com.nineoldandroids.view.ViewHelper;
import com.stevenschoen.putionew.R;

public class AddTransferFile extends Fragment {
	public static AddTransferFile newInstance() {
		AddTransferFile fragment = new AddTransferFile();
		
		return fragment;
	}
	
	private File chosenFile = null;
	
	private TextView textFile;
	private TextView textNotATorrent;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.addtransfer_file, container, false);
		
		Typeface robotoLight = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Light.ttf");
		
		textFile = (TextView) view.findViewById(R.id.text_addtransferfile_file);
		textFile.setTypeface(robotoLight);
		textFile.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), FileChooserActivity.class);
				startActivityForResult(intent, 0);
			}
		});
		
		textNotATorrent = (TextView) view.findViewById(R.id.text_addtransferfile_notatorrent);
		ViewHelper.setAlpha(textNotATorrent, 0);
		
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
	        	if (data != null) {
	        		final Uri uri = data.getData();
					try {
						// Create a file instance from the URI
						final File file = FileUtils.getFile(uri);
						textFile.setText(file.getName());
						chosenFile = file;
						String mimetype = new MimeTypes().getMimeType(uri);
						if (!mimetype.matches("application/x-bittorrent") && ViewHelper.getAlpha(textNotATorrent) == 0) {
							animate(textNotATorrent).alpha(1);
						} else if (mimetype.matches("application/x-bittorrent") && ViewHelper.getAlpha(textNotATorrent) != 0) {
							animate(textNotATorrent).alpha(0);
						}
					} catch (Exception e) {
						Log.e("FileSelectorTestActivity", "File select error", e);
					}
	        	}
	        }
	        break;
	    }
	}
}