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

import static com.android.settings.accessibility.FloatingMenuTransparencyPreferenceController.DEFAULT_TRANSPARENCY;
import static com.android.settings.accessibility.FloatingMenuTransparencyPreferenceController.MAXIMUM_TRANSPARENCY;
import static com.android.settings.accessibility.FloatingMenuTransparencyPreferenceController.PRECISION;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settings.widget.SeekBarPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link FloatingMenuTransparencyPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class})
public class FloatingMenuTransparencyPreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private ContentResolver mContentResolver;
    private FloatingMenuTransparencyPreferenceController mController;
    private SeekBarPreference mSeekBarPreference;

    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mController = new FloatingMenuTransparencyPreferenceController(mContext, "test_key");

        mSeekBarPreference = new SeekBarPreference(mContext);
        doReturn(mSeekBarPreference).when(mScreen).findPreference("test_key");
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
    public void displayPreference_floatingMenuMode_fadeEnabled_preferenceEnabled() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, /* ON */ 1);

        mController.displayPreference(mScreen);

        assertThat(mSeekBarPreference.isEnabled()).isTrue();
    }

    @Test
    public void displayPreference_floatingMenuMode_fadeDisabled_preferenceDisabled() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, /* OFF */ 0);

        mController.displayPreference(mScreen);

        assertThat(mSeekBarPreference.isEnabled()).isFalse();
    }

    @Test
    public void displayPreference_navigationBarMode_preferenceDisabled() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);

        mController.displayPreference(mScreen);

        assertThat(mSeekBarPreference.isEnabled()).isFalse();
    }

    @Test
    public void onChange_floatingMenuModeChangeToNavigationBar_preferenceDisabled() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, /* ON */ 1);
        mController.displayPreference(mScreen);

        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);
        mController.mContentObserver.onChange(false);

        assertThat(mSeekBarPreference.isEnabled()).isFalse();
    }

    @Test
    public void onChange_navigationBarModeChangeToFloatingMenu_preferenceDisabled() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, /* ON */ 1);
        mController.displayPreference(mScreen);

        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);
        mController.mContentObserver.onChange(false);

        assertThat(mSeekBarPreference.isEnabled()).isTrue();
    }

    @Test
    public void getSliderPosition_putNormalOpacityValue_expectedValue() {
        final float transparencyValue = 0.65f;
        Settings.Secure.putFloat(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY,
                (MAXIMUM_TRANSPARENCY - transparencyValue));

        assertThat(mController.getSliderPosition()).isEqualTo((int) (transparencyValue * 100));
    }

    @Test
    public void getSliderPosition_putOutOfBoundOpacityValue_defaultValue() {
        Settings.Secure.putFloat(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, 0.01f);

        final int defaultValue = Math.round(DEFAULT_TRANSPARENCY * PRECISION);
        assertThat(mController.getSliderPosition()).isEqualTo(defaultValue);
    }

    @Test
    public void setSliderPosition_expectedValue() {
        final float transparencyValue = 0.27f;
        mController.setSliderPosition((int) (transparencyValue * 100));

        final float value = Settings.Secure.getFloat(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, -1);
        assertThat(value).isEqualTo((MAXIMUM_TRANSPARENCY - transparencyValue));
    }

    @Test
    public void onResume_registerSpecificContentObserver() {
        mController.onResume();

        verify(mContentResolver).registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_BUTTON_MODE), false,
                mController.mContentObserver);
        verify(mContentResolver).registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED),
                false,
                mController.mContentObserver);
    }

    @Test
    public void onPause_unregisterContentObserver() {
        mController.onPause();

        verify(mContentResolver).unregisterContentObserver(mController.mContentObserver);
    }
}
