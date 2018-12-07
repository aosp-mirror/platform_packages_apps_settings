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
package com.android.settings.applications.managedomainurls;

import static android.provider.Settings.Global.ENABLE_EPHEMERAL_FEATURE;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class InstantAppWebActionPreferenceControllerTest {
    private static final String PREF_KEY = "instant_app_web_action_toggle";
    private static final String KEY_INSTANT_APPS_ENABLED = Settings.Secure.INSTANT_APPS_ENABLED;

    private Context mContext;
    private ContentResolver mContentResolver;
    private int mEnableEphemeralFeature;
    private InstantAppWebActionPreferenceController mController;
    private SwitchPreference mSwitchPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mController = new InstantAppWebActionPreferenceController(mContext, PREF_KEY);
        mSwitchPreference = new SwitchPreference(mContext);
        mEnableEphemeralFeature = Settings.Global.getInt(mContentResolver,
                ENABLE_EPHEMERAL_FEATURE, 1);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContentResolver, ENABLE_EPHEMERAL_FEATURE,
                mEnableEphemeralFeature);
    }

    @Test
    public void testGetAvailabilityStatus_enableWebActions() {
        Settings.Global.putInt(mContentResolver, ENABLE_EPHEMERAL_FEATURE, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_disableWebActions() {
        Settings.Global.putInt(mContentResolver, ENABLE_EPHEMERAL_FEATURE, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onPreferenceChange_enable() {
        mController.onPreferenceChange(mSwitchPreference, true);

        assertThat(Settings.Secure.getInt(mContentResolver, KEY_INSTANT_APPS_ENABLED, -1))
                .isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_disable() {
        mController.onPreferenceChange(mSwitchPreference, false);

        assertThat(Settings.Secure.getInt(mContentResolver, KEY_INSTANT_APPS_ENABLED, -1))
                .isEqualTo(0);
    }
}
