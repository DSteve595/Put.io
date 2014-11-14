package com.stevenschoen.putionew.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.stevenschoen.putionew.AutoResizeTextView;
import com.stevenschoen.putionew.DividerItemDecoration;
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

    public interface Callbacks {
        public void onFileSelected(PutioFileData file);
        public void onSomethingSelected();
        public void currentFolderRefreshed();
    }

    private Callbacks callbacks;

	private PutioUtils utils;

    private ActionMode actionMode;

	private RecyclerView filesList;
    private FilesAdapter filesAdapter;
	private SwipeRefreshLayout swipeRefreshLayout;

	private View loadingView;
	private View emptyView;
	private View emptySubfolderView;

    private View viewCurrentFolder;
    private ImageView iconCurrentFolder;
    private AutoResizeTextView textCurrentFolder;

	private boolean hasUpdated = false;

	protected State state;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

        if (savedInstanceState != null && savedInstanceState.containsKey("state")) {
            state = savedInstanceState.getParcelable("state");
        }
        if (state == null) {
            if (getArguments() != null && getArguments().containsKey("state")) {
                state = getArguments().getParcelable("state");
            }
            if (state == null) {
                state = new State();
                state.requestedId = 0;
                state.currentFolder = new PutioFileData();
                state.currentFolder.id = 0;
                state.isSearch = false;
                state.origId = 0;
                state.fileData = new ArrayList<>();
                state.hasUpdated = false;
            }
        }

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();

		utils.getEventBus().register(this);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(getLayoutResId(), container, false);

		filesList = (RecyclerView) view.findViewById(R.id.fileslist);
        if (!UIUtils.isTV(getActivity())) {
            swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.filesSwipeRefresh);
            swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(
                    R.color.putio_accent,
                    R.color.putio_accent,
                    R.color.putio_accent,
                    R.color.putio_accent);
		}
        LinearLayoutManager filesManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        filesList.setLayoutManager(filesManager);
		filesAdapter = new FilesAdapter(state.fileData);
        filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (filesAdapter.isInCheckMode()) {
                    filesAdapter.toggleChecked(position);
                } else {
                    if (hasCallbacks()) {
                        callbacks.onSomethingSelected();
                    }

                    PutioFileData file = getFileAtPosition(position);
                    if (file.isFolder()) {
                        state.isSearch = false;
                        state.requestedId = file.id;
                        invalidateList(true);
                    } else {
                        if (!UIUtils.isTablet(getActivity())) {
                            Intent detailsIntent = new Intent(getActivity(),
                                    FileDetailsActivity.class);
                            detailsIntent.putExtra("fileData", file);
                            startActivity(detailsIntent);
                        } else {
                            if (hasCallbacks()) {
                                callbacks.onFileSelected(getFileAtPosition(position));
                            }
                        }
                    }
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                filesAdapter.toggleChecked(position);
            }
        });
        filesAdapter.setItemsCheckedChangedListener(new FilesAdapter.OnItemsCheckedChangedListener() {
            @Override
            public void onItemsCheckedChanged() {
                if (getActivity() instanceof ActionBarActivity) {
                    if (actionMode == null) {
                        if (filesAdapter.isInCheckMode()) {
                            ActionBarActivity activity = (ActionBarActivity) getActivity();
                            actionMode = activity.startSupportActionMode(new ActionMode.Callback() {
                                @Override
                                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                                    if (filesAdapter.isInCheckMode()) {
                                        inflate(mode, menu);
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }

                                @Override
                                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                                    inflate(mode, menu);
                                    if (filesAdapter.isInCheckMode()) {
                                        mode.setTitle(getString(R.string.x_selected, filesAdapter.getCheckedCount()));
                                    } else {
                                        mode.finish();
                                    }
                                    return true;
                                }

                                public void inflate(ActionMode mode, Menu menu) {
                                    menu.clear();
                                    if (filesAdapter.getCheckedCount() > 1) {
                                        mode.getMenuInflater().inflate(R.menu.context_files_multiple, menu);
                                    } else {
                                        mode.getMenuInflater().inflate(R.menu.context_files, menu);
                                    }
                                }

                                @Override
                                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                                    int[] checkedPositions = filesAdapter.getCheckedPositions();

                                    switch (menuItem.getItemId()) {
                                        case R.id.context_download:
                                            initDownloadFiles(checkedPositions);
                                            return true;
                                        case R.id.context_copydownloadlink:
                                            initCopyFileDownloadLink(checkedPositions[0]);
                                            return true;
                                        case R.id.context_rename:
                                            initRenameFile(checkedPositions[0]);
                                            return true;
                                        case R.id.context_delete:
                                            initDeleteFile(checkedPositions);
                                            return true;
                                        case R.id.context_move:
                                            initMoveFile(checkedPositions);
                                            return true;
                                    }

                                    return false;
                                }

                                @Override
                                public void onDestroyActionMode(ActionMode actionMode) {
                                    filesAdapter.clearChecked();
                                    Files.this.actionMode = null;
                                }
                            });
                        }
                    } else {
                        actionMode.invalidate();
                    }
                }
            }
        });
        if (state.checkedIds != null) {
            filesAdapter.addCheckedIds(state.checkedIds);
        }
        filesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (state.currentFolder.id != state.origId) {
                    filesList.scrollToPosition(0);
                }
                state.origId = state.currentFolder.id;

                super.onChanged();
            }
        });
		filesList.setAdapter(filesAdapter);
        filesList.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL_LIST,
                (int) getResources().getDimension(R.dimen.files_divider_inset),
                0));
        filesList.setItemAnimator(new DefaultItemAnimator());
		if (UIUtils.isTablet(getActivity())) {
			filesList.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);
            filesList.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
		}

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

        viewCurrentFolder = view.findViewById(R.id.holder_files_currentfolder);
        iconCurrentFolder = (ImageView) viewCurrentFolder.findViewById(R.id.icon_files_currentfolder);
        textCurrentFolder = (AutoResizeTextView) viewCurrentFolder.findViewById(R.id.text_files_currentfolder);
        textCurrentFolder.setMaxTextSize(20);
        textCurrentFolder.setMinTextSize(18);

        refreshCurrentFolderView();

		if (savedInstanceState == null) {
			invalidateList(true);
			setViewMode(VIEWMODE_LOADING);
		} else {
			setViewMode(VIEWMODE_LISTORLOADING);
		}

		return view;
	}

    private void refreshCurrentFolderView() {
        if (isInSubfolderOrSearch()) {
            viewCurrentFolder.setVisibility(View.VISIBLE);

            textCurrentFolder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            if (state.isSearch) {
                iconCurrentFolder.setVisibility(View.GONE);
                textCurrentFolder.setText(state.searchQuery);
            } else {
                iconCurrentFolder.setVisibility(View.VISIBLE);
                Picasso.with(getActivity()).load(state.currentFolder.icon).into(iconCurrentFolder);
                textCurrentFolder.setText(state.currentFolder.name);
            }
        } else {
            viewCurrentFolder.setVisibility(View.GONE);
        }
    }

    protected int getLayoutResId() {
        return R.layout.files;
    }

    private void setViewMode(int mode) {
		switch (mode) {
            case VIEWMODE_LIST:
                filesList.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                emptySubfolderView.setVisibility(View.GONE);
                break;
            case VIEWMODE_LOADING:
                filesList.setVisibility(View.INVISIBLE);
                loadingView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                emptySubfolderView.setVisibility(View.GONE);
                break;
            case VIEWMODE_EMPTY:
                filesList.setVisibility(View.INVISIBLE);
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
                if (state.fileData == null || state.fileData.isEmpty()) {
                    setViewMode(VIEWMODE_EMPTY);
                } else {
                    setViewMode(VIEWMODE_LIST);
                }
                break;
            case VIEWMODE_LISTORLOADING:
                if ((state.fileData == null || state.fileData.isEmpty()) && !hasUpdated) {
                    setViewMode(VIEWMODE_LOADING);
                } else {
                    setViewMode(VIEWMODE_LISTOREMPTY);
                }
                break;
		}
		viewMode = mode;
	}

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    private boolean hasCallbacks() {
        return (callbacks != null);
    }

	private void initDownloadFiles(final int... indeces) {
		PutioFileData[] files = new PutioFileData[indeces.length];
		for (int i = 0; i < indeces.length; i++) {
			files[i] = getFileAtPosition(indeces[i]);
		}

		utils.downloadFile(getActivity(), PutioUtils.ACTION_NOTHING, files);
	}

	private void initCopyFileDownloadLink(int index) {
		utils.copyDownloadLink(getActivity(), getFileAtPosition(index).id);
	}

	private void initRenameFile(final int index) {
        utils.renameFileDialog(getActivity(), getFileAtPosition(index), new PutioUtils.RenameCallback() {
            @Override
            public void onRename(PutioFileData file, String newName) {
                getFileAtPosition(getIndexFromFileId(file.id)).name = newName;
                filesAdapter.notifyDataSetChanged();
            }
        }).show();
	}

	private void initDeleteFile(int... indeces) {
		PutioFileData[] files = new PutioFileData[indeces.length];
		for (int i = 0; i < indeces.length; i++) {
			files[i] = getFileAtPosition(indeces[i]);
		}
		utils.deleteFilesDialog(getActivity(), null, files).show();
	}

    private void initMoveFile(int... indeces) {
        DestinationFilesDialog destinationFilesDialog = (DestinationFilesDialog)
                DestinationFilesDialog.instantiate(getActivity(), DestinationFilesDialog.class.getName());
        destinationFilesDialog.show(getFragmentManager(), "dialog");
        mIdsToMove = new int[indeces.length];
        for (int i = 0; i < indeces.length; i++) {
            mIdsToMove[i] = getFileAtPosition(indeces[i]).id;
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
        if (folder != null) {
            state.currentFolder = folder;
        }
		hasUpdated = true;

        refreshCurrentFolderView();

		if (files == null || files.isEmpty()) {
			setViewMode(VIEWMODE_EMPTY);
			return;
		}

		state.fileData.clear();
        state.fileData.addAll(files);
        filesAdapter.notifyDataSetChanged();

		setViewMode(VIEWMODE_LISTOREMPTY);

		if (highlightFileId != -1) {
            filesList.post(new Runnable() {
                @Override
                public void run() {
                    int highlightPos = getIndexFromFileId(highlightFileId);
                    if (highlightPos != -1) {
                        filesList.requestFocus();
                        filesList.smoothScrollToPosition(highlightPos);
                        highlightFileId = -1;
                    }
                }
            });
		}

        if (folder != null) {
            if (state.origId == folder.id && callbacks != null) {
                callbacks.currentFolderRefreshed();
            }
        }
	}

    public State getState() {
        return state;
    }

	public int getIndexFromFileId(long fileId) {
		for (int i = 0; i < state.fileData.size(); i++) {
			if (getFileAtPosition(i).id == fileId) {
				return i;
			}
		}
		return -1;
	}

    public boolean isShowingFileId(long fileId) {
        return getIndexFromFileId(fileId) != -1;
    }

	public PutioFileData getFileAtPosition(int position) {
		return state.fileData.get(position);
	}

    public boolean goBack() {
        if (filesAdapter.isInCheckMode()) {
            filesAdapter.clearChecked();
            return true;
        } else if (isInSubfolderOrSearch()) {
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

        state.checkedIds = new long[filesAdapter.getCheckedIds().size()];
        List<Long> checkedIds = filesAdapter.getCheckedIds();
        for (int i = 0; i < checkedIds.size(); i++) {
            state.checkedIds[i] = checkedIds.get(i);
        }

        outState.putParcelable("state", state);
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
        int selectedFolderId = folder.id;
        utils.getJobManager().addJobInBackground(new PutioRestInterface.PostMoveFilesJob(utils, selectedFolderId, mIdsToMove));
        Toast.makeText(getActivity(), getString(R.string.filemoved), Toast.LENGTH_SHORT).show();
    }

    public static class State implements Parcelable {
        public boolean isSearch;
        public String searchQuery;
        public PutioFileData currentFolder;
        public long[] checkedIds;
        public int origId;
        public int requestedId;
        public boolean hasUpdated;
        public List<PutioFileData> fileData;

        public State() { }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte(isSearch ? (byte) 1 : (byte) 0);
            dest.writeString(this.searchQuery);
            dest.writeParcelable(this.currentFolder, 0);
            dest.writeLongArray(this.checkedIds);
            dest.writeInt(this.origId);
            dest.writeInt(this.requestedId);
            dest.writeByte(hasUpdated ? (byte) 1 : (byte) 0);
            dest.writeTypedList(fileData);
        }

        private State(Parcel in) {
            this.isSearch = in.readByte() != 0;
            this.searchQuery = in.readString();
            this.currentFolder = in.readParcelable(PutioFileData.class.getClassLoader());
            this.checkedIds = in.createLongArray();
            this.origId = in.readInt();
            this.requestedId = in.readInt();
            this.hasUpdated = in.readByte() != 0;
            in.readTypedList(fileData, PutioFileData.CREATOR);
        }

        public static final Creator<State> CREATOR = new Creator<State>() {
            public State createFromParcel(Parcel source) {
                return new State(source);
            }

            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }
}