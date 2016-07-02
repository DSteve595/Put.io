package com.stevenschoen.putionew.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.stevenschoen.putionew.AutoResizeTextView;
import com.stevenschoen.putionew.DividerItemDecoration;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.files.DestinationFolderDialogFragment;
import com.stevenschoen.putionew.files.FilesAdapter;
import com.stevenschoen.putionew.model.files.PutioFile;
import com.stevenschoen.putionew.model.responses.FilesSearchResponse;
import com.trello.rxlifecycle.components.support.RxAppCompatDialogFragment;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class Files extends RxAppCompatDialogFragment implements SwipeRefreshLayout.OnRefreshListener {

	private static final int VIEWMODE_LIST = 1;
	private static final int VIEWMODE_LISTOREMPTY = 2;
	private static final int VIEWMODE_LOADING = 3;
	private static final int VIEWMODE_EMPTY = 4;
	private static final int VIEWMODE_LISTORLOADING = 5;

    private long highlightFileId = -1;

	public interface Callbacks {
        void onFileSelected(PutioFile file);
        void onSomethingSelected();
        void currentFolderRefreshed();
    }

    private Callbacks callbacks;

	private PutioUtils utils;

    private ActionMode actionMode;

	private RecyclerView filesListView;
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
        if (state == null && getArguments() != null && getArguments().containsKey("state")) {
            state = getArguments().getParcelable("state");
        }
        if (state == null) {
            state = new State();
            state.requestedId = 0;
            state.currentFolder = new PutioFile();
            state.currentFolder.id = 0;
            state.isSearch = false;
            state.origId = 0;
            state.fileData = new ArrayList<>();
            hasUpdated = false;
        }

		utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(getLayoutResId(), container, false);

		filesListView = (RecyclerView) view.findViewById(R.id.folder_list);
        if (getArguments() != null && getArguments().getBoolean("padForFab", false)) {
            PutioUtils.padForFab(filesListView);
        }
        if (!UIUtils.isTV(getActivity())) {
            swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.folder_swiperefresh);
            swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(R.color.putio_accent);
		}
        LinearLayoutManager filesManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        filesListView.setLayoutManager(filesManager);
		filesAdapter = new FilesAdapter(state.fileData, null, null);
