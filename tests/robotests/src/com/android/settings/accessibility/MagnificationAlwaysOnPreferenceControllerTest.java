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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
public class MagnificationAlwaysOnPreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String KEY_ALWAYS_ON =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED;

    private Context mContext;
    private ShadowContentResolver mShadowContentResolver;
    private SwitchPreference mSwitchPreference;
    private MagnificationAlwaysOnPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mShadowContentResolver = Shadow.extract(mContext.getContentResolver());

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mSwitchPreference = spy(new SwitchPreference(mContext));
        mSwitchPreference.setKey(MagnificationAlwaysOnPreferenceController.PREF_KEY);
        screen.addPreference(mSwitchPreference);

        mController = new MagnificationAlwaysOnPreferenceController(mContext,
                MagnificationAlwaysOnPreferenceController.PREF_KEY);
        mController.displayPreference(screen);
        mController.updateState(mSwitchPreference);

        reset(mSwitchPreference);
    }

    @Test
    public void performClick_switchDefaultStateForAlwaysOn_shouldReturnFalse() {
        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(false);
        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_disableAlwaysOn_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_ALWAYS_ON, OFF);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(false);
        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIDE_MAGNIFICATION_ALWAYS_ON_TOGGLE_WHEN_WINDOW_MODE_ONLY)
    public void onResume_flagOn_verifyRegisterCapabilityObserver() {
        mController.onResume();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY)))
                .hasSize(1);
    }

    @Test
    @EnableFlags(Flags.FLAG_HIDE_MAGNIFICATION_ALWAYS_ON_TOGGLE_WHEN_WINDOW_MODE_ONLY)
    public void onPause_flagOn_verifyUnregisterCapabilityObserver() {
        mController.onResume();
        mController.onPause();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY)))
                .isEmpty();
    }

    @Test
    @DisableFlags(Flags.FLAG_HIDE_MAGNIFICATION_ALWAYS_ON_TOGGLE_WHEN_WINDOW_MODE_ONLY)
    public void updateState_windowModeOnlyAndFlagOff_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.WINDOW);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_HIDE_MAGNIFICATION_ALWAYS_ON_TOGGLE_WHEN_WINDOW_MODE_ONLY)
    public void updateState_windowModeOnlyAndFlagOn_preferenceBecomesUnavailable() {
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
}
