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

package com.android.settings.gestures;

import static android.app.contextualsearch.ContextualSearchManager.FEATURE_CONTEXTUAL_SEARCH;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDeviceConfig.class)
public class NavigationSettingsContextualSearchControllerTest {

    private static final String KEY_PRESS_HOLD_FOR_SEARCH = "search_gesture_press_hold";

    private NavigationSettingsContextualSearchController mController;
    private Context mContext;
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mPackageManager = mock(PackageManager.class);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new NavigationSettingsContextualSearchController(
                mContext, KEY_PRESS_HOLD_FOR_SEARCH);
    }

    @Test
    public void isAvailable_hasContextualSearchSystemFeature_shouldReturnTrue() {
        when(mPackageManager.hasSystemFeature(FEATURE_CONTEXTUAL_SEARCH)).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_doesNotHaveContextualSearchSystemFeature_shouldReturnFalse() {
        when(mPackageManager.hasSystemFeature(FEATURE_CONTEXTUAL_SEARCH)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_noDefault_true() {
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_valueFalse_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_valueTrue_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChange_preferenceChecked_valueTrue() {
        mController.onPreferenceChange(null, true);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, -1)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_preferenceUnchecked_valueFalse() {
        mController.onPreferenceChange(null, false);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, -1)).isEqualTo(0);
    }
}
