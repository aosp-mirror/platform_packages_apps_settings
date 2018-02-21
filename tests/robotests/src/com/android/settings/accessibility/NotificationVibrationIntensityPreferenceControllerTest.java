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

import static android.provider.Settings.System.NOTIFICATION_VIBRATION_INTENSITY;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NotificationVibrationIntensityPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private Context mContext;
    private NotificationVibrationIntensityPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = RuntimeEnvironment.application;
        mController = new NotificationVibrationIntensityPreferenceController(mContext) {
            @Override
            protected int getDefaultIntensity() {
                return 10;
            }
        };
        mLifecycle.addObserver(mController);
        mPreference = new Preference(mContext);
        mPreference.setSummary("Test");
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void verifyConstants() {
        assertThat(mController.getPreferenceKey())
                .isEqualTo(NotificationVibrationIntensityPreferenceController.PREF_KEY);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_shouldRefreshSummary() {
        Settings.System.putInt(mContext.getContentResolver(),
                NOTIFICATION_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW);
        mController.updateState(null);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.accessibility_vibration_intensity_low));

        Settings.System.putInt(mContext.getContentResolver(),
                NOTIFICATION_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_HIGH);
        mController.updateState(null);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.accessibility_vibration_intensity_high));

        Settings.System.putInt(mContext.getContentResolver(),
                NOTIFICATION_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        mController.updateState(null);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.accessibility_vibration_intensity_medium));

        Settings.System.putInt(mContext.getContentResolver(),
                NOTIFICATION_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        mController.updateState(null);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.accessibility_vibration_intensity_off));
    }
}
