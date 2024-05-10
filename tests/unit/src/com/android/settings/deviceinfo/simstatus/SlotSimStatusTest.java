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

package com.android.settings.deviceinfo.simstatus;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class SlotSimStatusTest {

    private static final int SUB_ID_1 = 3;
    private static final int SUB_ID_2 = 8;

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo1;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;
    @Mock
    private Executor mExecutor;
    @Mock
    private Lifecycle mLifecycle;
    @Captor
    private ArgumentCaptor<Runnable> mRunnableCaptor;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mockService(Context.TELEPHONY_SERVICE, TelephonyManager.class, mTelephonyManager);
        mockService(Context.TELEPHONY_SUBSCRIPTION_SERVICE, SubscriptionManager.class,
                mSubscriptionManager);
    }

    @Test
    public void size_returnNumberOfPhone_whenQuery() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext, null, null) {
            @Override
            protected void postValue(Long value) {}
            @Override
            protected void setValue(Long value) {}
        };

        assertEquals(new Integer(target.size()), new Integer(2));
    }

    @Test
    public void size_returnNumberOfPhone_whenQueryInBackgroundThread() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext, mExecutor, mLifecycle) {
            @Override
            protected void postValue(Long value) {}
            @Override
            protected void setValue(Long value) {}
        };

        verify(mExecutor).execute(mRunnableCaptor.capture());
        mRunnableCaptor.getValue().run();

        assertEquals(new Integer(target.size()), new Integer(2));
    }

    @Test
    public void getPreferenceOrdering_returnOrdering_whenQuery() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext, null, null) {
            @Override
            protected void postValue(Long value) {}
            @Override
            protected void setValue(Long value) {}
        };
        target.setBasePreferenceOrdering(30);

        assertEquals(new Integer(target.getPreferenceOrdering(1)), new Integer(32));
    }

    @Test
    public void getPreferenceKey_returnKey_whenQuery() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext, null, null) {
            @Override
            protected void postValue(Long value) {}
            @Override
            protected void setValue(Long value) {}
        };
        target.setBasePreferenceOrdering(50);

        assertEquals(target.getPreferenceKey(1), "sim_status2");
    }

    @Test
    public void getSubscriptionInfo_returnSubscriptionInfo_whenActive() {
        doReturn(SUB_ID_1).when(mSubscriptionInfo1).getSubscriptionId();
        doReturn(0).when(mSubscriptionInfo1).getSimSlotIndex();
        doReturn(SUB_ID_2).when(mSubscriptionInfo2).getSubscriptionId();
        doReturn(1).when(mSubscriptionInfo2).getSimSlotIndex();

        doReturn(List.of(mSubscriptionInfo1, mSubscriptionInfo2))
                .when(mSubscriptionManager).getActiveSubscriptionInfoList();
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext, null, null) {
            @Override
            protected void postValue(Long value) {}
            @Override
            protected void setValue(Long value) {}
        };

        assertEquals(target.getSubscriptionInfo(1), mSubscriptionInfo2);
    }

    private <T> void mockService(String serviceName, Class<T> serviceClass, T service) {
        when(mContext.getSystemServiceName(serviceClass)).thenReturn(serviceName);
        when(mContext.getSystemService(serviceName)).thenReturn(service);
    }
}
