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
import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.CastOptions.Builder;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.sample.castcompanionlibrary.cast.callbacks.DataCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.callbacks.IDataCastConsumer;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.castcompanionlibrary.utils.LogUtils;
import com.google.sample.castcompanionlibrary.utils.Utils;

import android.content.Context;
import android.support.v7.app.MediaRouteDialogFactory;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.TextUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A concrete subclass of {@link BaseCastManager} that is suitable for data-centric applications
 * that use multiple namespaces.
 * <p>
 * This is a singleton that needs to be "initialized" (by calling <code>initialize()</code>) prior
 * to usage. Subsequent to initialization, an easier way to get access to the singleton class is to
 * call a variant of <code>getInstance()</code>. After initialization, callers can enable any
 * available feature (all features are off by default). To do so, call <code>enableFeature()</code>
 * and pass an OR-ed expression built from one ore more of the following constants:
 * <p>
 * <ul>
 * <li>FEATURE_DEBUGGING: to enable GMS level logging</li>
 * </ul>
 * Beyond managing the connectivity to a cast device, this class provides easy-to-use methods to
 * send and receive messages using one or more namspaces. These namespaces can be configured during
 * the initialization as part of the call to <code>initialize()</code> or can be added later on.
 * Clients can subclass this class to extend the features and functionality beyond what this class
 * provides. This class manages various states of the remote cast device. Client applications,
 * however, can complement the default behavior of this class by hooking into various callbacks that
 * it provides (see {@link IDataCastConsumer}). Since the number of these callbacks is usually much
 * larger than what a single application might be interested in, there is a no-op implementation of
 * this interface (see {@link DataCastConsumerImpl}) that applications can subclass to override only
 * those methods that they are interested in. Since this library depends on the cast functionalities
 * provided by the Google Play services, the library checks to ensure that the right version of that
 * service is installed. It also provides a simple static method
 * <code>checkGooglePlaySevices()</code> that clients can call at an early stage of their
 * applications to provide a dialog for users if they need to update/activate their GMS library. To
 * learn more about this library, please read the documentation that is distributed as part of this
 * library.
 */
