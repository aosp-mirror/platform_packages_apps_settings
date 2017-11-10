/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class LocationModeRadioButtonPreferenceControllerTest {

    @Mock
    private RadioButtonPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private LocationModeRadioButtonPreferenceController mController;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mController = new LocationModeRadioButtonPreferenceControllerTestable(mContext, mLifecycle);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void displayPreference_shouldAddClickListener() {
        mController.displayPreference(mScreen);

        verify(mPreference).setOnClickListener(mController);
    }

    @Test
    public void onRadioButtonClicked_shouldSetLocationModeToOwnMode() {
        mController.displayPreference(mScreen);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        mController.onRadioButtonClicked(mPreference);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF))
                .isEqualTo(mController.getLocationMode());
    }

    @Test
    public void onLocationModeChanged_otherModeSelected_shouldUncheckPreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onLocationModeChanged_ownModeSelected_shouldCheckPreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(mController.getLocationMode(), false);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void onLocationModeChanged_locationOff_shouldDisablePreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_OFF, false);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onLocationModeChanged_locationOn_shouldDisablePreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mPreference).setEnabled(true);
    }

    private class LocationModeRadioButtonPreferenceControllerTestable
            extends LocationModeRadioButtonPreferenceController {

        public LocationModeRadioButtonPreferenceControllerTestable(Context context,
                Lifecycle lifecycle) {
            super(context, lifecycle);
        }

        @Override
        public String getPreferenceKey() {
            return "test";
        }

        @Override
        protected int getLocationMode() {
            return Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
        }
    }
}
