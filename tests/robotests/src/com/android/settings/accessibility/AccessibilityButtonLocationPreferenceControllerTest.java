/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityButtonLocationPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonLocationPreferenceControllerTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final Resources mResources = mContext.getResources();
    private final ContentResolver mContentResolver = mContext.getContentResolver();
    private final ListPreference mListPreference = new ListPreference(mContext);
    private AccessibilityButtonLocationPreferenceController mController;


    @Before
    public void setUp() {
        mController = new AccessibilityButtonLocationPreferenceController(mContext,
                "test_key");
        when(mContext.getResources()).thenReturn(mResources);
    }

    @Test
    public void getAvailabilityStatus_navigationGestureEnabled_returnConditionallyUnavailable() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_GESTURAL);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_navigationGestureDisabled_returnAvailable() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_2BUTTON);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_a11yBtnModeNavigationBar_navigationBarValue() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);

        mController.updateState(mListPreference);

        final String navigationBarValue = String.valueOf(ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);
        assertThat(mListPreference.getValue()).isEqualTo(navigationBarValue);
    }

    @Test
    public void onPreferenceChange_a11yBtnModeFloatingMenu_floatingMenuValue() {
        final String floatingMenuValue = String.valueOf(ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);

        mController.onPreferenceChange(mListPreference, floatingMenuValue);

        assertThat(mListPreference.getValue()).isEqualTo(floatingMenuValue);
    }
}
