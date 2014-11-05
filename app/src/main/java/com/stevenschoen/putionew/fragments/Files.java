package com.stevenschoen.putionew.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.stevenschoen.putionew.FilesAdapter;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.activities.DestinationFilesDialog;
import com.stevenschoen.putionew.activities.FileDetailsActivity;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFileData;
import com.stevenschoen.putionew.model.responses.BasePutioResponse;
import com.stevenschoen.putionew.model.responses.CachedFilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesSearchResponse;

import java.util.ArrayList;
import java.util.List;

public class Files extends DialogFragment implements SwipeRefreshLayout.OnRefreshListener {

	private static final int VIEWMODE_LIST = 1;
	private static final int VIEWMODE_LISTOREMPTY = 2;
	private static final int VIEWMODE_LOADING = 3;
	private static final int VIEWMODE_EMPTY = 4;
	private static final int VIEWMODE_LISTORLOADING = 5;
	private int viewMode = VIEWMODE_LIST;

    private int highlightFileId = -1;

    private int[] mIdsToMove;

    private boolean buttonUpFolderShown;
	private View buttonUpFolder;

    public interface Callbacks {
        public void onFileSelected(int id);
        public void onSomethingSelected();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onFileSelected(int id) { }
        @Override
        public void onSomethingSelected() { }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

	private PutioUtils utils;

	private FilesAdapter adapter;
	private ListView listview;
	private SwipeRefreshLayout swipeRefreshLayout;

	private View loadingView;
	private View emptyView;
	private View emptySubfolderView;

	private boolean hasUpdated = false;

	private ArrayList<PutioFileData> fileData;

	public static class State {
		public boolean isSearch;
        public String searchQuery;
        public PutioFileData currentFolder;
        public int requestedId;
	}

	protected State state = new State();

	private int origId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		try {
            state.requestedId = savedInstanceState.getInt("requestedId");
			state.currentFolder = savedInstanceState.getParcelable("currentFolder");
			state.isSearch = savedInstanceState.getBoolean("isSearch");
            state.searchQuery = savedInstanceState.getString("searchQuery");
			origId = savedInstanceState.getInt("origId");
			fileData =  savedInstanceState.getParcelableArrayList("fileData");
			hasUpdated = savedInstanceState.getBoolean("hasUpdated");
		} catch (NullPointerException e) {
            state.requestedId = 0;
			state.currentFolder = new PutioFileData();
            state.currentFolder.id = 0;
			state.isSearch = false;
			origId = 0;
			fileData = new ArrayList<>();
			hasUpdated = false;
		}

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();

		utils.getEventBus().register(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(getLayoutResId(), container, false);

		listview = (ListView) view.findViewById(R.id.fileslist);
        if (!UIUtils.isTV(getActivity())) {
            swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.filesSwipeRefresh);
            swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(
                    R.color.putio_accent,
                    R.color.putio_accent,
                    R.color.putio_accent,
                    R.color.putio_accent);
		}

