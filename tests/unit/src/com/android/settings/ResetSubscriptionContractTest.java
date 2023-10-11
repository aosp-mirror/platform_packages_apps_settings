/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ResetSubscriptionContractTest {

    private static final int SUB_ID_1 = 3;
    private static final int SUB_ID_2 = 8;

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;
    @Mock
    private SubscriptionInfo mSubscriptionInfo1;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;

    private Context mContext;
    private ResetNetworkRequest mRequestArgs;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mRequestArgs = new ResetNetworkRequest(new Bundle());
    }

    private ResetSubscriptionContract createTestObject() {
        return new ResetSubscriptionContract(mContext, mRequestArgs) {
            @Override
            protected SubscriptionManager getSubscriptionManager() {
                return mSubscriptionManager;
            }
            @Override
            protected OnSubscriptionsChangedListener getChangeListener() {
                return mOnSubscriptionsChangedListener;
            }
        };
    }

    @Test
    public void getAnyMissingSubscriptionId_returnNull_whenNoSubscriptionChange() {
        mRequestArgs.setResetTelephonyAndNetworkPolicyManager(SUB_ID_1);
        doReturn(mSubscriptionInfo1).when(mSubscriptionManager)
                .getActiveSubscriptionInfo(SUB_ID_1);
        mRequestArgs.setResetApn(SUB_ID_2);
        doReturn(mSubscriptionInfo2).when(mSubscriptionManager)
                .getActiveSubscriptionInfo(SUB_ID_2);

        ResetSubscriptionContract target = createTestObject();

        verify(mSubscriptionManager).addOnSubscriptionsChangedListener(any(), any());

        assertNull(target.getAnyMissingSubscriptionId());
    }

    @Test
    public void getAnyMissingSubscriptionId_returnSubId_whenSubscriptionNotActive() {
        mRequestArgs.setResetTelephonyAndNetworkPolicyManager(SUB_ID_1);
        doReturn(mSubscriptionInfo1).when(mSubscriptionManager)
                .getActiveSubscriptionInfo(SUB_ID_1);
        mRequestArgs.setResetApn(SUB_ID_2);
        doReturn(null).when(mSubscriptionManager)
                .getActiveSubscriptionInfo(SUB_ID_2);

        ResetSubscriptionContract target = createTestObject();

        verify(mSubscriptionManager).addOnSubscriptionsChangedListener(any(), any());

        assertEquals(target.getAnyMissingSubscriptionId(), new Integer(SUB_ID_2));
    }
}
