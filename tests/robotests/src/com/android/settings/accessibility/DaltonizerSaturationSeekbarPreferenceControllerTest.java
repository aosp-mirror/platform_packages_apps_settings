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

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Looper;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link DaltonizerSaturationSeekbarPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class DaltonizerSaturationSeekbarPreferenceControllerTest {

    private ContentResolver mContentResolver;
    private DaltonizerSaturationSeekbarPreferenceController mController;

    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    private SeekBarPreference mPreference;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mContentResolver = context.getContentResolver();

        mPreference = new SeekBarPreference(context);
        mPreference.setKey(ToggleDaltonizerPreferenceFragment.KEY_SATURATION);
        mScreen = new PreferenceManager(context).createPreferenceScreen(context);
        mScreen.addPreference(mPreference);

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new DaltonizerSaturationSeekbarPreferenceController(
                context,
                ToggleDaltonizerPreferenceFragment.KEY_SATURATION);
    }

    @After
    public void cleanup() {
        Settings.Secure.putString(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                null);
        Settings.Secure.putString(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                null);
        Settings.Secure.putString(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                null);
    }

    @Test
    public void constructor_defaultValuesMatch() {
        assertThat(mController.getSliderPosition()).isEqualTo(7);
        assertThat(mController.getMax()).isEqualTo(10);
        assertThat(mController.getMin()).isEqualTo(1);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagDisabled_unavailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_defaultSettings_unavailable() {
        // By default enabled == false.
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_enabledDefaultDisplayMode_available() {
        setDaltonizerEnabled(1);

        // By default display mode is deuteranomaly.
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagEnabledProtanEnabled_available() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 11);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagEnabledDeutranEnabled_available() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 12);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagEnabledTritanEnabled_available() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 13);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagEnabledGrayScale_disabled() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 0);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagEnabledColorCorrectionDisabled_disabled() {
        setDaltonizerMode(/* enabled= */ 0, /* mode= */ 11);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagEnabledColorCorrectionDisabledGrayScale_disabled() {
        setDaltonizerMode(/* enabled= */ 0, /* mode= */ 0);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void displayPreference_flagEnabledColorCorrectionEnabled_enabledWithDefaultValues() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 11);
        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getMax()).isEqualTo(10);
        assertThat(mPreference.getMin()).isEqualTo(1);
        assertThat(mPreference.getProgress()).isEqualTo(7);
        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getOnPreferenceChangeListener()).isEqualTo(mController);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void displayPreference_flagEnabledColorCorrectionDisabled_disabledWithDefaultValues() {
        setDaltonizerMode(/* enabled= */ 0, /* mode= */ 11);
        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getMax()).isEqualTo(10);
        assertThat(mPreference.getMin()).isEqualTo(1);
        assertThat(mPreference.getProgress()).isEqualTo(7);
        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getOnPreferenceChangeListener()).isEqualTo(mController);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void displayPreference_disabled_notVisible() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mPreference.getOnPreferenceChangeListener()).isNull();
    }

    @Test
    public void setSliderPosition_inRange_secureSettingsUpdated() {
        var isSliderSet = mController.setSliderPosition(9);

        assertThat(isSliderSet).isTrue();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(9);
    }

    @Test
    public void setSliderPosition_min_secureSettingsUpdated() {
        var isSliderSet = mController.setSliderPosition(1);

        assertThat(isSliderSet).isTrue();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(1);
    }

    @Test
    public void setSliderPosition_max_secureSettingsUpdated() {
        var isSliderSet = mController.setSliderPosition(10);

        assertThat(isSliderSet).isTrue();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(10);
    }

    @Test
    public void setSliderPosition_tooLarge_secureSettingsNotUpdated() {
        var isSliderSet = mController.setSliderPosition(11);

        assertThat(isSliderSet).isFalse();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(7);
    }

    @Test
    public void setSliderPosition_tooSmall_secureSettingsNotUpdated() {
        var isSliderSet = mController.setSliderPosition(-1);

        assertThat(isSliderSet).isFalse();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(7);
    }

    @Test
    public void updateState_enabledProtan_preferenceEnabled() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 11);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_enabledDeuteran_preferenceEnabled() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 12);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_enabledTritan_preferenceEnabled() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 13);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_disabledGrayScale_preferenceDisabled() {
        setDaltonizerMode(/* enabled= */ 0, /* mode= */ 0);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_nullPreference_noError() {
        setDaltonizerMode(/* enabled= */ 0, /* mode= */ 0);

        mController.updateState(null);
    }

    @Test
    public void updateState_enabledGrayScale_preferenceDisabled() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 0);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onResume_daltonizerEnabledAfterResumed_preferenceEnabled() {
        setDaltonizerMode(/* enabled= */ 0, /* mode= */ 11);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isEnabled()).isFalse();

        mLifecycle.addObserver(mController);
        mLifecycle.handleLifecycleEvent(ON_RESUME);

        setDaltonizerEnabled(1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onResume_daltonizerDisabledAfterResumed_preferenceDisabled() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 11);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isEnabled()).isTrue();

        mLifecycle.addObserver(mController);
        mLifecycle.handleLifecycleEvent(ON_RESUME);

        setDaltonizerEnabled(0);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onResume_daltonizerGrayScaledAfterResumed_preferenceDisabled() {
        setDaltonizerMode(/* enabled= */ 1, /* mode= */ 11);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isEnabled()).isTrue();

        mLifecycle.addObserver(mController);
        mLifecycle.handleLifecycleEvent(ON_RESUME);

        setDaltonizerDisplay(0);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onStop_daltonizerEnabledAfterOnStop_preferenceNotChanged() {
        setDaltonizerMode(/* enabled= */ 0, /* mode= */ 11);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isEnabled()).isFalse();

        mLifecycle.addObserver(mController);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        // enabled.
        setDaltonizerEnabled(1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    private void setDaltonizerMode(int enabled, int mode) {
        setDaltonizerEnabled(enabled);
        setDaltonizerDisplay(mode);
    }

    private void setDaltonizerEnabled(int enabled) {
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                enabled);
    }

    private void setDaltonizerDisplay(int mode) {
        Settings.Secure.putString(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                Integer.toString(mode));
    }
}