public class DataCastManager extends BaseCastManager
        implements Cast.MessageReceivedCallback {

    private static final String TAG = LogUtils.makeLogTag(DataCastManager.class);
    private static DataCastManager sInstance;
    private final Set<String> mNamespaceList = new HashSet<String>();
    protected Set<IDataCastConsumer> mDataConsumers;

    /**
     * Initializes the DataCastManager for clients. Before clients can use DataCastManager, they
     * need to initialize it by calling this static method. Then clients can obtain an instance of
     * this singleton class by calling {@link DataCastManager#getInstance()}. Failing to initialize
     * this class before requesting an instance will result in a {@link CastException} exception.
     *
     * @param context
     * @param applicationId the unique ID for your application
     * @param namespaces to be set up for this class.
     * @return
     */
    public static DataCastManager initialize(Context context,
            String applicationId, String... namespaces) {
        if (null == sInstance) {
            LOGD(TAG, "New instance of DataCastManager is created");
            if (ConnectionResult.SUCCESS != GooglePlayServicesUtil
                    .isGooglePlayServicesAvailable(context)) {
                String msg = "Couldn't find the appropriate version of Goolge Play Services";
                LOGE(TAG, msg);
                throw new RuntimeException(msg);
            }
            sInstance = new DataCastManager(context, applicationId, namespaces);
            mCastManager = sInstance;
        }
        return sInstance;
    }

    protected DataCastManager(Context context, String applicationId, String... namespaces) {
        super(context, applicationId);
        mDataConsumers = new HashSet<IDataCastConsumer>();
        if (null != namespaces) {
            for (String namespace : namespaces) {
                mNamespaceList.add(namespace);
            }
        }
    }

    /**
     * Returns the initialized instance of this class. If it is not initialized yet, a
     * {@link CastException} will be thrown.
     *
     * @see initialze()
     * @return
     * @throws CastException
     */
    public static DataCastManager getInstance() throws CastException {
        if (null == sInstance) {
            LOGE(TAG, "No DataCastManager instance was initialized, you need to " +
                    "call initialize() first");
            throw new CastException();
        }
        return sInstance;
    }

    /**
     * Returns the initialized instance of this class. If it is not initialized yet, a
     * {@link CastException} will be thrown. The {@link Context} that is passed as the argument will
     * be used to update the context. The main purpose of updating context is to enable the library
     * to provide {@link Context} related functionalities, e.g. it can create an error dialog if
     * needed. This method is preferred over the similar one without a context argument.
     *
     * @see {@link initialize()}, {@link setContext()}
     * @param ctx the current Context
     * @return
     * @throws CastException
     */
    public static DataCastManager getInstance(Context ctx) throws CastException {
        if (null == sInstance) {
            LOGE(TAG, "No DataCastManager instance was initialized, you need to " +
                    "call initialize() first");
            throw new CastException();
        }
        LOGD(TAG, "Updated context to: " + ctx.getClass().getName());
        sInstance.mContext = ctx;
        return sInstance;
    }

    /**
     * Adds a channel with the given <code>namespace</code> and registers {@link DataCastManager} as
     * the callback receiver. If the namespace is already registered, this returns
     * <code>false</code>, otherwise returns <code>true
     * </code>.
     *
     * @param namespace
     * @return
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     * @throws IllegalArgumentException If namespace is null or empty
     */
    public boolean addNamespace(String namespace) throws IllegalStateException, IOException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (TextUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        if (mNamespaceList.contains(namespace)) {
            LOGD(TAG, "Ignoring to add a namespace that is already added.");
            return false;
        }
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, namespace, this);
            mNamespaceList.add(namespace);
            return true;
        } catch (IOException e) {
            LOGE(TAG, "Failed to add namespace", e);
        } catch (IllegalStateException e) {
            LOGE(TAG, "Failed to add namespace", e);
        }
        return false;
    }

    /**
     * Unregisters a namespace. If namespace is not already registered, it returns
     * <code>false</code>, otherwise a successful removal returns <code>true
     * </code>.
     *
     * @param namespace
     * @return
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     * @throws IllegalArgumentException If namespace is null or empty
     */
    public boolean removeNamespace(String namespace) throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        if (TextUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        if (!mNamespaceList.contains(namespace)) {
            LOGD(TAG, "Ignoring to remove a namespace that is not registered.");
            return false;
        }
        try {
            Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, namespace);
            mNamespaceList.remove(namespace);
            return true;
        } catch (IOException e) {
            LOGE(TAG, "Failed to remove namespace: " + namespace, e);
        } catch (IllegalStateException e) {
            LOGE(TAG, "Failed to remove namespace: " + namespace, e);
        }
        return false;

    }

    /**
     * Sends the <code>message</code> on the data channel for the <code>namespace</code>. If fails,
     * it will call <code>onMessageSendFailed</code>
     *
     * @param message
     * @param namespace
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     *             possibly transient loss of network
     * @throws IllegalArgumentException If the the message is null, empty, or too long; or if the
     *             namespace is null or too long.
     * @throws IllegalStateException If there is no active service connection.
     * @throws IOException
     */
    public void sendDataMessage(String message, String namespace)
            throws IllegalArgumentException, IllegalStateException, IOException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (TextUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        Cast.CastApi.sendMessage(mApiClient, namespace, message).
                setResultCallback(new ResultCallback<Status>() {

                    @Override
                    public void onResult(Status result) {
                        if (!result.isSuccess()) {
                            DataCastManager.this.onMessageSendFailed(result);
                        }
                    }
                });
    }

    /*************************************************************************/
    /************** BaseCastManager methods **********************************/
    /*************************************************************************/

    @Override
    protected void onDeviceUnselected() {
        try {
            detachDataChannels();
        } catch (NoConnectionException e) {
            LOGE(TAG, "Failed to detach data channels", e);
        } catch (TransientNetworkDisconnectionException e) {
            LOGE(TAG, "Failed to detach data channels", e);
        }
    }

    @Override
    protected Builder getCastOptionBuilder(CastDevice device) {

        Builder builder = Cast.CastOptions.builder(
                mSelectedCastDevice, new CastListener());
        if (isFeatureEnabled(FEATURE_DEBUGGING)) {
            builder.setDebuggingEnabled();
        }
        return builder;
    }

    class CastListener extends Cast.Listener {

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationDisconnected (int)
         */
        @Override
        public void onApplicationDisconnected(int statusCode) {
            DataCastManager.this.onApplicationDisconnected(statusCode);
        }

        /*
         * (non-Javadoc)
         * @see com.google.android.gms.cast.Cast.Listener#onApplicationStatusChanged ()
         */
        @Override
        public void onApplicationStatusChanged() {
            DataCastManager.this.onApplicationStatusChanged();
        }
    }

    @Override
    protected MediaRouteDialogFactory getMediaRouteDialogFactory() {
        return null;
    }

    /*************************************************************************/
    /************** Cast.Listener callbacks **********************************/
    /*************************************************************************/

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String applicationStatus,
            String sessionId, boolean wasLaunched) {
        LOGD(TAG, "onApplicationConnected() reached with sessionId: " + sessionId);

        // saving session for future retrieval; we only save the last session
        // info
        Utils.saveStringToPreference(mContext, PREFS_KEY_SESSION_ID, sessionId);
        if (mReconnectionStatus == ReconnectionStatus.IN_PROGRESS) {
            // we have tried to reconnect and successfully launched the app, so
            // it is time to select the route and make the cast icon happy :-)
            List<RouteInfo> routes = mMediaRouter.getRoutes();
            if (null != routes) {
                String routeId = Utils.getStringFromPreference(mContext, PREFS_KEY_ROUTE_ID);
                boolean found = false;
                for (RouteInfo routeInfo : routes) {
                    if (routeId.equals(routeInfo.getId())) {
                        // found the right route
                        LOGD(TAG, "Found the correct route during reconnection attempt");
                        found = true;
                        mReconnectionStatus = ReconnectionStatus.FINALIZE;
                        mMediaRouter.selectRoute(routeInfo);
                        break;
                    }
                }
                if (!found) {
                    // we were hoping to have the route that we wanted, but we
                    // didn't so we deselect the device
                    onDeviceSelected(null);
                    mReconnectionStatus = ReconnectionStatus.INACTIVE;
                    // uncomment the following if you want to clear session
                    // persisted data if a reconnection attempt fails
                    // Utils.saveStringToPreference(mContext,
                    // PREFS_KEY_SESSION_ID, null);
                    // Utils.saveStringToPreference(mContext,
                    // PREFS_KEY_ROUTE_ID, null);
                    return;
                }
            }
        }
        // registering namespaces, if any
        try {
            attachDataChannels();
            for (IDataCastConsumer consumer : mDataConsumers) {
                try {
                    consumer.onApplicationConnected(appMetadata, applicationStatus, sessionId,
                            wasLaunched);
                } catch (Exception e) {
                    LOGE(TAG, "onApplicationConnected(): Failed to inform " + consumer, e);
                }
            }
        } catch (IllegalStateException e) {
            LOGE(TAG, "Failed to attach namespaces", e);
        } catch (IOException e) {
            LOGE(TAG, "Failed to attach namespaces", e);
        } catch (TransientNetworkDisconnectionException e) {
            LOGE(TAG, "Failed to attach namespaces", e);
        } catch (NoConnectionException e) {
            LOGE(TAG, "Failed to attach namespaces", e);
        }

    }

    /*
     * Adds namespaces for data channel(s)
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     * possibly transient loss of network
     * @throws IOException If an I/O error occurs while performing the request.
     * @throws IllegalStateException Thrown when the controller is not connected to a CastDevice.
     * @throws IllegalArgumentException If namespace is null.
     */
    private void attachDataChannels() throws IllegalStateException, IOException,
            TransientNetworkDisconnectionException, NoConnectionException {
        checkConnectivity();
        if (!mNamespaceList.isEmpty() && null != Cast.CastApi) {
            for (String namespace : mNamespaceList) {
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient, namespace, this);
            }
        }
    }

    /*
     * Remove namespaces
     * @throws NoConnectionException If no connectivity to the device exists
     * @throws TransientNetworkDisconnectionException If framework is still trying to recover from a
     * possibly transient loss of network
     */
    private void detachDataChannels() throws TransientNetworkDisconnectionException,
            NoConnectionException {
        checkConnectivity();
        if (!mNamespaceList.isEmpty()) {
            for (String namespace : mNamespaceList) {
                try {
                    Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, namespace);
                } catch (IllegalStateException e) {
                    LOGE(TAG, "Failed to add namespace: " + namespace, e);
                } catch (IOException e) {
                    LOGE(TAG, "Failed to add namespace: " + namespace, e);
                }
            }
        }
    }

    @Override
    public void onApplicationConnectionFailed(int errorCode) {
        onDeviceSelected(null);
        for (IDataCastConsumer consumer : mDataConsumers) {
            try {
                consumer.onApplicationConnectionFailed(errorCode);
            } catch (Exception e) {
                LOGE(TAG, "onApplicationConnectionFailed(): Failed to inform " + consumer, e);
            }
        }

    }

    public void onApplicationDisconnected(int errorCode) {
        for (IDataCastConsumer consumer : mDataConsumers) {
            try {
                consumer.onApplicationDisconnected(errorCode);
            } catch (Exception e) {
                LOGE(TAG, "onApplicationDisconnected(): Failed to inform " + consumer, e);
            }
        }
        if (null != mMediaRouter) {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
        onDeviceSelected(null);

    }

    public void onApplicationStatusChanged() {
        String appStatus = null;
        if (!isConnected()) {
            return;
        }
        try {
            appStatus = Cast.CastApi.getApplicationStatus(mApiClient);
            LOGD(TAG, "onApplicationStatusChanged() reached: "
                    + Cast.CastApi.getApplicationStatus(mApiClient));

            for (IDataCastConsumer consumer : mDataConsumers) {
                try {
                    consumer.onApplicationStatusChanged(appStatus);
                } catch (Exception e) {
                    LOGE(TAG, "onApplicationStatusChanged(): Failed to inform " + consumer, e);
                }
            }
        } catch (IllegalStateException e) {
            LOGE(TAG, "onApplicationStatusChanged(): Failed", e);
        }

    }

    @Override
    public void onApplicationStopFailed(int errorCode) {
        for (IDataCastConsumer consumer : mDataConsumers) {
            try {
                consumer.onApplicationStopFailed(errorCode);
            } catch (Exception e) {
                LOGE(TAG, "onApplicationStopFailed(): Failed to inform " + consumer, e);
            }
        }

    }

    public void onVolumeChanged() {
        // nothing relevant to data
    }

    /*************************************************************************/
    /************** MessageReceivedCallbacks callbacks ***********************/
    /*************************************************************************/

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
        for (IDataCastConsumer consumer : mDataConsumers) {
            try {
                consumer.onMessageReceived(castDevice, namespace, message);
            } catch (Exception e) {
                LOGE(TAG, "onMessageReceived(): Failed to inform " + consumer, e);
            }
        }

    }

    public void onMessageSendFailed(Status result) {
        for (IDataCastConsumer consumer : mDataConsumers) {
            try {
                consumer.onMessageSendFailed(result);
            } catch (Exception e) {
                LOGE(TAG, "onMessageSendFailed(): Failed to inform " + consumer, e);
            }
        }
    }

    /*************************************************************/
    /***** Registering IDataCastConsumer listeners ***************/
    /*************************************************************/
    /**
     * Registers an {@link IDataCastConsumer} interface with this class. Registered listeners will
     * be notified of changes to a variety of lifecycle and status changes through the callbacks
     * that the interface provides.
     *
     * @see DataCastConsumerImpl
     * @param listener
     */
    public synchronized void addDataCastConsumer(IDataCastConsumer listener) {
        if (null != listener) {
            super.addBaseCastConsumer(listener);
            boolean result = mDataConsumers.add(listener);
            if (result) {
                LOGD(TAG, "Successfully added the new DataCastConsumer listener " + listener);
            } else {
                LOGD(TAG, "Adding Listener " + listener + " was already registered, " +
                        "skipping this step");
            }
        }
    }

    /**
     * Unregisters an {@link IDataCastConsumer}.
     *
     * @param listener
     */
    public synchronized void removeDataCastConsumer(IDataCastConsumer listener) {
        if (null != listener) {
            super.removeBaseCastConsumer(listener);
            mDataConsumers.remove(listener);
        }
    }

}
