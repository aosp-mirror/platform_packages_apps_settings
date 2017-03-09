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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryOptimizationPreferenceControllerTest {
    private static final String KEY_OPTIMIZATION = "battery_optimization";
    private static final String KEY_OTHER = "other";
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private Fragment mFragment;
    @Mock
    private Preference mPreference;

    private BatteryOptimizationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new BatteryOptimizationPreferenceController(mSettingsActivity, mFragment);
    }

    @Test
    public void testHandlePreferenceTreeClick_OptimizationPreference_HandleClick() {
        when(mPreference.getKey()).thenReturn(KEY_OPTIMIZATION);

        final boolean handled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(handled).isTrue();
        verify(mSettingsActivity).startPreferencePanel(any(Fragment.class),
                anyString(), any(Bundle.class), anyInt(), any(CharSequence.class),
                any(Fragment.class), anyInt());
    }

    @Test
    public void testHandlePreferenceTreeClick_OtherPreference_NotHandleClick() {
        when(mPreference.getKey()).thenReturn(KEY_OTHER);

        final boolean handled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(handled).isFalse();
        verify(mSettingsActivity, never()).startPreferencePanel(any(Fragment.class),
                anyString(), any(Bundle.class), anyInt(), any(CharSequence.class),
                any(Fragment.class), anyInt());
    }
}