		adapter = new FilesAdapter(getActivity(), fileData);
		listview.setAdapter(adapter);
        listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listview.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                mode.setTitle(listview.getCheckedItemCount() + " selected");
                mode.getMenu().clear();
                if (listview.getCheckedItemCount() > 1) {
                    mode.getMenuInflater().inflate(R.menu.context_files_multiple, mode.getMenu());
                } else {
                    mode.getMenuInflater().inflate(R.menu.context_files, mode.getMenu());
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (listview.getCheckedItemCount() > 1) {
                    mode.getMenuInflater().inflate(R.menu.context_files_multiple, mode.getMenu());
                } else {
                    mode.getMenuInflater().inflate(R.menu.context_files, mode.getMenu());
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTitle(listview.getCheckedItemCount() + " selected");
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                SparseBooleanArray checkedPositionsBooleans = listview.getCheckedItemPositions();
                ArrayList<Integer> checkedPositions = new ArrayList<>();
                for (int i = 0; i < checkedPositionsBooleans.size(); i++) {
                    if (checkedPositionsBooleans.valueAt(i)) {
                        checkedPositions.add(checkedPositionsBooleans.keyAt(i));
                    }
                }
                int[] checkedPositionsArray = new int[checkedPositions.size()];
                for (int i = 0; i < checkedPositions.size(); i++) {
                    checkedPositionsArray[i] = checkedPositions.get(i);
                }

                listview.clearChoices();
                mode.finish();

                switch (item.getItemId()) {
                    case R.id.context_download:
                        initDownloadFiles(checkedPositionsArray);
                        return true;
                    case R.id.context_copydownloadlink:
                        initCopyFileDownloadLink(checkedPositionsArray[0]);
                        return true;
                    case R.id.context_rename:
                        initRenameFile(checkedPositionsArray[0]);
                        return true;
                    case R.id.context_delete:
                        initDeleteFile(checkedPositionsArray);
                        return true;
                    case R.id.context_move:
                        initMoveFile(checkedPositionsArray);
                        return true;
                }

                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) { }
        });
		if (UIUtils.isTablet(getActivity())) {
			listview.setVerticalFadingEdgeEnabled(true);
			listview.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);
            listview.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
            listview.setFastScrollAlwaysVisible(true);
		}
		listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position, long id) {
				mCallbacks.onSomethingSelected();

				state.isSearch = false;

				PutioFileData file = fileData.get(position);
				if (!file.isFolder()) {
					if (!UIUtils.isTablet(getActivity())) {
						Intent detailsIntent = new Intent(getActivity(),
								FileDetailsActivity.class);
						detailsIntent.putExtra("fileData", file);
						startActivity(detailsIntent);
					} else {
						mCallbacks.onFileSelected(position);
					}
				} else {
					state.requestedId = file.id;
					invalidateList(true);
				}
			}
		});

		buttonUpFolder = view.findViewById(R.id.files_up);
        PutioUtils.setupFab(buttonUpFolder);
        buttonUpFolder.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				goBack();
			}
		});

        buttonUpFolderShown = false;

        buttonUpFolder.post(new Runnable() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                if (isInSubfolderOrSearch()) {
                    upButtonShow(false);
                } else {
                    upButtonHide(false);
                }
            }
        });

		loadingView = view.findViewById(R.id.files_loading);
		emptyView = view.findViewById(R.id.files_empty);
		ImageButton buttonRefresh = (ImageButton) emptyView.findViewById(R.id.button_filesempty_refresh);
		buttonRefresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				invalidateList(false);
			}
		});
		emptySubfolderView = view.findViewById(R.id.files_empty_subfolder);

		if (savedInstanceState == null) {
			buttonUpFolder.post(new Runnable() {
				@Override
				public void run() {
                    buttonUpFolder.setTranslationY(buttonUpFolder.getHeight());
				}
			});
			invalidateList(true);
			setViewMode(VIEWMODE_LOADING);
		} else {
			setViewMode(VIEWMODE_LISTORLOADING);
		}

		return view;
	}

    protected int getLayoutResId() {
        return R.layout.files;
    }

    private void setViewMode(int mode) {
		switch (mode) {
            case VIEWMODE_LIST:
                listview.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                emptySubfolderView.setVisibility(View.GONE);
                break;
            case VIEWMODE_LOADING:
                listview.setVisibility(View.INVISIBLE);
                loadingView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                emptySubfolderView.setVisibility(View.GONE);
                break;
            case VIEWMODE_EMPTY:
                listview.setVisibility(View.INVISIBLE);
                loadingView.setVisibility(View.GONE);
                if (state.currentFolder.id == 0 && !state.isSearch) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptySubfolderView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    emptySubfolderView.setVisibility(View.VISIBLE);
                }
                break;
            case VIEWMODE_LISTOREMPTY:
                if (fileData == null || fileData.isEmpty()) {
                    setViewMode(VIEWMODE_EMPTY);
                } else {
                    setViewMode(VIEWMODE_LIST);
                }
                break;
            case VIEWMODE_LISTORLOADING:
                if ((fileData == null || fileData.isEmpty()) && !hasUpdated) {
                    setViewMode(VIEWMODE_LOADING);
                } else {
                    setViewMode(VIEWMODE_LISTOREMPTY);
                }
                break;
		}
		viewMode = mode;
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof Callbacks) {
            mCallbacks = (Callbacks) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = sDummyCallbacks;
    }

	private void initDownloadFiles(final int... indeces) {
		PutioFileData[] files = new PutioFileData[indeces.length];
		for (int i = 0; i < indeces.length; i++) {
			files[i] = fileData.get(indeces[i]);
		}

		utils.downloadFile(getActivity(), PutioUtils.ACTION_NOTHING, files);
	}

	private void initCopyFileDownloadLink(int index) {
		utils.copyDownloadLink(getActivity(), fileData.get(index).id);
	}

	private void initRenameFile(final int index) {
        utils.renameFileDialog(getActivity(), fileData.get(index), new PutioUtils.RenameCallback() {
            @Override
            public void onRename(PutioFileData file, String newName) {
                fileData.get(getIndexFromFileId(file.id)).name = newName;
                adapter.notifyDataSetChanged();
            }
        }).show();
	}

	private void initDeleteFile(int... indeces) {
		PutioFileData[] files = new PutioFileData[indeces.length];
		for (int i = 0; i < indeces.length; i++) {
			files[i] = fileData.get(indeces[i]);
		}
		utils.showDeleteFilesDialog(getActivity(), false, files);
	}

    private void initMoveFile(int... indeces) {
        DestinationFilesDialog destinationFilesDialog = (DestinationFilesDialog)
                DestinationFilesDialog.instantiate(getActivity(), DestinationFilesDialog.class.getName());
        destinationFilesDialog.show(getFragmentManager(), "dialog");
        mIdsToMove = new int[indeces.length];
        for (int i = 0; i < indeces.length; i++) {
            mIdsToMove[i] = fileData.get(indeces[i]).id;
        }
    }

	public void invalidateList(boolean useCache) {
        if (useCache && UIUtils.isTV(getActivity())) {
            useCache = false;
        }
		utils.getJobManager().addJobInBackground(new PutioRestInterface.GetFilesListJob(
				utils, state.requestedId, useCache));

		if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.files, menu);
        MenuItem buttonSearch = menu.findItem(R.id.menu_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(buttonSearch);
        searchView.setIconifiedByDefault(true);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
	}

    public void initSearch(String query) {
        state.searchQuery = query;

		utils.getJobManager().addJobInBackground(new PutioRestInterface.GetFilesSearchJob(
				utils, query));

		if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
	}

	private void populateList(final List<PutioFileData> files, final PutioFileData folder) {
        state.currentFolder = folder;
		hasUpdated = true;
		final int index = listview.getFirstVisiblePosition();
		View v = listview.getChildAt(0);
		final int top = (v == null) ? 0 : v.getTop();

		if (isInSubfolderOrSearch()) {
            upButtonShow(true);
        } else {
            upButtonHide(true);
        }

		if (files == null) {
			setViewMode(VIEWMODE_EMPTY);
			return;
		}

		fileData.clear();

        if (folder.id == origId) {
            Log.d("asdf", "keeping position");
            listview.setSelectionFromTop(index, top);

            origId = folder.id;
        }

        fileData.addAll(files);
        adapter.notifyDataSetChanged();

		setViewMode(VIEWMODE_LISTOREMPTY);

		if (highlightFileId != -1) {
			final int highlightPos = getIndexFromFileId(highlightFileId);

			listview.requestFocus();
            listview.smoothScrollToPosition(highlightPos);

            highlightFileId = -1;
		}
	}

    private void upButtonHide(boolean animate) {
        int hideY = getUpButtonHideY();

        if (animate) {
            if (buttonUpFolderShown) {
                buttonUpFolder.setVisibility(View.VISIBLE);
                buttonUpFolder.animate()
                        .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                        .setInterpolator(new AccelerateInterpolator())
                        .translationY(hideY);
            }
        } else {
            buttonUpFolder.setTranslationY(hideY);
        }

        listview.setPadding(0, 0, 0, 0);
        buttonUpFolder.setEnabled(false);
        buttonUpFolder.setFocusable(false);

        buttonUpFolderShown = false;
    }

    private void upButtonShow(boolean animate) {
        int hideY = getUpButtonHideY();

        if (animate) {
            if (!buttonUpFolderShown) {
                buttonUpFolder.animate()
                        .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                        .setInterpolator(new DecelerateInterpolator())
                        .translationY(0);
            }
        } else {
            buttonUpFolder.setTranslationY(0);
        }

        listview.setPadding(0, 0, 0, hideY);
        buttonUpFolder.setEnabled(true);
        buttonUpFolder.setFocusable(true);
        buttonUpFolder.setVisibility(View.VISIBLE);

        buttonUpFolderShown = true;
    }

    private int getUpButtonHideY() {
        int hideY = buttonUpFolder.getHeight();
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
                buttonUpFolder.getLayoutParams();
        hideY += params.bottomMargin;

        return hideY;
    }

    protected State getState() {
        return state;
    }

	public int getIndexFromFileId(int fileId) {
		for (int i = 0; i < fileData.size(); i++) {
			if (fileData.get(i).id == fileId) {
				return i;
			}
		}
		return -1;
	}

	public PutioFileData getFileAtPosition(int position) {
		return fileData.get(position);
	}

    public boolean goBack() {
		if (isInSubfolderOrSearch()) {
			if (state.isSearch) {
				state.isSearch = false;
				state.requestedId = 0;
			} else {
				state.requestedId = state.currentFolder.parentId;
			}
			invalidateList(true);

			return true;
		} else {
			return false;
		}
	}

	public boolean isInSubfolderOrSearch() {
		return (state.currentFolder.id != 0 || state.isSearch);
	}

	public void highlightFile(int parentId, int id) {
		state.requestedId = parentId;
        highlightFileId = id;
		invalidateList(true);
	}

	public void setFileChecked(int fileId, boolean checked) {
		listview.setItemChecked(getIndexFromFileId(fileId), checked);
	}

	public void onEventMainThread(FilesListResponse result) {
        if (result != null && result.getParent().id == state.requestedId) {
			state.isSearch = false;
            populateList(result.getFiles(), result.getParent());
			if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
		}
	}

	public void onEventMainThread(CachedFilesListResponse result) {
		if (result != null && result.getParent().id == state.requestedId) {
			state.isSearch = false;
			populateList(result.getFiles(), result.getParent());
		}
	}

	public void onEventMainThread(FilesSearchResponse result) {
		if (result != null && state.searchQuery.equals(result.getQuery())) {
			state.isSearch = true;
			populateList(result.getFiles(), null);
			if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
		}
	}

	public void onEventMainThread(BasePutioResponse.FileChangingResponse result) {
		if (result.getStatus().equals("OK")) {
			invalidateList(false);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

        outState.putInt("requestedId", state.requestedId);
		outState.putParcelable("currentFolder", state.currentFolder);
		outState.putInt("origId", origId);
		outState.putBoolean("isSearch", state.isSearch);
        outState.putString("searchQuery", state.searchQuery);
		outState.putParcelableArrayList("fileData", fileData);
		outState.putBoolean("hasUpdated", hasUpdated);
	}

	@Override
	public void onDestroy() {
		utils.getEventBus().unregister(this);

		super.onDestroy();
	}

	@Override
	public void onRefresh() {
		invalidateList(false);
	}

    public void onDestinationFolderSelected(PutioFileData folder) {
        // TODO confirmation popup?
        int selectedFolderId = folder.id;
        utils.getJobManager().addJobInBackground(new PutioRestInterface.PostMoveFilesJob(utils, selectedFolderId, mIdsToMove));
        Toast.makeText(getActivity(), getString(R.string.filemoved), Toast.LENGTH_SHORT).show();
    }
}