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
package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDashboardFragmentTest {
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    private static final String SLICE_ACTION = "com.android.settings.SEARCH_RESULT_TRAMPOLINE";
    private static final String TEST_APP_NAME = "com.testapp.settings";
    private static final String TEST_ACTION = "com.testapp.settings.ACTION_START";

    private BluetoothDashboardFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new BluetoothDashboardFragment();
    }


    @Test
    public void isAlwaysDiscoverable_callingAppIsNotFromSystemApp_returnsFalse() {
        assertThat(mFragment.isAlwaysDiscoverable(TEST_APP_NAME, TEST_ACTION)).isFalse();
    }

    @Test
    public void isAlwaysDiscoverable_callingAppIsFromSettings_returnsTrue() {
        assertThat(mFragment.isAlwaysDiscoverable(SETTINGS_PACKAGE_NAME, TEST_ACTION)).isTrue();
    }

    @Test
    public void isAlwaysDiscoverable_callingAppIsFromSystemUI_returnsTrue() {
        assertThat(mFragment.isAlwaysDiscoverable(SYSTEMUI_PACKAGE_NAME, TEST_ACTION)).isTrue();
    }

    @Test
    public void isAlwaysDiscoverable_actionIsFromSlice_returnsFalse() {
        assertThat(mFragment.isAlwaysDiscoverable(SYSTEMUI_PACKAGE_NAME, SLICE_ACTION)).isFalse();
    }
}
