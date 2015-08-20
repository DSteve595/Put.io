package com.stevenschoen.putionew.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.FrameLayout;

import com.trello.rxlifecycle.components.support.RxDialogFragment;

/*
 * Hack for the private NoSaveStateFrameLayout view that's wrapped around each
 * fragment by the support library. Doesn't happen with system fragments.
 */
public abstract class NoClipSupportDialogFragment extends RxDialogFragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        if (view instanceof FrameLayout) {
            ((FrameLayout) view).setClipChildren(false);
        }
    }
}