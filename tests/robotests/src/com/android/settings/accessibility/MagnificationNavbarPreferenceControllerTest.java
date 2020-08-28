/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.settings.accessibility.MagnificationPreferenceFragment.OFF;
import static com.android.settings.accessibility.MagnificationPreferenceFragment.ON;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
public class MagnificationNavbarPreferenceControllerTest {

    private Context mContext;
    private MagnificationNavbarPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new MagnificationNavbarPreferenceController(mContext, "test_key");
        mPreference = new Preference(mContext);
        mController.updateState(mPreference);
    }

    @After
    public void tearDown() {
        ShadowMagnificationPreferenceFragment.reset();
    }

    @Test
    @Config(shadows = ShadowMagnificationPreferenceFragment.class)
    public void isAvailable_unsupported_shouldNotBeAvailable() {
        ShadowMagnificationPreferenceFragment.setApplicable(false);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(AVAILABLE);
    }

    @Test
    @Config(shadows = ShadowMagnificationPreferenceFragment.class)
    public void isAvailable_supported_shouldBeAvailable() {
        ShadowMagnificationPreferenceFragment.setApplicable(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_shouldRefreshSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, ON);
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_on));

        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_off));
    }

    @Test
    public void updateState_shouldRefreshSummarySuw() {
        mController.setIsFromSUW(true);
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.
                        accessibility_screen_magnification_navbar_short_summary));
    }

    @Test
    public void isChecked_enabled() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, ON);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabled() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, OFF);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_enabled() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, -1))
                .isEqualTo(ON);
    }

    @Test
    public void setChecked_disabled() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, -1))
                .isEqualTo(OFF);
    }

    @Implements(MagnificationPreferenceFragment.class)
    public static class ShadowMagnificationPreferenceFragment {
        private static boolean sIsApplicable;

        @Resetter
        static void reset() {
            sIsApplicable = false;
        }

        @Implementation
        protected static boolean isApplicable(Resources res) {
            return sIsApplicable;
        }

        static void setApplicable(boolean applicable) {
            sIsApplicable = applicable;
        }
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final MagnificationNavbarPreferenceController controller =
                new MagnificationNavbarPreferenceController(mContext,
                        "screen_magnification_navbar_preference_screen");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final MagnificationNavbarPreferenceController controller =
                new MagnificationNavbarPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
