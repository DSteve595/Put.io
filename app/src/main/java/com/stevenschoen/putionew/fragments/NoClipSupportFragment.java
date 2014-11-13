package com.stevenschoen.putionew.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.FrameLayout;

public abstract class NoClipSupportFragment extends Fragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        if (view instanceof FrameLayout) {
            ((FrameLayout) view).setClipChildren(false);
        }
    }
}