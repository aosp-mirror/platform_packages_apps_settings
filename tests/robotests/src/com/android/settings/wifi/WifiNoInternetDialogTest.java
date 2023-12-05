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

package com.android.settings.wifi;

import static android.net.ConnectivityManager.ACTION_PROMPT_LOST_VALIDATION;
import static android.net.ConnectivityManager.ACTION_PROMPT_PARTIAL_CONNECTIVITY;
import static android.net.ConnectivityManager.ACTION_PROMPT_UNVALIDATED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@Ignore("b/314867581")
@RunWith(RobolectricTestRunner.class)
public class WifiNoInternetDialogTest {

    private static final String FAKE_SSID = "fake_ssid";

    @Mock
    private Network mNetwork;

    @Captor
    private ArgumentCaptor<ConnectivityManager.NetworkCallback> mCallbackCaptor;

    @Mock
    private ConnectivityManager mConnectivityManager;

    @Mock
    private NetworkInfo mNetworkInfo;

    @Mock
    private NetworkCapabilities mNetworkCapabilities;

    private WifiNoInternetDialog mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void launchActivity_noIntentAction_shouldNotFatalException() {
        WifiNoInternetDialog wifiNoInternetDialog =
                Robolectric.setupActivity(WifiNoInternetDialog.class);
    }

    @Test
    public void setupPromptUnvalidated_shouldShowNoInternetAccessRemember() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();

        mActivity.onCreate(new Bundle());

