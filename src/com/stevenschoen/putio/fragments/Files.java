package com.stevenschoen.putio.fragments;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.stevenschoen.putio.FilesAdapter;
import com.stevenschoen.putio.PutioFileData;
import com.stevenschoen.putio.PutioFileLayout;
import com.stevenschoen.putio.PutioFileUtils;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.UIUtils;
import com.stevenschoen.putio.activities.FileDetailsActivity;
import com.stevenschoen.putio.activities.Putio;

public final class Files extends SherlockFragment {
	public static Files newInstance() {
		Files fragment = new Files();
		
		return fragment;
	}
	
    public interface Callbacks {

        public void onFileSelected(int id);
        public void onSomethingSelected();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onFileSelected(int id) {
        }
        @Override
        public void onSomethingSelected() {
        }
    };
	
    private Callbacks mCallbacks = sDummyCallbacks;
    
	SharedPreferences sharedPrefs;
	
	public final String baseUrl = "https://api.put.io/v2/";
	private FilesAdapter adapter;
	private ArrayList<PutioFileLayout> fileLayouts = new ArrayList<PutioFileLayout>();
	private PutioFileLayout dummyFile = new PutioFileLayout("Loading...", "Your files will appear shortly.", R.drawable.ic_launcher);
	
	private PutioFileData[] fileData;
	private int origId;
	
	updateFilesTask update = new updateFilesTask();
	private String token;
	private String tokenWithStuff;
	
	public int currentFolderId;
	private int parentParentId;

	private ListView listview;
	private int mActivatedPosition = ListView.INVALID_POSITION;

	private Button buttonRefresh;
	private ProgressBar itemRefreshing;
	private View buttonBar;
	private int buttonBarHeight;
	
