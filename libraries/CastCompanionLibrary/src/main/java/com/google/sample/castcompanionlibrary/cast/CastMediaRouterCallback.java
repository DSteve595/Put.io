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

package com.google.sample.castcompanionlibrary.cast;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGD;

import android.content.Context;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;

import com.google.android.gms.cast.CastDevice;
import com.google.sample.castcompanionlibrary.cast.BaseCastManager.ReconnectionStatus;
import com.google.sample.castcompanionlibrary.utils.LogUtils;
import com.google.sample.castcompanionlibrary.utils.Utils;

/**
 * Provides a handy implementation of {@link MediaRouter.Callback}. When a {@link RouteInfo} is
 * selected by user from the list of available routes, this class will call the
 * {@link DeviceSelectionListener#setDevice(CastDevice))} of the listener that was passed to it in
 * the constructor. In addition, as soon as a non-default route is discovered, the
 * {@link DeviceSelectionListener#onCastDeviceDetected(CastDevice))} is called.
 * <p>
 * There is also logic in this class to help with the process of previous session recovery.
 */
public class CastMediaRouterCallback extends MediaRouter.Callback {
    private static final String TAG = LogUtils.makeLogTag(CastMediaRouterCallback.class);
    private final DeviceSelectionListener selectDeviceInterface;
    private final Context mContext;

    public CastMediaRouterCallback(DeviceSelectionListener callback, Context context) {
        this.selectDeviceInterface = callback;
        this.mContext = context;
    }

    @Override
    public void onRouteSelected(MediaRouter router, RouteInfo info) {
        LOGD(TAG, "onRouteSelected: info=" + info);
        if (BaseCastManager.getCastManager().getReconnectionStatus()
                == BaseCastManager.ReconnectionStatus.FINALIZE) {
            BaseCastManager.getCastManager().setReconnectionStatus(ReconnectionStatus.INACTIVE);
            BaseCastManager.getCastManager().cancelReconnectionTask();
            return;
        }
        Utils.saveStringToPreference(mContext, BaseCastManager.PREFS_KEY_ROUTE_ID, info.getId());
        CastDevice device = CastDevice.getFromBundle(info.getExtras());
        selectDeviceInterface.onDeviceSelected(device);
        LOGD(TAG, "onResult: mSelectedDevice=" + device.getFriendlyName());
    }

    @Override
    public void onRouteUnselected(MediaRouter router, RouteInfo route) {
        LOGD(TAG, "onRouteUnselected: route=" + route);
        selectDeviceInterface.onDeviceSelected(null);
    }

    @Override
    public void onRouteAdded(MediaRouter router, RouteInfo route) {
        super.onRouteAdded(router, route);
        if (!router.getDefaultRoute().equals(route)) {
            selectDeviceInterface.onCastDeviceDetected(route);
        }
        if (BaseCastManager.getCastManager().getReconnectionStatus() == ReconnectionStatus.STARTED) {
            String routeId = Utils.getStringFromPreference(mContext,
                    BaseCastManager.PREFS_KEY_ROUTE_ID);
            if (route.getId().equals(routeId)) {
                // we found the route, so lets go with that
                LOGD(TAG, "onRouteAdded: Attempting to recover a session with info=" + route);
                BaseCastManager.getCastManager().setReconnectionStatus(
                        ReconnectionStatus.IN_PROGRESS);

                CastDevice device = CastDevice.getFromBundle(route.getExtras());
                LOGD(TAG, "onRouteAdded: Attempting to recover a session with device: "
                        + device.getFriendlyName());
                selectDeviceInterface.onDeviceSelected(device);
            }
        }
    }

}
