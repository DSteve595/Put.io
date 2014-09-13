package com.stevenschoen.putionew.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.fragments.Files;
import com.stevenschoen.putionew.model.files.PutioFileData;

/**
 * Created by simon_xomo on 2014-09-13.
 */
public class DestinationFilesDialog extends Files {

    private DestinationFilesDialog.Callbacks mCallbacks;

    public interface Callbacks {
        public void onDestinationFolderSelected();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mCallbacks = (Callbacks) activity;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.dialog_destination;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        view.findViewById(R.id.button_destination_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        view.findViewById(R.id.button_destination_choose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.onDestinationFolderSelected();
                getDialog().dismiss();
            }
        });
        return view;
    }
}
