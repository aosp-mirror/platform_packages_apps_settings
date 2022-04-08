/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;

import com.android.settings.network.telephony.DataConnectivityListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DataConnectivityListenerTest {
    @Mock
    private DataConnectivityListener.Client mClient;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private Network mActiveNetwork;

    private Context mContext;
    private DataConnectivityListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mConnectivityManager.getActiveNetwork()).thenReturn(mActiveNetwork);
        mListener = new DataConnectivityListener(mContext, mClient);
    }

    @Test
    public void noStart_doesNotRegister() {
        verify(mConnectivityManager, never()).registerNetworkCallback(any(NetworkRequest.class),
                any(ConnectivityManager.NetworkCallback.class), any(Handler.class));
    }

    @Test
    public void start_doesRegister() {
        mListener.start();
        verify(mConnectivityManager).registerNetworkCallback(any(NetworkRequest.class),
                eq(mListener), any(Handler.class));
    }

    @Test
    public void onCapabilitiesChanged_notActiveNetwork_noCallback() {
        Network changedNetwork = mock(Network.class);
        mListener.onCapabilitiesChanged(changedNetwork, mock(NetworkCapabilities.class));
        verify(mClient, never()).onDataConnectivityChange();
    }

    @Test
    public void onCapabilitiesChanged_activeNetwork_onDataConnectivityChangeFires() {
        mListener.onCapabilitiesChanged(mActiveNetwork, mock(NetworkCapabilities.class));
        verify(mClient).onDataConnectivityChange();
    }

    @Test
    public void onLosing_notActiveNetwork_onDataConnectivityChangeFires() {
        Network changedNetwork = mock(Network.class);
        mListener.onLosing(changedNetwork, 500);
        verify(mClient).onDataConnectivityChange();
    }

    @Test
    public void onLosing_activeNetwork_onDataConnectivityChangeFires() {
        mListener.onLosing(mActiveNetwork, 500);
        verify(mClient).onDataConnectivityChange();
    }

    @Test
    public void onLost_notActiveNetwork_onDataConnectivityChangeFires() {
        Network changedNetwork = mock(Network.class);
        mListener.onLost(changedNetwork);
        verify(mClient).onDataConnectivityChange();
    }

    @Test
    public void onLost_activeNetwork_onDataConnectivityChangeFires() {
        mListener.onLost(mActiveNetwork);
        verify(mClient).onDataConnectivityChange();
    }
}
