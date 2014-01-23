package com.stevenschoen.putionew.fragments;

import android.app.Activity;
import android.content.ContentResolver;
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
import com.nineoldandroids.view.ViewHelper;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

public class AddTransferFile extends Fragment {
	public static AddTransferFile newInstance() {
		AddTransferFile fragment = new AddTransferFile();
		
		return fragment;
	}
	
	private Uri chosenTorrentUri = null;
	
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
                if (UIUtils.hasKitKat()) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("application/x-bittorrent");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 0);
                } else {
                    Intent intent = new Intent(getActivity(), FileChooserActivity.class);
                    startActivityForResult(intent, 0);
                }
			}
		});
		
		textNotATorrent = (TextView) view.findViewById(R.id.text_addtransferfile_notatorrent);
		ViewHelper.setAlpha(textNotATorrent, 0);
		
		if (getArguments() != null && getArguments().getString("filepath") != null) {
			Intent fileIntent = new Intent();
			fileIntent.setData(Uri.parse(getArguments().getString("filepath")));
			onActivityResult(0, Activity.RESULT_OK, fileIntent);
		}
		
		return view;
	}
	
	public Uri getChosenTorrentUri() {
		return chosenTorrentUri;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
	    case 0:
	        if (resultCode == Activity.RESULT_OK) {
	        	if (data != null) {
	        		final Uri uri = data.getData();
					try {
                        chosenTorrentUri = uri;
						textFile.setText(PutioUtils.getNameFromUri(getActivity(), uri));
                        ContentResolver cr = getActivity().getContentResolver();
                        String mimetype = cr.getType(uri);
                        if (!mimetype.matches("application/x-bittorrent") && ViewHelper.getAlpha(textNotATorrent) == 0) {
							animate(textNotATorrent).alpha(1);
						} else if (mimetype.matches("application/x-bittorrent") && ViewHelper.getAlpha(textNotATorrent) != 0) {
							animate(textNotATorrent).alpha(0);
						}
					} catch (Exception e) {
						Log.d("asdf", "File select error", e);
					}
	        	}
	        }
	        break;
	    }
	}
}