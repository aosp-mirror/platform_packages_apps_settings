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

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_GESTURE;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Flags;
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

/** Tests for {@link AccessibilityButtonGesturePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonGesturePreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final Resources mResources = mContext.getResources();
    private final ContentResolver mContentResolver = mContext.getContentResolver();
    private final ListPreference mListPreference = new ListPreference(mContext);
    private AccessibilityButtonGesturePreferenceController mController;

    @Before
    public void setUp() {
        mController = new AccessibilityButtonGesturePreferenceController(mContext,
                "test_key");
        when(mContext.getResources()).thenReturn(mResources);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getAvailabilityStatus_navigationGestureEnabled_returnAvailable() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_GESTURAL);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void
            getAvailabilityStatus_navigationGestureEnabled_gestureFlag_conditionallyUnavailable() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_GESTURAL);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_navigationGestureDisabled_returnConditionallyUnavailable() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_2BUTTON);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_a11yBtnModeGesture_navigationBarValue() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_GESTURE);

        mController.updateState(mListPreference);

        final String gestureValue = String.valueOf(ACCESSIBILITY_BUTTON_MODE_GESTURE);
        assertThat(mListPreference.getValue()).isEqualTo(gestureValue);
    }
}
