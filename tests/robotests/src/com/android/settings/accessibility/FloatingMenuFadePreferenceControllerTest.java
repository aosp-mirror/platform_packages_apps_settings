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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link FloatingMenuFadePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class FloatingMenuFadePreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    private static final int OFF = 0;
    private static final int ON = 1;

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private ContentResolver mContentResolver;
    private final SwitchPreference mSwitchPreference = new SwitchPreference(mContext);
    private FloatingMenuFadePreferenceController mController;

    @Before
    public void setUp() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mController = new FloatingMenuFadePreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailabilityStatus_a11yBtnModeFloatingMenu_returnAvailable() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_a11yBtnModeNavigationBar_returnDisabledDependentSetting() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void updateState_keyFloatingMenuFadeDisabled_fadeIsDisabled() {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, OFF);

        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChange_floatingMenuFadeEnabled_keyFloatingMenuFadeIsOn() {
        mController.onPreferenceChange(mSwitchPreference, Boolean.TRUE);

        final int actualValue = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, OFF);
        assertThat(actualValue).isEqualTo(ON);
    }

    @Test
    public void onChange_floatingMenuFadeChangeToDisabled_preferenceDisabled() {
        mController.mPreference = mSwitchPreference;
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, OFF);

        mController.mContentObserver.onChange(false);

        assertThat(mController.mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onResume_registerSpecificContentObserver() {
        mController.onResume();

        verify(mContentResolver).registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_BUTTON_MODE), false,
                mController.mContentObserver);
    }

    @Test
    public void onPause_unregisterContentObserver() {
        mController.onPause();

        verify(mContentResolver).unregisterContentObserver(mController.mContentObserver);
    }
}