//        filesAdapter.setOnItemClickListener(new FilesAdapter.OnItemClickListener() {
//            @Override
//            public void onItemClick(View view, int position) {
//                if (filesAdapter.isInCheckMode()) {
//                    filesAdapter.togglePositionChecked(position);
//                } else {
//                    if (hasCallbacks()) {
//                        callbacks.onSomethingSelected();
//                    }
//
//                    PutioFile file = getFileAtPosition(position);
//                    if (file.isFolder()) {
//                        state.isSearch = false;
//                        state.requestedId = file.id;
//                        invalidateList(true);
//                    } else {
//                        if (!UIUtils.isTablet(getActivity())) {
//                            Intent detailsIntent = new Intent(getActivity(),
//                                    FileDetailsActivity.class);
//                            detailsIntent.putExtra("fileData", file);
//                            startActivity(detailsIntent);
//                        } else {
//                            if (hasCallbacks()) {
//                                callbacks.onFileSelected(getFileAtPosition(position));
//                            }
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onItemLongClick(View view, int position) {
//                filesAdapter.togglePositionChecked(position);
//            }
//        });
        filesAdapter.setItemsCheckedChangedListener(new FilesAdapter.OnItemsCheckedChangedListener() {
            @Override
            public void onItemsCheckedChanged() {
                if (getActivity() instanceof AppCompatActivity) {
                    if (actionMode == null) {
                        if (filesAdapter.isInCheckMode()) {
                            AppCompatActivity activity = (AppCompatActivity) getActivity();
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
                                        mode.setTitle(getString(R.string.x_selected, filesAdapter.checkedCount()));
                                    } else {
                                        mode.finish();
                                    }
                                    return true;
                                }

                                public void inflate(ActionMode mode, Menu menu) {
                                    menu.clear();
                                    if (filesAdapter.checkedCount() > 1) {
//                                        mode.getMenuInflater().inflate(R.menu.context_files_multiple, menu);
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
                                            initCopyFileDownloadLink(checkedPositions);
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
                    filesListView.scrollToPosition(0);
                    getActivity().supportInvalidateOptionsMenu();
                }
                state.origId = state.currentFolder.id;

                super.onChanged();
            }
        });
		filesListView.setAdapter(filesAdapter);
        filesListView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL_LIST,
                (int) getResources().getDimension(R.dimen.files_divider_inset),
                0));
        filesListView.setItemAnimator(new DefaultItemAnimator());
		if (UIUtils.isTablet(getActivity())) {
			filesListView.setScrollBarStyle(ScrollView.SCROLLBARS_OUTSIDE_INSET);
            filesListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
		}

//		loadingView = view.findViewById(R.id.files_loading);
//		emptyView = view.findViewById(R.id.files_empty);
//		ImageButton buttonRefresh = (ImageButton) emptyView.findViewById(R.id.button_filesempty_refresh);
//		buttonRefresh.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				invalidateList(false);
//			}
//		});
//		emptySubfolderView = view.findViewById(R.id.files_empty_subfolder);

//        viewCurrentFolder = view.findViewById(R.id.files_currentfolder_holder);
//        iconCurrentFolder = (ImageView) viewCurrentFolder.findViewById(R.id.files_currentfolder_icon);
//        textCurrentFolder = (AutoResizeTextView) viewCurrentFolder.findViewById(R.id.files_currentfolder_name);
//        textCurrentFolder.setMaxTextSize(20);
//        textCurrentFolder.setMinTextSize(18);

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
                iconCurrentFolder.setImageResource(R.drawable.ic_currentfolder_search);
                textCurrentFolder.setText(state.searchQuery);
            } else {
                iconCurrentFolder.setImageResource(R.drawable.ic_putio_folder);
                textCurrentFolder.setText(state.currentFolder.name);
            }
        } else {
            viewCurrentFolder.setVisibility(View.GONE);
        }
    }

    protected int getLayoutResId() {
        return R.layout.folder;
    }

    private void setViewMode(int mode) {
		switch (mode) {
            case VIEWMODE_LIST:
                filesListView.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                emptySubfolderView.setVisibility(View.GONE);
                break;
            case VIEWMODE_LOADING:
                filesListView.setVisibility(View.INVISIBLE);
                loadingView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                emptySubfolderView.setVisibility(View.GONE);
                break;
            case VIEWMODE_EMPTY:
                filesListView.setVisibility(View.INVISIBLE);
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
	}

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    private boolean hasCallbacks() {
        return (callbacks != null);
    }

	private void initDownloadFiles(final int... indeces) {
//        final PutioFile[] files = getFilesAtPositions(indeces);
//        if (indeces.length > 1) {
//            final Dialog dialog = PutioUtils.showPutioDialog(getActivity(), getString(R.string.download_files), R.layout.download_individualorzip);
//            Button buttonIndividual = (Button) dialog.findViewById(R.id.button_indidualorzip_individual);
//            buttonIndividual.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    utils.downloadFiles(getActivity(), PutioUtils.ACTION_NOTHING, files);
//                    dialog.dismiss();
//                }
//            });
//            Button buttonZip = (Button) dialog.findViewById(R.id.button_indidualorzip_zip);
//            buttonZip.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    long fileIds[] = new long[files.length];
//                    String name = files[0].name;
//                    for (int i = 0; i < files.length; i++) {
//                        fileIds[i] = files[i].id;
//                        if (i > 0) {
//                            name += (", " + files[i].name);
//                        }
//                    }
//					name += ".zip";
//                    PutioUtils.download(getActivity(), Uri.parse(utils.getZipDownloadUrl(fileIds)), name)
//                            .subscribe(new Action1<Long>() {
//                                @Override
//                                public void call(Long aLong) {
//                                    Toast.makeText(getContext(), getString(R.string.downloadstarted), Toast.LENGTH_SHORT).show();
//                                }
//                            }, new Action1<Throwable>() {
//                                @Override
//                                public void call(Throwable throwable) {
//                                    throwable.printStackTrace();
//                                }
//                            });
//                    dialog.dismiss();
//                }
//            });
//            Button buttonCancel = (Button) dialog.findViewById(R.id.button_indidualorzip_cancel);
//            buttonCancel.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dialog.cancel();
//                }
//            });
//        } else if (indeces.length > 0) {
//            utils.downloadFiles(getActivity(), PutioUtils.ACTION_NOTHING, files);
//        }
	}

	private void initCopyFileDownloadLink(int... indeces) {
        if (indeces.length == 1) {
            PutioFile file = getFileAtPosition(indeces[0]);
            if (file.isFolder()) {
                utils.copyZipDownloadLink(getActivity(), file);
            } else {
                utils.copyDownloadLink(getActivity(), getFileAtPosition(indeces[0]));
            }
        } else if (indeces.length > 0) {
            utils.copyZipDownloadLink(getActivity(), getFilesAtPositions(indeces));
        }
	}

	private void initRenameFile(final int index) {
//        utils.renameFileDialog(getActivity(), new PutioUtils.RenameCallback() {
//            @Override
//            public void onRenameClicked(PutioFile file, String newName) {
//                getFileAtPosition(getIndexFromFileId(file.id)).name = newName;
//                filesAdapter.notifyItemChanged(index);
//            }
//
//            @Override
//            public void onRenameFinished() {
//                invalidateList(false);
//            }
//        }, getFileAtPosition(index)).show();
	}

	private void initDeleteFile(final int... indeces) {
		PutioFile[] files = new PutioFile[indeces.length];
		for (int i = 0; i < indeces.length; i++) {
			files[i] = getFileAtPosition(indeces[i]);
		}
//		utils.deleteFilesDialog(getActivity(), new PutioUtils.DeleteCallback() {
//			@Override
//			public void onDeleteClicked() {
//				long[] ids = new long[indeces.length];
//				for (int i = 0; i < indeces.length; i++) {
//					ids[i] = getFileAtPosition(indeces[i]).id;
//				}
//				for (long id : ids) {
//					int index = getIndexFromFileId(id);
//					getState().fileData.remove(index);
//					filesAdapter.notifyItemRemoved(index);
//				}
//			}
//
//			@Override
//			public void onDeleteFinished() {
//				invalidateList(false);
//			}
//		}, files).show();
	}

    private void initMoveFile(final int... indeces) {
        DestinationFolderDialogFragment destinationFilesDialog = (DestinationFolderDialogFragment)
                DestinationFolderDialogFragment.instantiate(getActivity(), DestinationFolderDialogFragment.class.getName());
//        destinationFilesDialog.show(getFragmentManager(), "dialog");
        destinationFilesDialog.setCallbacks(new DestinationFolderDialogFragment.Callbacks() {
			@Override
			public void onSelectionEnded() {

			}

			@Override
			public void onSelectionStarted() {

			}

			@Override
			public void onCurrentFileChanged() {

			}

			//			@Override
//			public void onDestinationFolderSelected(PutioFile folder) {
//				long[] idsToMove = new long[indeces.length];
//				for (int i = 0; i < indeces.length; i++) {
//					idsToMove[i] = getFileAtPosition(indeces[i]).id;
//				}
//				for (long id : idsToMove) {
//					int index = getIndexFromFileId(id);
//					getState().fileData.remove(index);
//					filesAdapter.notifyItemRemoved(index);
//				}
//				long selectedFolderId = folder.id;
//				utils.getRestInterface().moveFile(PutioUtils.longsToString(idsToMove), selectedFolderId)
//						.compose(Files.this.<BasePutioResponse.FileChangingResponse>bindToLifecycle())
//						.observeOn(AndroidSchedulers.mainThread())
//						.subscribe(new Action1<BasePutioResponse.FileChangingResponse>() {
//							@Override
//							public void call(BasePutioResponse.FileChangingResponse fileChangingResponse) {
//								invalidateList(false);
//							}
//						}, new Action1<Throwable>() {
//							@Override
//							public void call(Throwable throwable) {
//								throwable.printStackTrace();
//							}
//						});
//				Toast.makeText(getActivity(), getString(R.string.filemoved), Toast.LENGTH_SHORT).show();
//			}
		});
    }

	public void invalidateList(final boolean useCache) {
//        filesListObservable(utils, state.requestedId)
//                .compose(this.<FilesProvider.FilesResponse>bindToLifecycle())
//                .observeOn(AndroidSchedulers.mainThread())
//				.filter(new Func1<FilesProvider.FilesResponse, Boolean>() {
//					@Override
//					public Boolean call(FilesProvider.FilesResponse filesResponse) {
//						return (filesResponse.fresh || (useCache && !UIUtils.isTV(getContext())));
//					}
//				})
//                .subscribe(new Action1<FilesProvider.FilesResponse>() {
//					@Override
//					public void call(FilesProvider.FilesResponse response) {
//						if (response != null && response.parent.id == state.requestedId) {
//							state.isSearch = false;
//							populateList(response.files, response.parent);
//							if (response.fresh) {
//								if (swipeRefreshLayout != null)
//									swipeRefreshLayout.setRefreshing(false);
//							}
//						}
//					}
//				}, new Action1<Throwable>() {
//					@Override
//					public void call(Throwable throwable) {
//						throwable.printStackTrace();
//					}
//				});
//
//		if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_files, menu);
        MenuItem itemSearch = menu.findItem(R.id.menu_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(itemSearch);
        searchView.setIconifiedByDefault(true);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
	}

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem itemCreateFolder = menu.findItem(R.id.menu_createfolder);
        itemCreateFolder.setVisible(!state.isSearch);
        itemCreateFolder.setEnabled(!state.isSearch);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_createfolder:
//                utils.createFolderDialog(getActivity(), new PutioUtils.CreateFolderCallback() {
//                    @Override
//                    public void onCreateFolderClicked() { }
//
//                    @Override
//                    public void onCreateFolderFinished() {
//                        invalidateList(false);
//                    }
//                }, state.currentFolder.id).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initSearch(String query) {
        state.searchQuery = query;

        utils.getRestInterface().searchFiles(query)
                .compose(this.<FilesSearchResponse>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<FilesSearchResponse>() {
                    @Override
                    public void call(FilesSearchResponse response) {
                        if (response != null) {
                            state.isSearch = true;
                            populateList(response.getFiles(), null);
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

		if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
    }

	private void populateList(final List<PutioFile> files, final PutioFile folder) {
        if (folder != null) {
            state.currentFolder = folder;
        }
		hasUpdated = true;

        refreshCurrentFolderView();

        state.fileData.clear();
		if (files == null || files.isEmpty()) {
			setViewMode(VIEWMODE_EMPTY);
            filesAdapter.notifyDataSetChanged();
			return;
		}

        state.fileData.clear();
        state.fileData.addAll(files);
        filesAdapter.notifyDataSetChanged();

		setViewMode(VIEWMODE_LISTOREMPTY);

		if (highlightFileId != -1) {
            filesListView.post(new Runnable() {
                @Override
                public void run() {
                    int highlightPos = getIndexFromFileId(highlightFileId);
                    if (highlightPos != -1) {
                        filesListView.requestFocus();
                        filesListView.smoothScrollToPosition(highlightPos);
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

	public PutioFile getFileAtPosition(int position) {
		return state.fileData.get(position);
	}

    public PutioFile[] getFilesAtPositions(int[] positions) {
        PutioFile[] files = new PutioFile[positions.length];
        for (int i = 0; i < positions.length; i++) {
            files[i] = getFileAtPosition(positions[i]);
        }

        return files;
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

	public void highlightFile(long parentId, long id) {
		state.requestedId = parentId;
        highlightFileId = id;
		invalidateList(true);
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
		super.onDestroy();
	}

	@Override
	public void onRefresh() {
		invalidateList(false);
	}

    public static class State implements Parcelable {
        public boolean isSearch;
        public String searchQuery;
        public PutioFile currentFolder;
        public long[] checkedIds;
        public long origId;
        public long requestedId;
        public boolean hasUpdated;
        public List<PutioFile> fileData;

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
            dest.writeLong(this.origId);
            dest.writeLong(this.requestedId);
            dest.writeByte(hasUpdated ? (byte) 1 : (byte) 0);
            dest.writeTypedList(fileData);
        }

        private State(Parcel in) {
            this.isSearch = in.readByte() != 0;
            this.searchQuery = in.readString();
            this.currentFolder = in.readParcelable(PutioFile.class.getClassLoader());
            this.checkedIds = in.createLongArray();
            this.origId = in.readLong();
            this.requestedId = in.readLong();
            this.hasUpdated = in.readByte() != 0;
            this.fileData = new ArrayList<>();
            in.readTypedList(fileData, PutioFile.CREATOR);
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