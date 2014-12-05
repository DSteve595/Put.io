package com.stevenschoen.putionew.activities;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.fragments.Files;
import com.stevenschoen.putionew.model.files.PutioFile;

public class DestinationFilesDialog extends Files {

    private DestinationFilesDialog.Callbacks callbacks;

    public interface Callbacks {
        public void onDestinationFolderSelected(PutioFile folder);
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
                if (callbacks != null) {
                    callbacks.onDestinationFolderSelected(getState().currentFolder);
                }
                getDialog().dismiss();
            }
        });

        return view;
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }
}