/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryStatsHelperLoaderTest {
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private ConnectivityManager mConnectivityManager;

    private Context mContext;
    private BatteryStatsHelperLoader mBatteryStatsHelperLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mConnectivityManager).when(mContext).getSystemService(
                Context.CONNECTIVITY_SERVICE);

        mBatteryStatsHelperLoader = spy(new BatteryStatsHelperLoader(mContext));
        mBatteryStatsHelperLoader.mBatteryUtils = mBatteryUtils;
    }

    @Test
    public void testLoadInBackground_loadWithoutBundle() {
        when(mBatteryStatsHelperLoader.getContext()).thenReturn(mContext);
        mBatteryStatsHelperLoader.loadInBackground();

        verify(mBatteryUtils).initBatteryStatsHelper(any(), eq(null), any());
    }
}
