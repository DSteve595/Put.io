/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.google.sample.castcompanionlibrary.widgets;

import com.google.android.gms.cast.MediaStatus;
import com.google.sample.castcompanionlibrary.widgets.MiniController.OnMiniControllerChangedListener;

import android.net.Uri;

/**
 * An interface to abstract {@link MiniController} so that other components can also control the
 * MiniControllers. Clients should code against this interface when they want to control the
 * provided {@link MiniController} or other custom implementations.
 */
public interface IMiniController {

    /**
     * Sets the uri for the album art
     * 
     * @param uri
     */
    public void setIcon(Uri uri);

    /**
     * Sets the title
     * 
     * @param title
     */
    public void setTitle(String title);

    /**
     * Sets the subtitle
     * 
     * @param subTitle
     */
    public void setSubTitle(String subTitle);

    /**
     * Sets the state of the play/pause button. Values that can be passed to this method are from
     * {@link MediaStatus}
     * 
     * @param state
     */
    public void setPlaybackStatus(int state);

    /**
     * Sets whether this component should be visible or hidden.
     * 
     * @param visibility
     */
    public void setVisibility(int visibility);

    /**
     * Returns the visibility state of this widget
     * 
     * @return
     */
    public boolean isVisible();

    /**
     * Assigns a {@link OnMiniControllerChangedListener} listener to be notified of the changes in
     * the mini controller
     * 
     * @param listener
     */
    public void setOnMiniControllerChangedListener(OnMiniControllerChangedListener listener);

}
