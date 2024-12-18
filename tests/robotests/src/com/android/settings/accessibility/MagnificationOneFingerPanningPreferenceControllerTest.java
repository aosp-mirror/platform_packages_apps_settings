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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
public class MagnificationOneFingerPanningPreferenceControllerTest {
    private static final String ONE_FINGER_PANNING_KEY =
            Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ShadowContentResolver mShadowContentResolver;
    private final SwitchPreference mSwitchPreference = spy(new SwitchPreference(mContext));
    private final MagnificationOneFingerPanningPreferenceController mController =
            new MagnificationOneFingerPanningPreferenceController(mContext);

    @Before
    public void setUp() {
        mShadowContentResolver = Shadow.extract(mContext.getContentResolver());

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mSwitchPreference.setKey(MagnificationOneFingerPanningPreferenceController.PREF_KEY);
        screen.addPreference(mSwitchPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void onResume_verifyRegisterCapabilityObserver() {
        mController.onResume();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY)))
                .hasSize(1);
    }

    @Test
    public void onPause_verifyUnregisterCapabilityObserver() {
        mController.onResume();
        mController.onPause();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY)))
                .isEmpty();
    }

    @Test
    public void updateState_windowModeOnly_preferenceIsUnavailable() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.WINDOW);
        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_fullscreenModeOnly_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.FULLSCREEN);
        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_switchMode_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);
        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    public void isChecked_defaultState_returnFalse() {
        mController.updateState(mSwitchPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingsOn_returnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, ON);
        mController.updateState(mSwitchPreference);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_settingsOff_returnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, OFF);
        mController.updateState(mSwitchPreference);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void getSummary_switchModeAndSettingsOff_defaultSummaryTextUsed() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, OFF);

        mController.updateState(mSwitchPreference);

        assertThat(mController.getSummary().toString()).isEqualTo(defaultSummary());
    }

    @Test
    public void getSummary_switchModeAndSettingsOn_defaultSummaryTextUsed() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, ON);

        mController.updateState(mSwitchPreference);

        assertThat(mController.getSummary().toString()).isEqualTo(defaultSummary());
    }

    @Test
    public void getSummary_windowModeOnly_unavailableSummaryTextUsed() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.WINDOW);

        mController.updateState(mSwitchPreference);

        assertThat(mController.getSummary().toString()).isEqualTo(unavailableSummary());
    }

    @Test
    public void performClick_defaultSettings_toggleOn() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);
        mController.updateState(mSwitchPreference);
        reset(mSwitchPreference);

        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(true);
        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void performClick_settingsOn_toggleOff() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, ON);
        mController.updateState(mSwitchPreference);
        reset(mSwitchPreference);

        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(false);
        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void getAvailableStatus_notInSetupWizard_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailableStatus_inSetupWizard_returnConditionallyUnavailable() {
        mController.setInSetupWizard(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    private String defaultSummary() {
        return mContext.getString(
                R.string.accessibility_magnification_one_finger_panning_summary);
    }

    private String unavailableSummary() {
        return mContext.getString(
                R.string.accessibility_magnification_one_finger_panning_summary_unavailable);
    }
}
