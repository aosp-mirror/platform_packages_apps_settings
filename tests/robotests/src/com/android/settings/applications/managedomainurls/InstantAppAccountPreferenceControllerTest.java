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
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class InstantAppAccountPreferenceControllerTest {

    private static final String PREF_KEY = "instant_app_account_pref";

    @Mock
    private ComponentName mComponentName;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private ContentResolver mContentResolver;
    private int mEnableEphemeralFeature;
    private InstantAppAccountPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mContentResolver = mContext.getContentResolver();
        mEnableEphemeralFeature = Settings.Global.getInt(mContentResolver,
                ENABLE_EPHEMERAL_FEATURE, 1);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContentResolver, ENABLE_EPHEMERAL_FEATURE,
                mEnableEphemeralFeature);
    }

    @Test
    public void testGetAvailabilityStatus_nullAppSettingsComponent() {
        when(mPackageManager.getInstantAppResolverSettingsComponent()).thenReturn(null);
        mController = new InstantAppAccountPreferenceController(mContext, PREF_KEY);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testGetAvailabilityStatus_enableWebActions() {
        when(mPackageManager.getInstantAppResolverSettingsComponent()).thenReturn(mComponentName);
        mController = new InstantAppAccountPreferenceController(mContext, PREF_KEY);
        Settings.Global.putInt(mContentResolver, ENABLE_EPHEMERAL_FEATURE, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_disableWebActions() {
        when(mPackageManager.getInstantAppResolverSettingsComponent()).thenReturn(mComponentName);
        mController = new InstantAppAccountPreferenceController(mContext, PREF_KEY);
        Settings.Global.putInt(mContentResolver, ENABLE_EPHEMERAL_FEATURE, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
