package com.stevenschoen.putionew.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.files.PutioFileData;

import java.lang.reflect.Field;

public class FilesAndFileDetails extends NoClipSupportFragment implements FileDetails.Callbacks {

    private static final String TAG_FILES = "files";
    private static final String TAG_FILEDETAILS = "fileDetails";
    private Callbacks callbacks;
    private Files files;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle filesArgs = new Bundle();
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("files_state")) {
                filesArgs.putParcelable("state", savedInstanceState.getParcelable("files_state"));
            }
        }
        files = (Files) Fragment.instantiate(getActivity(), Files.class.getName(), filesArgs);
        files.setCallbacks(new Files.Callbacks() {
            @Override
            public void onFileSelected(PutioFileData file) {
                if (hasCallbacks()) {
                    callbacks.filesRequestAttention();
                }
                showDetails(file);
            }

            @Override
            public void onSomethingSelected() {
                removeDetails();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        View view = inflater.inflate(R.layout.tablet_files, container, false);

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.add(R.id.fragment_files, files, TAG_FILES);
        Bundle detailsArgs = new Bundle();
        if (savedInstanceState != null) {
            Log.d("asdf", "not null");
            if (savedInstanceState.containsKey("details_state")) {
                Log.d("asdf", "has state");
                detailsArgs.putParcelable("state", savedInstanceState.getParcelable("details_state"));
                FileDetails details = (FileDetails) Fragment.instantiate(getActivity(), FileDetails.class.getName(), detailsArgs);
                ft.add(R.id.fragment_details, details, TAG_FILEDETAILS);
            }
        }
        ft.commit();

        return view;
    }

    public Files getFilesFragment() {
        return (Files) getChildFragmentManager().findFragmentByTag(TAG_FILES);
    }

    public FileDetails getFileDetailsFragment() {
        return (FileDetails) getChildFragmentManager().findFragmentByTag(TAG_FILEDETAILS);
    }

    private void showDetails(PutioFileData file) {
        Bundle fileDetailsBundle = new Bundle();
        fileDetailsBundle.putParcelable("fileData", file);
        FileDetails fileDetailsFragment = (FileDetails) FileDetails.instantiate(
                getActivity(), FileDetails.class.getName(), fileDetailsBundle);
        fileDetailsFragment.setCallbacks(this);

        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_fromleft,
                        R.anim.slide_out_toright)
                .add(R.id.fragment_details, fileDetailsFragment, TAG_FILEDETAILS)
                .commit();
    }

    public void removeDetails() {
        FileDetails details = getFileDetailsFragment();
        if (isDetailsShown()) {
            getChildFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_fromleft,
                            R.anim.slide_out_toright)
                    .remove(details)
                    .commit();
        }
    }

    public boolean isDetailsShown() {
        return isDetailsShown(getFileDetailsFragment());
    }

    private boolean isDetailsShown(FileDetails details) {
        return (details != null && details.isAdded());
    }

    @Override
    public void onFileDetailsClosed() {
        removeDetails();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("files_state", getFilesFragment().getState());

        FileDetails details = getFileDetailsFragment();
        if (isDetailsShown(details)) {
            outState.putParcelable("details_state", details.getState());
        }
    }

    // Dumb hack, thanks Google
    @Override
    public void onDetach() {
        super.onDetach();

        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        Files files = getFilesFragment();
        if (files != null) {
            files.setMenuVisibility(menuVisible);
        }

        FileDetails details = getFileDetailsFragment();
        if (details != null) {
            details.setMenuVisibility(menuVisible);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        Files files = getFilesFragment();
        if (files != null) {
            files.setUserVisibleHint(isVisibleToUser);
        }

        FileDetails details = getFileDetailsFragment();
        if (details != null) {
            details.setUserVisibleHint(isVisibleToUser);
        }
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    private boolean hasCallbacks() {
        return (callbacks != null);
    }

    public interface Callbacks {
        public void filesRequestAttention();
    }
}
