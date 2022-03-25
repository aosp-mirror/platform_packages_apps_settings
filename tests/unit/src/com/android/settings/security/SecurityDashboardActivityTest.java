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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.Settings;
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
public class SecurityDashboardActivityTest {
    private static final String ALTERNATIVE_FRAGMENT_CLASSNAME = "AlternativeFragmentClassname";
    private static final String DEFAULT_FRAGMENT_CLASSNAME = "DefaultFragmentClassname";

    private SecuritySettingsFeatureProvider mSecuritySettingsFeatureProvider;
    private Settings.SecurityDashboardActivity mActivity;
    private Intent mDefaultIntent;

    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSecuritySettingsFeatureProvider = mFeatureFactory.getSecuritySettingsFeatureProvider();
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;

        mDefaultIntent = new Intent();
        mDefaultIntent.setAction(android.provider.Settings.ACTION_SECURITY_SETTINGS);
        mDefaultIntent.setClass(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                Settings.SecurityDashboardActivity.class);
        mDefaultIntent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT_CLASSNAME);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mActivity =
                        spy((Settings.SecurityDashboardActivity) InstrumentationRegistry
                                .getInstrumentation().newActivity(
                                        getClass().getClassLoader(),
                                        Settings.SecurityDashboardActivity.class.getName(),
                                        mDefaultIntent));
            } catch (Exception e) {
                throw new RuntimeException(e); // nothing to do
            }
        });
        doNothing().when(mActivity).startActivity(any(Intent.class));
    }

    @Test
    public void noAvailableAlternativeFragmentAvailable_defaultFragmentSet() {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(false);

        assertThat(mActivity.getInitialFragmentName(mDefaultIntent))
                .isEqualTo(DEFAULT_FRAGMENT_CLASSNAME);
    }

    @Test
    public void alternativeFragmentAvailable_alternativeFragmentSet() {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(true);
        when(mSecuritySettingsFeatureProvider.getAlternativeSecuritySettingsFragmentClassname())
                .thenReturn(ALTERNATIVE_FRAGMENT_CLASSNAME);

        assertThat(mActivity.getInitialFragmentName(mDefaultIntent))
                .isEqualTo(ALTERNATIVE_FRAGMENT_CLASSNAME);
    }

    @Test
    public void noAvailableAlternativeFragmentAvailable_alternativeFragmentNotValid() {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(false);

        assertThat(mActivity.isValidFragment(ALTERNATIVE_FRAGMENT_CLASSNAME)).isFalse();
    }

    @Test
    public void alternativeFragmentAvailable_alternativeFragmentIsValid() {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(true);
        when(mSecuritySettingsFeatureProvider.getAlternativeSecuritySettingsFragmentClassname())
                .thenReturn(ALTERNATIVE_FRAGMENT_CLASSNAME);

        assertThat(mActivity.isValidFragment(ALTERNATIVE_FRAGMENT_CLASSNAME)).isTrue();
    }

    @Test
    public void onCreate_whenSafetyCenterEnabled_redirectsToSafetyCenter() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(true);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        mActivity.handleSafetyCenterRedirection();

        verify(mActivity).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Intent.ACTION_SAFETY_CENTER);
    }

    @Test
    public void onCreate_whenSafetyCenterDisabled_doesntRedirectToSafetyCenter() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(false);

        mActivity.handleSafetyCenterRedirection();

        verify(mActivity, times(0)).startActivity(any());
    }
}
