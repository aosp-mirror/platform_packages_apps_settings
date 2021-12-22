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

package com.android.settings.privacy;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.provider.DeviceConfig;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.safetycenter.SafetyCenterStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
public class PrivacyDashboardActivityTest {

    private static final String DEFAULT_FRAGMENT_CLASSNAME = "DefaultFragmentClassname";

    private Settings.PrivacyDashboardActivity mActivity;

    @Before
    public void setUp() {
        DeviceConfig.resetToDefaults(android.provider.Settings.RESET_MODE_PACKAGE_DEFAULTS,
                DeviceConfig.NAMESPACE_PRIVACY);
        final Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_PRIVACY_SETTINGS);
        intent.setClass(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                Settings.PrivacyDashboardActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT_CLASSNAME);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mActivity =
                        spy((Settings.PrivacyDashboardActivity) InstrumentationRegistry
                                .getInstrumentation().newActivity(
                                        getClass().getClassLoader(),
                                        Settings.PrivacyDashboardActivity.class.getName(),
                                        intent));
            } catch (Exception e) {
                throw new RuntimeException(e); // nothing to do
            }
        });
        doNothing().when(mActivity).startActivity(any(Intent.class));
    }

    @After
    public void tearDown() {
        DeviceConfig.resetToDefaults(android.provider.Settings.RESET_MODE_PACKAGE_DEFAULTS,
                DeviceConfig.NAMESPACE_PRIVACY);
    }

    @Test
    public void onCreate_whenSafetyCenterEnabled_redirectsToSafetyCenter() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_PRIVACY,
                SafetyCenterStatus.SAFETY_CENTER_IS_ENABLED,
                /* value = */ Boolean.toString(true),
                /* makeDefault = */ false);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        mActivity.handleSafetyCenterRedirection();

        verify(mActivity).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Intent.ACTION_SAFETY_CENTER);
    }

    @Test
    public void onCreate_whenSafetyCenterDisabled_doesntRedirectToSafetyCenter() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_PRIVACY,
                SafetyCenterStatus.SAFETY_CENTER_IS_ENABLED,
                /* value = */ Boolean.toString(false),
                /* makeDefault = */ false);

        mActivity.handleSafetyCenterRedirection();

        verify(mActivity, times(0)).startActivity(any());
    }
}
