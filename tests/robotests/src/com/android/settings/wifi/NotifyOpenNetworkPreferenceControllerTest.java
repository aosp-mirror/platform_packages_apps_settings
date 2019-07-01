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

package com.android.settings.wifi;

import static android.provider.Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NotifyOpenNetworkPreferenceControllerTest {

    private Context mContext;
    private NotifyOpenNetworksPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new NotifyOpenNetworksPreferenceController(mContext, mock(Lifecycle.class));
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_nonMatchingKey_shouldDoNothing() {
        final SwitchPreference pref = new SwitchPreference(mContext);

        assertThat(mController.handlePreferenceTreeClick(pref)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_nonMatchingType_shouldDoNothing() {
        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.handlePreferenceTreeClick(pref)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_matchingKeyAndType_shouldUpdateSetting() {
        final SwitchPreference pref = new SwitchPreference(mContext);
        pref.setChecked(true);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.handlePreferenceTreeClick(pref)).isTrue();
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0))
                .isEqualTo(1);
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingsAreEnabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 1);

        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingsAreDisabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putInt(mContext.getContentResolver(),
                WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0);

        mController.updateState(preference);

        verify(preference).setChecked(false);
    }
}