	PutioFileUtils utils;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			currentFolderId = savedInstanceState.getInt("currentFolderId");
		} catch (NullPointerException e) {
			currentFolderId = 0;
		}

		sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		token = sharedPrefs.getString("token", null);
		tokenWithStuff = "?oauth_token=" + token;
		
		utils = new PutioFileUtils(token, sharedPrefs);
	}
	
	@TargetApi(11)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.files, container, false);
		
		listview = (ListView) view.findViewById(R.id.fileslist);
		
		adapter = new FilesAdapter(getSherlockActivity(), R.layout.file,
				fileLayouts);
		listview.setEmptyView(view.findViewById(R.id.loading));
		listview.setAdapter(adapter);
		listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(listview);
		if (UIUtils.isTablet(getSherlockActivity())) {
			listview.setVerticalFadingEdgeEnabled(true);
			listview.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
			listview.setFastScrollAlwaysVisible(true);
			setActivateOnItemClick(true);
		}
		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> a, View view, int position,
					long id) {
				mCallbacks.onSomethingSelected();

				int adjustedPosition;

				// If user is in a folder other than root
				// Lowers the position value to realign with the extra list item
				if (currentFolderId != 0) {
					adjustedPosition = position - 1;

					// If user picked the "Go up" option
					if (position == 0) {
						goBack();
						return;
					}
				} else {
					adjustedPosition = position;
				}

				if (!fileData[adjustedPosition].isFolder) {
					if (!UIUtils.isTablet(getSherlockActivity())) {
						Intent detailsIntent = new Intent(getActivity(),
								FileDetailsActivity.class);
						detailsIntent.putExtra("fileData",
								fileData[adjustedPosition]);
						startActivity(detailsIntent);
					} else {
						mCallbacks.onFileSelected(adjustedPosition);
						setActivatedPosition(position);
					}
				} else if (fileData[adjustedPosition].isFolder) {
					currentFolderId = fileData[adjustedPosition].id;
					listview.setClickable(false);
					invalidateList();
				}
			}
		});
		
		buttonRefresh = (Button) view.findViewById(R.id.button_files_refresh);
		buttonRefresh.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				invalidateList();
			}
		});
		
		itemRefreshing = (ProgressBar) view.findViewById(R.id.item_files_refresh);
		itemRefreshing.setEnabled(false);
		
		buttonBar = view.findViewById(R.id.buttonbar);
		buttonBar.post(new Runnable() {

			@Override
			public void run() {
				buttonBarHeight = buttonBar.getHeight();
			}
		});
		
		fileLayouts.add(0, dummyFile);
		invalidateList();
		
		return view;
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
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		if ((info.id == 0) && (currentFolderId != 0)) {
			return;
		} else {
			if (v.getId() == R.id.fileslist) {
				menu.setHeaderTitle(fileData[getAdjustedPosition(info.position)].name);
			    MenuInflater inflater = getSherlockActivity().getMenuInflater();
			    inflater.inflate(R.menu.context, menu);
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.context_download:
				initDownload(getAdjustedPosition((int) info.id));
				return true;
			case R.id.context_rename:
				initRename(getAdjustedPosition((int) info.id));
				return true;
			case R.id.context_delete:
				initDelete(getAdjustedPosition((int) info.id));
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
	
	private void initDownload(final long id) {
		utils.downloadFile(getSherlockActivity(),
				fileData[(int) id].id,
				fileData[(int) id].name);
	}
	
	private void initRename(final long id) {
		final Dialog renameDialog = utils.PutioDialog(getSherlockActivity(), getString(R.string.renametitle), R.layout.dialog_rename);
		renameDialog.show();
		
		final EditText textFileName = (EditText) renameDialog.findViewById(R.id.editText_fileName);
		textFileName.setText(fileData[(int) id].name);
		
		Button btnUndoName = (Button) renameDialog.findViewById(R.id.button_undoName);
		btnUndoName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				textFileName.setText(fileData[(int) id].name);
			}
		});
		
		textFileName.post(new Runnable() {   
		    @Override
		    public void run() {
				View nameHolder = renameDialog.findViewById(R.id.layout_nameHolder);
				float nameHolderWidth = nameHolder.getMeasuredWidth();
				float dip40 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
				textFileName.setWidth(Math.round(nameHolderWidth - dip40));
	        }
	    });
		
		Button saveRename = (Button) renameDialog.findViewById(R.id.button_rename_save);
		saveRename.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				utils.saveFileToServer(getSherlockActivity(),
						fileData[(int) id].id, fileData[(int) id].name, textFileName.getText().toString());
				renameDialog.dismiss();
			}
		});
		
		Button cancelRename = (Button) renameDialog.findViewById(R.id.button_rename_cancel);
		cancelRename.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				renameDialog.cancel();
			}
		});
	}
	
	private void initDelete(final long id) {
		final Dialog deleteDialog = utils.PutioDialog(getSherlockActivity(), getString(R.string.deletetitle), R.layout.dialog_delete);
		deleteDialog.show();
		
		Button deleteDelete = (Button) deleteDialog.findViewById(R.id.button_delete_delete);
		deleteDelete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				utils.deleteFile(getSherlockActivity(), fileData[(int) id].id);
				deleteDialog.dismiss();
			}
		});
		
		Button cancelDelete = (Button) deleteDialog.findViewById(R.id.button_delete_cancel);
		cancelDelete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				deleteDialog.cancel();
			}
		});
	}
	
	public void toast(String message) {
		Toast.makeText(getSherlockActivity(), message, Toast.LENGTH_SHORT).show();
	}
	public void log(String message) {
		Log.d("asdf", message);
	}
	
	public void invalidateList() {
		if (update.getStatus() == AsyncTask.Status.RUNNING) {
			update.cancel(true);
		}
		update = new updateFilesTask();
		update.execute();
		setShowRefreshButton(false);
	}
	
	public void setShowRefreshButton(boolean value) {
		if (value) {
			animate(buttonBar).setDuration(200).translationY(buttonBarHeight);
			animate(buttonBar).setListener(new AnimatorListener() {

				@Override
				public void onAnimationStart(Animator animation) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					itemRefreshing.setEnabled(false);
					itemRefreshing.setVisibility(View.INVISIBLE);
					
					buttonRefresh.setEnabled(true);
					animate(buttonRefresh).setDuration(0).alpha(1);
					
					animate(buttonBar).setDuration(200).translationY(0);
					animate(buttonBar).setListener(null);
				}

				@Override
				public void onAnimationCancel(Animator animation) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onAnimationRepeat(Animator animation) {
					// TODO Auto-generated method stub
					
				}
				
			});
		} else if (!value) {
			buttonRefresh.setEnabled(false);
			buttonBar.post(new Runnable() {
				@Override
				public void run() {
					animate(buttonBar).setDuration(200).translationY(buttonBarHeight);
				}
			});
			animate(buttonBar).setDuration(200).translationY(buttonBarHeight);
			animate(buttonBar).setListener(new AnimatorListener() {

				@Override
				public void onAnimationStart(Animator animation) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					animate(buttonBar).setDuration(200).translationY(0);
					animate(buttonBar).setListener(null);
					animate(buttonRefresh).setDuration(0).alpha(0);
					
					itemRefreshing.setEnabled(true);
					itemRefreshing.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationCancel(Animator animation) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onAnimationRepeat(Animator animation) {
					// TODO Auto-generated method stub
					
				}
			});
		}
	}
	
	class updateFilesTask extends AsyncTask<Void, Void, PutioFileData[]> {
		private int index;
		private View v;
		private int top;
		private int newId;
		private int origIdBefore = origId;

		public void onPreExecute() {
			JSONObject json;
			JSONArray array;
			
			index = listview.getFirstVisiblePosition();
			v = listview.getChildAt(0);
			top = (v == null) ? 0 : v.getTop();
			
			try {
				File input = new File(getSherlockActivity().getCacheDir(), currentFolderId + ".json");
				FileInputStream fis = FileUtils.openInputStream(input);
				
                json = new JSONObject(utils.convertStreamToString(fis));
                fis.close();
                
                array = json.getJSONArray("files");
                newId = json.getJSONObject("parent").getInt("id");
                origId = newId;
				PutioFileData[] file = new PutioFileData[array.length()];
				
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);

					file[i] = new PutioFileData(
							obj.getBoolean("is_shared"),
							obj.getString("name"),
							obj.getString("screenshot"),
							obj.getString("created_at"),
							obj.getInt("parent_id"),
							utils.stringToBooleanHack(obj.getString("is_mp4_available")),
							obj.getString("content_type"),
							obj.getInt("id"),
							obj.getLong("size"));
				}
				
				populateList(file);
				
				listview.setClickable(true);
				
				getSherlockActivity().sendBroadcast(new Intent(Putio.CUSTOM_INTENT2));
			} catch (FileNotFoundException e) {
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		protected PutioFileData[] doInBackground(Void... nothing) {
			JSONObject json;
			JSONArray array;
			
			try {
				InputStream is = utils.getListJsonData(baseUrl + "files/list"
						+ tokenWithStuff, currentFolderId);

				String string = utils.convertStreamToString(is);
				json = new JSONObject(string);
				
				if (currentFolderId != 0) {
					try {
						parentParentId = json.getJSONObject("parent").getInt("parent_id");
					} catch (JSONException e) {
						parentParentId = 0;
					}
				}

				array = json.getJSONArray("files");
				newId = json.getJSONObject("parent").getInt("id");
				origId = newId;
				PutioFileData[] file = new PutioFileData[array.length()];
				
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);

					file[i] = new PutioFileData(
							obj.getBoolean("is_shared"),
							obj.getString("name"),
							obj.getString("screenshot"),
							obj.getString("created_at"),
							obj.getInt("parent_id"),
							utils.stringToBooleanHack(obj.getString("is_mp4_available")),
							obj.getString("content_type"),
							obj.getInt("id"),
							obj.getLong("size"));
				}
				
				File output = new File(getSherlockActivity().getCacheDir(), currentFolderId + ".json");
				FileOutputStream fos = FileUtils.openOutputStream(output);
				fos.write(string.getBytes());
				fos.close();
				
				return file;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public void onPostExecute(final PutioFileData[] file) {
			populateList(file);
		}
		
		private void populateList(final PutioFileData[] file) {
			final ArrayList<PutioFileLayout> files = new ArrayList<PutioFileLayout>();
			for (int i = 0; i < file.length; i++) {
				int iconResource = PutioFileData.icons[file[i].contentTypeIndex];

				if (isAdded()) {
					files.add(new PutioFileLayout(
							file[i].name,
							getString(R.string.size_is)
									+ " "
									+ PutioFileUtils.humanReadableByteCount(file[i].size, true),
							iconResource));
				}
			}
			
			index = listview.getFirstVisiblePosition();
			v = listview.getChildAt(0);
			top = (v == null) ? 0 : v.getTop();
			
			if (newId != origIdBefore) {
				for (int i = 0; i < listview.getCount(); i++) {
					listview.setItemChecked(i, false);
				}
			}
			
			adapter.clear();
			if (currentFolderId != 0) {
				adapter.add(new PutioFileLayout("Up",
						"Go back to the previous folder",
						R.drawable.ic_back));
			}
			fileData = file;

			for (int ii = 0; ii < files.size(); ii++) {
				adapter.add(files.get(ii));
			}
			listview.setSelectionFromTop(index, top);
			
			setShowRefreshButton(true);
			
			fileLayouts = files;
		}
	}
	
	private int getAdjustedPosition(int position) {
		if (currentFolderId == 0) {
			return position;
		} else {
			return position - 1;
		}
	}
	
	public PutioFileData getFileAtId(int id) {
		return fileData[id];
	}
	
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        listview.setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    public void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            listview.setItemChecked(mActivatedPosition, false);
        } else {
            listview.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
	
	public void goBack() {
		currentFolderId = parentParentId;
		invalidateList();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt("currentFolderId", currentFolderId);
	}
}