        assertThat(mActivity.mAlwaysAllow.getText()).isEqualTo(
                mActivity.getString(R.string.no_internet_access_remember));
    }

    @Test
    public void setupPromptPartialConnectivity_shouldShowNoInternetAccessRemember() {
        setupActivityWithAction(ACTION_PROMPT_PARTIAL_CONNECTIVITY, mNetwork);
        setupNetworkComponents();

        mActivity.onCreate(new Bundle());

        assertThat(mActivity.mAlwaysAllow.getText()).isEqualTo(
                mActivity.getString(R.string.no_internet_access_remember));
    }

    @Test
    public void setupPromptLostValidationAction_shouldShowLostInternetAccessPersist() {
        setupActivityWithAction(ACTION_PROMPT_LOST_VALIDATION, mNetwork);
        setupNetworkComponents();

        mActivity.onCreate(new Bundle());

        assertThat(mActivity.mAlwaysAllow.getText()).isEqualTo(
                mActivity.getString(R.string.lost_internet_access_persist));
    }

    @Test
    public void clickPositiveButton_whenPromptUnvalidated_shouldCallSetAcceptUnvalidated() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();
        mActivity.onCreate(new Bundle());

        mActivity.onClick(null, DialogInterface.BUTTON_POSITIVE);

        verify(mConnectivityManager).setAcceptUnvalidated(any(Network.class), eq(true), eq(false));
    }

    @Test
    public void positiveButton_withPartialConnectivity_shouldCallSetAcceptPartialConnectivity() {
        setupActivityWithAction(ACTION_PROMPT_PARTIAL_CONNECTIVITY, mNetwork);
        setupNetworkComponents();
        mActivity.onCreate(new Bundle());

        mActivity.onClick(null, DialogInterface.BUTTON_POSITIVE);

        verify(mConnectivityManager).setAcceptPartialConnectivity(any(Network.class), eq(true),
                eq(false));
    }

    @Test
    public void positiveButton_withLostValidation_shouldCallSetAvoidUnvalidated() {
        setupActivityWithAction(ACTION_PROMPT_LOST_VALIDATION, mNetwork);
        setupNetworkComponents();
        mActivity.onCreate(new Bundle());

        mActivity.onClick(null, DialogInterface.BUTTON_POSITIVE);

        verify(mConnectivityManager).setAvoidUnvalidated(any(Network.class));
    }

    @Test
    public void destroyWithNoClick_inPartialConnectivity_shouldCallSetAcceptPartialConnectivity() {
        setupActivityWithAction(ACTION_PROMPT_PARTIAL_CONNECTIVITY, mNetwork);
        setupNetworkComponents();
        when(mActivity.isFinishing()).thenReturn(true);
        mActivity.onCreate(new Bundle());

        mActivity.onDestroy();

        verify(mConnectivityManager).setAcceptPartialConnectivity(any(Network.class), eq(false),
                eq(false));
    }

    @Test
    public void destroyWithNoClick_whenUnvalidated_shouldCallSetAcceptUnvalidated() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();
        when(mActivity.isFinishing()).thenReturn(true);
        mActivity.onCreate(new Bundle());

        mActivity.onDestroy();

        verify(mConnectivityManager).setAcceptUnvalidated(any(Network.class), eq(false), eq(false));
    }

    @Test
    public void networkCallbackOnLost_shouldFinish() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();
        mActivity.onCreate(new Bundle());
        verify(mConnectivityManager, times(1)).registerNetworkCallback(
                any(NetworkRequest.class), mCallbackCaptor.capture());

        mCallbackCaptor.getValue().onLost(mNetwork);

        verify(mActivity).finish();
    }

    @Test
    public void networkCallbackOnLost_shouldNotFinishIfNetworkIsNotTheSame() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();
        mActivity.onCreate(new Bundle());
        verify(mConnectivityManager, times(1)).registerNetworkCallback(
                any(NetworkRequest.class), mCallbackCaptor.capture());

        Network unexpectedNetwork = mock(Network.class);
        mCallbackCaptor.getValue().onLost(unexpectedNetwork);

        verify(mActivity, never()).finish();
    }

    @Test
    public void networkCallbackOnCapabilitiesChanged_shouldFinish() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();
        when(mNetworkCapabilities.hasCapability(NET_CAPABILITY_VALIDATED)).thenReturn(true);
        mActivity.onCreate(new Bundle());
        verify(mConnectivityManager, times(1)).registerNetworkCallback(
                any(NetworkRequest.class), mCallbackCaptor.capture());

        mCallbackCaptor.getValue().onCapabilitiesChanged(mNetwork, mNetworkCapabilities);

        verify(mActivity).finish();
    }

    @Test
    public void networkCallbackOnCapabilitiesChanged_shouldNotFinishIfNetworkIsNotTheSame() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();
        when(mNetworkCapabilities.hasCapability(NET_CAPABILITY_VALIDATED)).thenReturn(true);
        mActivity.onCreate(new Bundle());
        verify(mConnectivityManager, times(1)).registerNetworkCallback(
                any(NetworkRequest.class), mCallbackCaptor.capture());

        Network unexpectedNetwork = mock(Network.class);
        mCallbackCaptor.getValue().onCapabilitiesChanged(unexpectedNetwork, mNetworkCapabilities);

        verify(mActivity, never()).finish();
    }

    @Test
    public void networkNotConnectedOrConnecting_shouldFinish() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, mNetwork);
        setupNetworkComponents();
        when(mNetworkInfo.isConnectedOrConnecting()).thenReturn(false);

        mActivity.onCreate(new Bundle());

        verify(mActivity).finish();
    }

    @Test
    public void withNullNetwork_shouldFinish() {
        setupActivityWithAction(ACTION_PROMPT_UNVALIDATED, null);
        setupNetworkComponents();

        mActivity.onCreate(new Bundle());

        verify(mActivity).finish();
    }

    private void setupNetworkComponents() {
        when(mActivity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mNetworkInfo.isConnectedOrConnecting()).thenReturn(true);
        when(mNetworkCapabilities.getSsid()).thenReturn(FAKE_SSID);
        when(mConnectivityManager.getNetworkInfo(any()))
                .thenReturn(mNetworkInfo);
        when(mConnectivityManager.getNetworkCapabilities(any()))
                .thenReturn(mNetworkCapabilities);
    }

    private void setupActivityWithAction(String action, Network network) {
        final Intent intent = new Intent(action).setClassName(
                RuntimeEnvironment.application.getPackageName(),
                WifiNoInternetDialog.class.getName());
        intent.putExtra(ConnectivityManager.EXTRA_NETWORK, network);
        mActivity = spy(Robolectric.buildActivity(WifiNoInternetDialog.class, intent).get());
    }
}