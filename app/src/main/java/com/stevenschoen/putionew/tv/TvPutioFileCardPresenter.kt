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

package com.stevenschoen.putionew.tv

import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.Presenter
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile

class TvPutioFileCardPresenter : Presenter() {

    companion object {
        private val CARD_WIDTH = 500
        private val CARD_HEIGHT = 176
    }

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return Presenter.ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val file = item as PutioFile
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = file.name
        //cardView.setContentText(PutioUtils.humanReadableByteCount(file.size, false));
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        val mainImageView = cardView.mainImageView
        if (file.isFolder) {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)
            Picasso.with(mainImageView.context).cancelRequest(mainImageView)
            mainImageView.setImageResource(R.drawable.ic_putio_folder_accent)
        } else {
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_CROP)
            Picasso.with(mainImageView.context).load(file.screenshot).into(mainImageView)
        }

        if (file.isAccessed) {
            cardView.badgeImage = ContextCompat.getDrawable(cardView.context, R.drawable.ic_fileinfo_accessed)
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }
}
