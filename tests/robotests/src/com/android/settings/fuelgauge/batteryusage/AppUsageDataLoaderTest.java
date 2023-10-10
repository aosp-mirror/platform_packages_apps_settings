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

package com.android.settings.fuelgauge.batteryusage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class AppUsageDataLoaderTest {
    private Context mContext;
    @Mock
    private ContentResolver mMockContentResolver;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mMockContentResolver).when(mContext).getContentResolver();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(new Intent()).when(mContext).registerReceiver(any(), any());
    }

    @Test
    public void loadAppUsageData_withData_insertFakeDataIntoProvider() {
        final List<AppUsageEvent> AppUsageEventList = new ArrayList<>();
        final AppUsageEvent appUsageEvent = AppUsageEvent.newBuilder().setUid(0).build();
        AppUsageEventList.add(appUsageEvent);
        AppUsageDataLoader.sFakeAppUsageEventsSupplier = () -> new HashMap<>();
        AppUsageDataLoader.sFakeUsageEventsListSupplier = () -> AppUsageEventList;

        AppUsageDataLoader.loadAppUsageData(mContext);

        verify(mMockContentResolver).bulkInsert(any(), any());
        verify(mMockContentResolver).notifyChange(any(), any());
    }

    @Test
    public void loadAppUsageData_nullAppUsageEvents_notInsertDataIntoProvider() {
        AppUsageDataLoader.sFakeAppUsageEventsSupplier = () -> null;

        AppUsageDataLoader.loadAppUsageData(mContext);

        verifyNoMoreInteractions(mMockContentResolver);
    }

    @Test
    public void loadAppUsageData_nullUsageEventsList_notInsertDataIntoProvider() {
        AppUsageDataLoader.sFakeAppUsageEventsSupplier = () -> new HashMap<>();
        AppUsageDataLoader.sFakeUsageEventsListSupplier = () -> null;

        AppUsageDataLoader.loadAppUsageData(mContext);

        verifyNoMoreInteractions(mMockContentResolver);
    }

    @Test
    public void loadAppUsageData_emptyUsageEventsList_notInsertDataIntoProvider() {
        AppUsageDataLoader.sFakeAppUsageEventsSupplier = () -> new HashMap<>();
        AppUsageDataLoader.sFakeUsageEventsListSupplier = () -> new ArrayList<>();

        AppUsageDataLoader.loadAppUsageData(mContext);

        verifyNoMoreInteractions(mMockContentResolver);
    }
}
