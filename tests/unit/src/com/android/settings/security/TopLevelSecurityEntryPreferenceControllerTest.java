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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.SettingsActivity;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class TopLevelSecurityEntryPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "top_level_security";
    private static final String ALTERNATIVE_FRAGMENT_CLASSNAME = "AlternativeFragmentClassname";

    private TopLevelSecurityEntryPreferenceController mTopLevelSecurityEntryPreferenceController;
    private Preference mPreference;
    private FakeFeatureFactory mFeatureFactory;
    private SecuritySettingsFeatureProvider mSecuritySettingsFeatureProvider;

    @Mock
    private Context mContext;
    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSecuritySettingsFeatureProvider = mFeatureFactory.getSecuritySettingsFeatureProvider();
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;

        mPreference = new Preference(ApplicationProvider.getApplicationContext());
        mPreference.setKey(PREFERENCE_KEY);

        doNothing().when(mContext).startActivity(any(Intent.class));
        mTopLevelSecurityEntryPreferenceController =
                new TopLevelSecurityEntryPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void handlePreferenceTreeClick_forDifferentPreferenceKey_isNotHandled() {
        Preference preference = new Preference(ApplicationProvider.getApplicationContext());
        preference.setKey("some_other_preference");

        boolean preferenceHandled =
                mTopLevelSecurityEntryPreferenceController.handlePreferenceTreeClick(preference);

        assertThat(preferenceHandled).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_withAlternativeFragment_launchesAlternativeFragment() {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(true);
        when(mSecuritySettingsFeatureProvider.getAlternativeSecuritySettingsFragmentClassname())
                .thenReturn(ALTERNATIVE_FRAGMENT_CLASSNAME);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        boolean preferenceHandled =
                mTopLevelSecurityEntryPreferenceController.handlePreferenceTreeClick(mPreference);

        assertThat(preferenceHandled).isTrue();
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ALTERNATIVE_FRAGMENT_CLASSNAME);
    }

    @Test
    public void handlePreferenceTreeClick_withDisabledAlternative_isNotHandled() {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(false);
        when(mSecuritySettingsFeatureProvider.getAlternativeSecuritySettingsFragmentClassname())
                .thenReturn(ALTERNATIVE_FRAGMENT_CLASSNAME);

        boolean preferenceHandled =
                mTopLevelSecurityEntryPreferenceController.handlePreferenceTreeClick(mPreference);

        assertThat(preferenceHandled).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_withoutAlternativeFragmentName_isNotHandled() {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(true);
        when(mSecuritySettingsFeatureProvider.getAlternativeSecuritySettingsFragmentClassname())
                .thenReturn(null);

        boolean preferenceHandled =
                mTopLevelSecurityEntryPreferenceController.handlePreferenceTreeClick(mPreference);

        assertThat(preferenceHandled).isFalse();
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterEnabled_returnsUnavailable() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(true);

        assertThat(mTopLevelSecurityEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSecurityEntryPreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterDisabled_returnsAvailable() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(false);

        assertThat(mTopLevelSecurityEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSecurityEntryPreferenceController.AVAILABLE);
    }
}
