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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivitySettingsManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class IngressRateLimitPreferenceControllerTest {
    private Context mContext = RuntimeEnvironment.application;
    private ListPreference mPreference;
    private IngressRateLimitPreferenceController mController;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPreference = new ListPreference(mContext);
        mPreference.setEntries(R.array.ingress_rate_limit_entries);
        mPreference.setEntryValues(R.array.ingress_rate_limit_values);

        mController = new IngressRateLimitPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_select5Mbits_shouldEnableIngressRateLimit() {
        final long newRateLimit = 625000; // 5mbit == 625000 B/s
        assertThat(mController.onPreferenceChange(mPreference, newRateLimit)).isTrue();

        final long configuredRateLimit =
                ConnectivitySettingsManager.getIngressRateLimitInBytesPerSecond(mContext);
        assertThat(configuredRateLimit).isEqualTo(newRateLimit);
    }

    @Test
    public void onPreferenceChanged_selectDisabled_shouldDisableIngressRateLimit() {
        final long disabledRateLimit = -1; // -1 == disabled
        assertThat(mController.onPreferenceChange(mPreference, disabledRateLimit)).isTrue();

        final long configuredRateLimit =
                ConnectivitySettingsManager.getIngressRateLimitInBytesPerSecond(mContext);
        assertThat(configuredRateLimit).isEqualTo(disabledRateLimit);
    }

    @Test
    public void onPreferenceChanged_invalidValue_returnsFalse() {
        final long invalidRateLimit = -123;
        assertThat(mController.onPreferenceChange(mPreference, invalidRateLimit)).isFalse();
    }

    @Test
    public void updateState_preferenceShouldBeSelected() {
        final long newRateLimit = 625000; // 5mbit == 625000 B/s
        ConnectivitySettingsManager.setIngressRateLimitInBytesPerSecond(mContext, newRateLimit);
        mController.updateState(mPreference);
        assertThat(Long.parseLong(mPreference.getValue())).isEqualTo(newRateLimit);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        final long newRateLimit = 625000; // 5mbit == 625000 B/s
        ConnectivitySettingsManager.setIngressRateLimitInBytesPerSecond(mContext, newRateLimit);
        mController.updateState(mPreference);

        mController.onDeveloperOptionsSwitchDisabled();
        assertThat(Long.parseLong(mPreference.getValue())).isEqualTo(-1);
        assertThat(ConnectivitySettingsManager.getIngressRateLimitInBytesPerSecond(
                mContext)).isEqualTo(-1);
    }
}
