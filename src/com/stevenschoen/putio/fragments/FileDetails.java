package com.stevenschoen.putio.fragments;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.PutioFileData;
import com.stevenschoen.putio.PutioFileUtils;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.UIUtils;

public class FileDetails extends SherlockFragment {
	PutioFileData origFileData;
	PutioFileData newFileData;
	
	TextView textPercent;
	
	public FileDetails(PutioFileData fileData) {
		this.origFileData = fileData;
		this.newFileData = fileData;
	}
	
	public FileDetails() {
    }
	
    public interface Callbacks {

        public void onFDCancelled();
        public void onFDFinished();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onFDCancelled() {
        }
        @Override
        public void onFDFinished() {
        }
    };
	
    private Callbacks mCallbacks = sDummyCallbacks;

	SharedPreferences sharedPrefs;
	
	public final String baseUrl = "https://api.put.io/v2/";
	
	private String token;
	private String tokenWithStuff;

	private TextView textFileCreatedDate;
	private TextView textFileCreatedTime;
	
	PutioFileUtils utils;
	private EditText textFileName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		token = sharedPrefs.getString("token", null);
		tokenWithStuff = "?oauth_token=" + token;
		
		utils = new PutioFileUtils(token);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (origFileData == null) {
			origFileData = savedInstanceState.getParcelable("origFileData");
			newFileData = savedInstanceState.getParcelable("newFileData");
		}
		
		final View view = inflater.inflate(R.layout.filedetails, container, false);
		
		textFileName = (EditText) view.findViewById(R.id.editText_fileName);
		textFileName.setText(origFileData.name);
		
		Button btnUndoName = (Button) view.findViewById(R.id.button_undoName);
		btnUndoName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				textFileName.setText(origFileData.name);
			}
			
		});
		
		view.post(new Runnable() {   
		    @Override
		    public void run() {
				RelativeLayout nameHolder = (RelativeLayout) view.findViewById(R.id.layout_nameHolder);
				float nameHolderWidth = nameHolder.getMeasuredWidth();
				float dip40 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
				textFileName.setWidth(Math.round(nameHolderWidth - dip40));
	        }
	    });
		
		String[] created = PutioFileData.separateIsoTime(origFileData.createdTime);
		TextView textFileCreatedCreated = (TextView) view.findViewById(R.id.text_fileDetailsCreatedStatic);
		textFileCreatedCreated.setText(getString(R.string.created) + " ");
		
		textFileCreatedDate = (TextView) view.findViewById(R.id.text_fileDetailsCreatedDate);
		textFileCreatedDate.setText(created[0]);
		
		TextView textFileCreatedAt = (TextView) view.findViewById(R.id.text_fileDetailsCreatedStaticAt);
		textFileCreatedAt.setText(" " + getString(R.string.at) + " ");
		
		textFileCreatedTime = (TextView) view.findViewById(R.id.text_fileDetailsCreatedTime);
		textFileCreatedTime.setText(created[1]);
		
		Button btnSave = (Button) view.findViewById(R.id.button_filedetails_save);
		btnSave.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				newFileData.name = textFileName.getText().toString();
				saveFileToServerAndFinish();
			}

		});
		
		class updateFileTask extends AsyncTask<Void, Void, PutioFileData> {
			protected PutioFileData doInBackground(Void... nothing) {
				JSONObject obj;
				try {					
					InputStream is = utils.getFileJsonData(baseUrl + "files/" + origFileData.id + tokenWithStuff);
					String string = utils.convertStreamToString(is);
					obj = new JSONObject(string).getJSONObject("file");

					newFileData = new PutioFileData(
							origFileData.isShared,
							obj.getString("name"),
							obj.getString("created_at"),
							obj.getInt("parent_id"),
							origFileData.hasMp4,
							obj.getString("content_type"),
							obj.getInt("id"),
							obj.getLong("size"));
					return newFileData;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
			
			public void onPostExecute(final PutioFileData file) {
				String[] created = PutioFileData.separateIsoTime(file.createdTime);
				if (!created[0].matches(textFileCreatedDate.getText().toString()) || !created[1].matches(textFileCreatedTime.getText().toString())) {
					textFileCreatedDate.setText(created[0]);
					textFileCreatedTime.setText(created[1]);
				}
			}
		}
		new updateFileTask().execute();
		
		Button btnCancel = (Button) view.findViewById(R.id.button_filedetails_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (!UIUtils.isTablet(getActivity())) {
					getSherlockActivity().finish();
				} else {
					mCallbacks.onFDCancelled();
				}
			}

		});
		
		Button btnDownload = (Button) view.findViewById(R.id.button_filedetails_download);
		btnDownload.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				URL url = null;
				try {
					url = new URL(baseUrl + "files/" + origFileData.id + "/download" + tokenWithStuff);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				downloadFile(origFileData.id, getNewFilename(), baseUrl + "files/" + origFileData.id + "/download" + tokenWithStuff);
			}
			
		});
		return view;
	}
	
	@TargetApi(11)
	private void downloadFile(int id, String filename, String url) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
		request.setDescription("put.io");
		if (UIUtils.hasHoneycomb()) {
		    request.allowScanningByMediaScanner();
		    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationInExternalPublicDir(
				Environment.DIRECTORY_DOWNLOADS,
				"put.io" + File.separator
				+ id + File.separator
				+ filename);

		// get download service and enqueue file
		DownloadManager manager = (DownloadManager) getSherlockActivity().getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
	}
	
	public void updatePercent(int percent) {
		textPercent.setText(Integer.toString(percent));
	}
	
	public int getFileId() {
		return origFileData.id;
	}
	public String getOldFilename() {
		return origFileData.name;
	}
	public String getNewFilename() {
		return textFileName.getText().toString();
	}
	
	@Override
	public void onSaveInstanceState(Bundle icicle) {
		icicle.putParcelable("origFileData", origFileData);
		icicle.putParcelable("newFileData", newFileData);
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (UIUtils.isTablet(getSherlockActivity())) {
        	mCallbacks = (Callbacks) activity;
        }
    }
    
    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = sDummyCallbacks;
    }
	
	public void saveFileToServerAndFinish() {
		utils.saveFileToServer(getSherlockActivity(), newFileData.id, origFileData.name, newFileData.name);
		if (!UIUtils.isTablet(getSherlockActivity())) {
			getSherlockActivity().finish();
		} else {
			mCallbacks.onFDFinished();
		}
	}
}