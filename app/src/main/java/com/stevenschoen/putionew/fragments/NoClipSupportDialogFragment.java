package com.stevenschoen.putionew.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.FrameLayout;

/*
 * Hack for the private NoSaveStateFrameLayout view that's wrapped around each
 * fragment by the support library. Doesn't happen with system fragments.
 */
public abstract class NoClipSupportDialogFragment extends DialogFragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        if (view instanceof FrameLayout) {
            ((FrameLayout) view).setClipChildren(false);
        }
    }
}