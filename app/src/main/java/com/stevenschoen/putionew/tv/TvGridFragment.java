package com.stevenschoen.putionew.tv;

import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;

import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFile;
import com.stevenschoen.putionew.model.responses.CachedFilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesListResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by simonreggiani on 15-01-11.
 */
public class TvGridFragment extends android.support.v17.leanback.app.VerticalGridSupportFragment {
    private static final String TAG = "VerticalGridFragment";

    private static final int NUM_COLUMNS = 3;

    private ArrayObjectAdapter mAdapter;
    private PutioUtils mUtils;
    private PutioFile mCurrentFolder;
    private long mCurrentFolderId = 0;
    private List<PutioFile> mCurrentFiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.putio));

        mUtils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();

        mUtils.getEventBus().register(this);

        setupFragment();
    }

    @Override
    public void onDestroy() {
        mUtils.getEventBus().unregister(this);

        super.onDestroy();
    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new TvPutioFileCardPresenter());

        setAdapter(mAdapter);

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o, RowPresenter.ViewHolder viewHolder2, Row row) {
                if (o instanceof PutioFile) {
                    PutioFile file = (PutioFile) o;
                    if (file.isFolder()) {
                        loadFolder(file);
                    } else {
                        playVideo(file);
                    }
                }
            }
        });

        loadFiles();
    }

    private void loadFolder(PutioFile folder) {
        mCurrentFolderId = folder.id;
        loadFiles();
    }

    private void playVideo(PutioFile file) {
        TvPlaybackOverlayActivity.launch(getActivity(), file);
    }

    private void loadFiles() {
        mUtils.getJobManager().addJobInBackground(new PutioRestInterface.GetFilesListJob(mUtils, mCurrentFolderId, true));
    }

    public void onEventMainThread(FilesListResponse result) {
        if (result != null && result.getParent().id == mCurrentFolderId) {
            populateGrid(result.getFiles(), result.getParent());
        }
    }

    public void onEventMainThread(CachedFilesListResponse result) {
        if (result != null && result.getParent().id == mCurrentFolderId) {
            populateGrid(result.getFiles(), result.getParent());
        }
    }

    private void populateGrid(List<PutioFile> files, PutioFile parent) {
        // filter only folders and media files
        List<PutioFile> onlyMediaAndFolders = new ArrayList<>(files.size());
        for (PutioFile file : files) {
            if (file.isFolder() || file.isMedia()) {
                onlyMediaAndFolders.add(file);
            }
        }

        // only refresh if changes
        if (!onlyMediaAndFolders.equals(mCurrentFiles)) {
            mCurrentFolder = parent;
            mCurrentFiles = onlyMediaAndFolders;
            mAdapter.clear();
            mAdapter.addAll(0, mCurrentFiles);
        }
    }

    public boolean isRootFolder() {
        return mCurrentFolderId == 0;
    }

    public void goBack() {
        mCurrentFolderId = mCurrentFolder.parentId;
        loadFiles();
    }
}
