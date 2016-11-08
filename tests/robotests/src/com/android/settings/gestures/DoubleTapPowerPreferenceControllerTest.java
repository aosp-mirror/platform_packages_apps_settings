/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DoubleTapPowerPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    private DoubleTapPowerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DoubleTapPowerPreferenceController(mContext);
    }

    @Test
    public void display_configIsTrue_shouldDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(true);
        mController.displayPreference(mScreen);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void display_configIsFalse_shouldNotDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(false);
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mock(Preference.class));

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        // Mock a TwoStatePreference
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        // Set the setting to be enabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0);

        // Run through updateState
        mController = new DoubleTapPowerPreferenceController(context);
        mController.updateState(preference);

        // Verify pref is checked (as setting is enabled).
        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        // Mock a TwoStatePreference
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        // Set the setting to be disabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 1);

        // Run through updateState
        mController = new DoubleTapPowerPreferenceController(context);
        mController.updateState(preference);

        // Verify pref is unchecked (as setting is disabled).
        verify(preference).setChecked(false);
    }

    @Test
    public void updateState_notTwoStatePreference_setSummary() {
        // Mock a regular preference
        final Preference preference = mock(Preference.class);
        // Set the setting to be disabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 1);

        // Run through updateState
        mController = new DoubleTapPowerPreferenceController(context);
        mController.updateState(preference);

        // Verify summary is set to off (as setting is disabled).
        verify(preference).setSummary(R.string.gesture_setting_off);
    }

}
