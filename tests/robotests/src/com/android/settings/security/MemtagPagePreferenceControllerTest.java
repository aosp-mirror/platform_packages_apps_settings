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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemProperties;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRestrictedLockUtilsInternal.class})
public class MemtagPagePreferenceControllerTest {
    private final String mMemtagSupportedProperty = "ro.arm64.memtag.bootctl_supported";

    private MemtagPagePreferenceController mController;
    private Context mContext;

    private static final String FRAGMENT_TAG = "memtag_page";

    @Before
    public void setUp() {
        ShadowSystemProperties.override(mMemtagSupportedProperty, "true");

        mContext = RuntimeEnvironment.application;
        mController = new MemtagPagePreferenceController(mContext, FRAGMENT_TAG);
    }

    @Test
    public void displayPreference_disabledByAdmin_disablesPreference() {
        ShadowRestrictedLockUtilsInternal.setMteIsDisabled(true);
        RestrictedPreference preference = new RestrictedPreference(mContext);
        preference.setKey(mController.getPreferenceKey());
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        screen.addPreference(preference);

        mController.displayPreference(screen);
        assertThat(preference.isDisabledByAdmin()).isTrue();
    }
}
