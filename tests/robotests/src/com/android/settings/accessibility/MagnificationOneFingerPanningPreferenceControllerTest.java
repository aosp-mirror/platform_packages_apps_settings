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

import static com.android.server.accessibility.Flags.enableMagnificationOneFingerPanningGesture;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MagnificationOneFingerPanningPreferenceControllerTest {
    private static final String ONE_FINGER_PANNING_KEY =
            Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED;

    @Rule public final SetFlagsRule mSetFlagsRule =
            new SetFlagsRule(SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT);

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final SwitchPreference mSwitchPreference = spy(new SwitchPreference(mContext));
    private final MagnificationOneFingerPanningPreferenceController mController =
            new MagnificationOneFingerPanningPreferenceController(mContext);

    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mSwitchPreference.setKey(MagnificationOneFingerPanningPreferenceController.PREF_KEY);
        mScreen.addPreference(mSwitchPreference);
        mController.displayPreference(mScreen);
    }

    @After
    public void cleanup() {
        // Can't use resetToDefaults as it NPE with
        // "Cannot invoke "android.content.IContentProvider.call"
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                MagnificationOneFingerPanningPreferenceController.PREF_KEY,
                (mController.mDefaultValue) ? ON : OFF);
    }

    @Test
    public void displayPreference_defaultState_correctSummarySet() {
        assertThat(mSwitchPreference.getSummary())
                .isEqualTo(mContext.getString(
                        R.string.accessibility_magnification_one_finger_panning_summary_off));
    }

    @Test
    public void getAvailabilityStatus_defaultState_disabled() {
        int status = mController.getAvailabilityStatus();

        assertThat(status).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_featureFlagEnabled_enabled() {
        enableFlag();

        int status = mController.getAvailabilityStatus();

        assertThat(status).isEqualTo(AVAILABLE);
    }

    @Test
    public void isChecked_defaultState_returnFalse() {
        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingsEnabled_returnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, ON);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_settingsDisabled_returnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, OFF);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_enabled_enabledSummarySet() {
        mController.setChecked(true);

        assertThat(mSwitchPreference.getSummary()).isEqualTo(enabledSummary());
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_disabled_disabledSummarySet() {
        mController.setChecked(false);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.getSummary()).isEqualTo(disabledSummary());
    }

    @Test
    public void getSummary_disable_disableSummaryTextUsed() {
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, OFF);

        var summary = mController.getSummary();

        assertThat(summary).isEqualTo(disabledSummary());
    }

    @Test
    public void getSummary_enable_enabledSummaryTextUsed() {
        Settings.Secure.putInt(mContext.getContentResolver(), ONE_FINGER_PANNING_KEY, ON);

        var summary = mController.getSummary();

        assertThat(summary).isEqualTo(enabledSummary());
    }

    @Test
    public void performClick_switchDefaultState_shouldReturnTrue() {
        enableFlag();

        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(true);
        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    private void enableFlag() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE);
        assertThat(enableMagnificationOneFingerPanningGesture()).isTrue();
        // This ensures that preference change listeners are added correctly.
        mController.displayPreference(mScreen);
    }

    private String enabledSummary() {
        return mContext.getString(
                R.string.accessibility_magnification_one_finger_panning_summary_on);
    }

    private String disabledSummary() {
        return mContext.getString(
                R.string.accessibility_magnification_one_finger_panning_summary_off);
    }
}
