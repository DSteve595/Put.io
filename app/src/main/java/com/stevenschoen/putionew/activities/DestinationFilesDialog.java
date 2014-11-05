package com.stevenschoen.putionew.activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.fragments.Files;
import com.stevenschoen.putionew.model.files.PutioFileData;

public class DestinationFilesDialog extends Files {

    private DestinationFilesDialog.Callbacks mCallbacks;

    public interface Callbacks {
        public void onDestinationFolderSelected(PutioFileData folder);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Putio_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        TextView textTitle = (TextView) view.findViewById(R.id.dialog_title);
        textTitle.setText(getString(R.string.choose_folder));

        view.findViewById(R.id.button_destination_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        view.findViewById(R.id.button_destination_choose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.onDestinationFolderSelected(getState().currentFolder);
                getDialog().dismiss();
            }
        });

        return view;
    }
}