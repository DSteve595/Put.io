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

package com.google.sample.castcompanionlibrary.cast.callbacks;

import com.google.android.gms.common.ConnectionResult;
import com.google.sample.castcompanionlibrary.cast.exceptions.OnFailedListener;

import android.support.v7.media.MediaRouter.RouteInfo;

public interface IBaseCastConsumer extends OnFailedListener {

    /**
     * Called when connection is established
     */
    public void onConnected();

    /**
     * Called when the client is temporarily in a disconnected state. This can happen if there is a
     * problem with the remote service (e.g. a crash or resource problem causes it to be killed by
     * the system). When called, all requests have been canceled and no outstanding listeners will
     * be executed. Applications could disable UI components that require the service, and wait for
     * a call to onConnectivityRecovered() to re-enable them.
     * 
     * @param cause The reason for the disconnection. Defined by constants CAUSE_*.
     */
    public void onConnectionSuspended(int cause);

    /**
     * Called when a device is disconnected
     */
    public void onDisconnected();

    /**
     * Called when an error happens while connecting to a device. If this method returns
     * <code>true</code>, then the library will provide an error dialog to inform the user. Clients
     * can extend this method and return <code>false</code> to handle the error message themselves.
     * 
     * @param result
     * @return <code>true</code> if you want the library handle the error message
     */
    public boolean onConnectionFailed(ConnectionResult result);

    /**
     * Called when the MediaRouterCallback detects a non-default route.
     * 
     * @param info
     */
    public void onCastDeviceDetected(RouteInfo info);

    /**
     * Called after reconnection is established following a temporary disconnection, say, due to
     * network issues.
     */
    public void onConnectivityRecovered();
}
