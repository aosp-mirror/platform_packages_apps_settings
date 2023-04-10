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

package com.android.settings.development;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemProperties;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRestrictedLockUtilsInternal.class})
public class DevelopmentMemtagPagePreferenceControllerTest {
    private final String mMemtagSupportedProperty = "ro.arm64.memtag.bootctl_supported";

    private DevelopmentMemtagPagePreferenceController mController;
    private Context mContext;

    @Mock private DevelopmentSettingsDashboardFragment mFragment;
    private static final String FRAGMENT_TAG = "memtag_page";

    @Before
    public void setUp() {
        ShadowSystemProperties.override(mMemtagSupportedProperty, "true");

        mContext = RuntimeEnvironment.application;
        mController = new DevelopmentMemtagPagePreferenceController(mContext, mFragment);
    }

    @Test
    public void onAvailable_sysPropEnabled() {
        SystemProperties.set("ro.arm64.memtag.bootctl_supported", "1");
        assertTrue(mController.isAvailable());
    }

    @Test
    public void onAvailable_sysPropDisabled() {
        SystemProperties.set("ro.arm64.memtag.bootctl_supported", "0");
        assertFalse(mController.isAvailable());
    }
}
