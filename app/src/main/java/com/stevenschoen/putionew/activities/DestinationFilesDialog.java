package com.stevenschoen.putionew.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.fragments.Files;
import com.stevenschoen.putionew.model.files.PutioFile;

public class DestinationFilesDialog extends Files {

    private DestinationFilesDialog.Callbacks callbacks;

    public interface Callbacks {
        void onDestinationFolderSelected(PutioFile folder);
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
                if (callbacks != null) {
                    callbacks.onDestinationFolderSelected(getState().currentFolder);
                }
                getDialog().dismiss();
            }
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.choose_folder);

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    return goBack();
                }
                return false;
            }
        });

        return dialog;
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }
}