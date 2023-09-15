/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.HandlerExecutor;
import android.telephony.RadioAccessFamily;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AllowedNetworkTypesListenerTest {

    private static final int SUB_ID = 1;

    private Context mContext;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;

    @Mock
    private AllowedNetworkTypesListener.OnAllowedNetworkTypesListener mListener;
    @Mock
    private TelephonyManager mTelephonyManager;


    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mAllowedNetworkTypesListener =
                spy(new AllowedNetworkTypesListener(mContext.getMainExecutor()));
    }

    @Test
    public void onChange_userReasonChanged_shouldCallListener() {
        mAllowedNetworkTypesListener.mListener = mListener;
        long networkType = (long) RadioAccessFamily.getRafFromNetworkType(
                TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO);

        mAllowedNetworkTypesListener.onAllowedNetworkTypesChanged(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER, networkType);

        verify(mListener).onAllowedNetworkTypesChanged();
    }

    @Test
    public void onChange_carrierReasonChanged_shouldCallListener() {
        mAllowedNetworkTypesListener.mListener = mListener;
        long networkType = (long) RadioAccessFamily.getRafFromNetworkType(
                TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO);

        mAllowedNetworkTypesListener.onAllowedNetworkTypesChanged(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER, networkType);

        verify(mListener).onAllowedNetworkTypesChanged();
    }

    @Test
    public void register_shouldRegisterContentObserver() {
        mAllowedNetworkTypesListener.register(mContext, SUB_ID);

        verify(mTelephonyManager, times(1)).registerTelephonyCallback(any(HandlerExecutor.class),
                any(TelephonyCallback.class));
    }

    @Test
    public void unregister_shouldUnregisterContentObserver() {
        mAllowedNetworkTypesListener.unregister(mContext, SUB_ID);

        verify(mTelephonyManager).unregisterTelephonyCallback(
                mAllowedNetworkTypesListener);
    }
}
