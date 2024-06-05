/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SmartBatterySettingsTest {
    private Context mContext;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private SmartBatterySettings mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPowerUsageFeatureProvider = FakeFeatureFactory.setupForTest().powerUsageFeatureProvider;
        mFragment = new SmartBatterySettings();
    }

    @Test
    public void isPageSearchEnabled_smartBatterySupported_returnTrue() {
        when(mPowerUsageFeatureProvider.isSmartBatterySupported()).thenReturn(true);
        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        assertIsPageSearchEnabled(true);
    }

    @Test
    public void isPageSearchEnabled_smartBatteryUnsupported_returnFalse() {
        when(mPowerUsageFeatureProvider.isSmartBatterySupported()).thenReturn(false);

        assertIsPageSearchEnabled(false);
    }

    private void assertIsPageSearchEnabled(boolean expectedResult) {
        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj =
                org.robolectric.util.ReflectionHelpers.callInstanceMethod(
                        provider, /*methodName=*/ "isPageSearchEnabled",
                        ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isEqualTo(expectedResult);
    }
}
