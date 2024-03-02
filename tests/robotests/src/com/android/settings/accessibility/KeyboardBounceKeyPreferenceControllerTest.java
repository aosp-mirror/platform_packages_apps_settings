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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


@RunWith(RobolectricTestRunner.class)
public class KeyboardBounceKeyPreferenceControllerTest {

    private static final String KEY_ACCESSIBILITY_BOUNCE_KEYS =
            Settings.Secure.ACCESSIBILITY_BOUNCE_KEYS;
    private static final int UNKNOWN = -1;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final SwitchPreference mSwitchPreference = spy(new SwitchPreference(mContext));
    private final KeyboardBounceKeyPreferenceController mController =
            new KeyboardBounceKeyPreferenceController(mContext,
                    KeyboardBounceKeyPreferenceController.PREF_KEY);

    @Before
    public void setUp() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mSwitchPreference.setKey(KeyboardBounceKeyPreferenceController.PREF_KEY);
        screen.addPreference(mSwitchPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_disableBounceKey_onResumeShouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_ACCESSIBILITY_BOUNCE_KEYS, OFF);

        mController.updateState(mSwitchPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enableBounceKey_onResumeShouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_ACCESSIBILITY_BOUNCE_KEYS, ON);

        mController.updateState(mSwitchPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void performClick_enableBounceKey_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_ACCESSIBILITY_BOUNCE_KEYS, OFF);

        mController.updateState(mSwitchPreference);

        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(true);
        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void performClick_disableBounceKey_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_ACCESSIBILITY_BOUNCE_KEYS, ON);

        mController.updateState(mSwitchPreference);

        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(false);
        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setFalse_shouldDisableBounceKey() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(
                mContext.getContentResolver(), KEY_ACCESSIBILITY_BOUNCE_KEYS, UNKNOWN)).isEqualTo(
                OFF);
    }

    @Test
    public void setChecked_setTrue_shouldEnableBounceKey() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(
                mContext.getContentResolver(), KEY_ACCESSIBILITY_BOUNCE_KEYS,
                UNKNOWN)).isNotEqualTo(OFF);
    }
}
