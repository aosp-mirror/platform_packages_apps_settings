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

package com.android.settings.safetycenter;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class TopLevelSafetyCenterEntryPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "top_level_safety_center";

    private TopLevelSafetyCenterEntryPreferenceController
            mTopLevelSafetyCenterEntryPreferenceController;
    private Preference mPreference;

    @Mock
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPreference = new Preference(ApplicationProvider.getApplicationContext());
        mPreference.setKey(PREFERENCE_KEY);

        doNothing().when(mContext).startActivity(any(Intent.class));
        mTopLevelSafetyCenterEntryPreferenceController =
                new TopLevelSafetyCenterEntryPreferenceController(mContext, PREFERENCE_KEY);
        DeviceConfig.resetToDefaults(Settings.RESET_MODE_PACKAGE_DEFAULTS,
                DeviceConfig.NAMESPACE_PRIVACY);
    }

    @After
    public void tearDown() {
        DeviceConfig.resetToDefaults(Settings.RESET_MODE_PACKAGE_DEFAULTS,
                DeviceConfig.NAMESPACE_PRIVACY);
    }

    @Test
    public void handlePreferenceTreeClick_forDifferentPreferenceKey_isNotHandled() {
        Preference preference = new Preference(ApplicationProvider.getApplicationContext());
        preference.setKey("some_other_preference");

        boolean preferenceHandled =
                mTopLevelSafetyCenterEntryPreferenceController.handlePreferenceTreeClick(
                        preference);

        assertThat(preferenceHandled).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_launchesIntendedIntent() {
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        boolean preferenceHandled = mTopLevelSafetyCenterEntryPreferenceController
                .handlePreferenceTreeClick(mPreference);

        assertThat(preferenceHandled).isTrue();
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Intent.ACTION_SAFETY_CENTER);
    }

    @Test
    public void handlePreferenceTreeClick_onStartActivityThrows_returnsFalse() {
        doThrow(ActivityNotFoundException.class)
                .when(mContext).startActivity(any(Intent.class));

        boolean preferenceHandled = mTopLevelSafetyCenterEntryPreferenceController
                .handlePreferenceTreeClick(mPreference);

        assertThat(preferenceHandled).isFalse();
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterDisabled_returnsUnavailable() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_PRIVACY,
                SafetyCenterStatus.SAFETY_CENTER_IS_ENABLED,
                /* value = */ Boolean.toString(false),
                /* makeDefault = */ false);

        assertThat(mTopLevelSafetyCenterEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSafetyCenterEntryPreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterEnabled_returnsAvailable() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_PRIVACY,
                SafetyCenterStatus.SAFETY_CENTER_IS_ENABLED,
                /* value = */ Boolean.toString(true),
                /* makeDefault = */ false);

        assertThat(mTopLevelSafetyCenterEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSafetyCenterEntryPreferenceController.AVAILABLE);
    }
}
