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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class SlotSimStatusTest {

    @Mock
    private TelephonyManager mTelephonyManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mockService(Context.TELEPHONY_SERVICE, TelephonyManager.class, mTelephonyManager);
    }

    @Test
    public void size_returnNumberOfPhone_whenQuery() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext);

        assertEquals(new Integer(target.size()), new Integer(2));
    }

    @Test
    public void size_returnNumberOfPhone_whenQueryInBackgroundThread() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        SlotSimStatus target = new SlotSimStatus(mContext, executor);

        assertEquals(new Integer(target.size()), new Integer(2));
    }

    @Test
    public void getPreferenceOrdering_returnOrdering_whenQuery() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext);
        target.setBasePreferenceOrdering(30);

        assertEquals(new Integer(target.getPreferenceOrdering(1)), new Integer(32));
    }

    @Test
    public void getPreferenceKey_returnKey_whenQuery() {
        doReturn(2).when(mTelephonyManager).getPhoneCount();

        SlotSimStatus target = new SlotSimStatus(mContext);
        target.setBasePreferenceOrdering(50);

        assertEquals(target.getPreferenceKey(1), "sim_status52");
    }

    private <T> void mockService(String serviceName, Class<T> serviceClass, T service) {
        when(mContext.getSystemServiceName(serviceClass)).thenReturn(serviceName);
        when(mContext.getSystemService(serviceName)).thenReturn(service);
    }
}
