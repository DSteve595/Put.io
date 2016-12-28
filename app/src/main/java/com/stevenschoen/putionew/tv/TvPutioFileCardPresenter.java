/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.stevenschoen.putionew.tv;

import android.content.Context;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.files.PutioFile;

/*
 * A TvPutFileCardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class TvPutioFileCardPresenter extends Presenter {
    private static final String TAG = "TvPutFileCardPresenter";

    private static Context mContext;
    private static int CARD_WIDTH = 500;
    private static int CARD_HEIGHT = 176;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        mContext = parent.getContext();

        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        PutioFile file = (PutioFile) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        cardView.setTitleText(file.getName());
        //cardView.setContentText(PutioUtils.humanReadableByteCount(file.size, false));
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        ImageView mainImageView = cardView.getMainImageView();
        if (file.isFolder()) {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
            Picasso.with(mContext).load(R.drawable.ic_putio_folder_accent).into(mainImageView);
        } else {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_CROP);
            Picasso.with(mContext).load(file.getScreenshot()).into(mainImageView);
        }

        if (file.isAccessed()) {
            cardView.setBadgeImage(mContext.getResources().getDrawable(R.drawable.ic_fileinfo_accessed));
            // weird bug with fade mask, remove it for now
//            cardView.findViewById(android.support.v17.leanback.R.id.fade_mask).setVisibility(View.GONE);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
