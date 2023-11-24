/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.deviceinfo.batteryinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.util.ReflectionHelpers;

@RunWith(org.robolectric.RobolectricTestRunner.class)
public class BatteryInfoFragmentTest {
    private Context mContext;
    private FakeFeatureFactory mFactory;
    private BatteryInfoFragment mFragment;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFactory = FakeFeatureFactory.setupForTest();
        mFragment = new BatteryInfoFragment();
    }

    @Test
    public void isPageSearchEnabled_batteryInfoEnabled_returnTrue() {
        when(mFactory.batterySettingsFeatureProvider.isBatteryInfoEnabled(mContext))
                .thenReturn(true);

        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj =
                org.robolectric.util.ReflectionHelpers.callInstanceMethod(
                        provider, /*methodName=*/ "isPageSearchEnabled",
                        ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void isPageSearchEnabled_batteryInfoDisabled_returnFalse() {
        when(mFactory.batterySettingsFeatureProvider.isBatteryInfoEnabled(mContext))
                .thenReturn(false);

        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj =
                org.robolectric.util.ReflectionHelpers.callInstanceMethod(
                        provider, /*methodName=*/ "isPageSearchEnabled",
                        ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isFalse();
    }
}
