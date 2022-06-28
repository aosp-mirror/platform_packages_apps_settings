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
package com.android.settings.applications.appinfo;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_DEFAULT;
import static android.app.AppOpsManager.MODE_IGNORED;
import static android.app.AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED;
import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED;
import static com.android.settings.Utils.PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.apphibernation.AppHibernationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class HibernationSwitchPreferenceControllerTest {
    private static final int PACKAGE_UID = 1;
    private static final String INVALID_PACKAGE_NAME = "invalid_package";
    private static final String KEY = "key";
    private static final String VALID_PACKAGE_NAME = "package";
    private static final String EXEMPTED_PACKAGE_NAME = "exempted_package";
    private static final String UNEXEMPTED_PACKAGE_NAME = "unexempted_package";
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AppHibernationManager mAppHibernationManager;
    @Mock
    private SwitchPreference mPreference;

    private HibernationSwitchPreferenceController mController;
    private Context mContext;
    private String mOriginalPreSFlagValue;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(mAppOpsManager);
        when(mContext.getSystemService(AppHibernationManager.class))
                .thenReturn(mAppHibernationManager);
        when(mPackageManager.getPackageUid(eq(VALID_PACKAGE_NAME), anyInt()))
                .thenReturn(PACKAGE_UID);
        when(mPackageManager.getPackageUid(eq(INVALID_PACKAGE_NAME), anyInt()))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mPackageManager.getTargetSdkVersion(eq(EXEMPTED_PACKAGE_NAME)))
                .thenReturn(android.os.Build.VERSION_CODES.Q);
        when(mPackageManager.getTargetSdkVersion(eq(UNEXEMPTED_PACKAGE_NAME)))
                .thenReturn(android.os.Build.VERSION_CODES.S);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        DeviceConfig.setProperty(NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED,
                "true", true /* makeDefault */);
        mController = new HibernationSwitchPreferenceController(mContext, KEY);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mOriginalPreSFlagValue = DeviceConfig.getProperty(NAMESPACE_APP_HIBERNATION,
                PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS);
    }

    @After
    public void cleanUp() {
        // Restore original device config values.
        DeviceConfig.setProperty(NAMESPACE_APP_HIBERNATION, PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS,
                mOriginalPreSFlagValue, true /* makeDefault */);
    }

    @Test
    public void getAvailabilityStatus_featureNotEnabled_shouldNotReturnAvailable() {
        mController.setPackage(VALID_PACKAGE_NAME);
        DeviceConfig.setProperty(NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED,
                "false", true /* makeDefault */);

        assertThat(mController.getAvailabilityStatus()).isNotEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_invalidPackage_shouldReturnNotAvailable() {
        mController.setPackage(INVALID_PACKAGE_NAME);

        assertThat(mController.getAvailabilityStatus()).isNotEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_validPackage_shouldReturnAvailable() {
        mController.setPackage(VALID_PACKAGE_NAME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onPreferenceChange_unhibernatesWhenExempted() {
        mController.setPackage(VALID_PACKAGE_NAME);
        mController.onPreferenceChange(mPreference, false);

        verify(mAppHibernationManager).setHibernatingForUser(VALID_PACKAGE_NAME, false);
        verify(mAppHibernationManager).setHibernatingGlobally(VALID_PACKAGE_NAME, false);
    }

    @Test
    public void isPackageHibernationExemptByUser_preSAppShouldBeExemptByDefault() {
        when(mAppOpsManager.unsafeCheckOpNoThrow(
                eq(OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED), anyInt(), eq(EXEMPTED_PACKAGE_NAME)))
                .thenReturn(MODE_DEFAULT);
        mController.setPackage(EXEMPTED_PACKAGE_NAME);

        assertTrue(mController.isPackageHibernationExemptByUser());
    }

    @Test
    public void isPackageHibernationExemptByUser_preSAppShouldNotBeExemptWithUserSetting() {
        when(mAppOpsManager.unsafeCheckOpNoThrow(
                eq(OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED), anyInt(), eq(EXEMPTED_PACKAGE_NAME)))
                .thenReturn(MODE_ALLOWED);
        mController.setPackage(EXEMPTED_PACKAGE_NAME);

        assertFalse(mController.isPackageHibernationExemptByUser());
    }

    @Test
    public void isPackageHibernationExemptByUser_SAppShouldBeExemptWithUserSetting() {
        when(mAppOpsManager.unsafeCheckOpNoThrow(
                eq(OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED), anyInt(), eq(UNEXEMPTED_PACKAGE_NAME)))
                .thenReturn(MODE_IGNORED);
        mController.setPackage(UNEXEMPTED_PACKAGE_NAME);

        assertTrue(mController.isPackageHibernationExemptByUser());
    }

    @Test
    public void isPackageHibernationExemptByUser_preSAppShouldNotBeExemptByDefaultWithPreSFlag() {
        DeviceConfig.setProperty(NAMESPACE_APP_HIBERNATION, PROPERTY_HIBERNATION_TARGETS_PRE_S_APPS,
                "true", true /* makeDefault */);
        when(mAppOpsManager.unsafeCheckOpNoThrow(
                eq(OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED), anyInt(), eq(EXEMPTED_PACKAGE_NAME)))
                .thenReturn(MODE_DEFAULT);
        mController.setPackage(EXEMPTED_PACKAGE_NAME);

        assertFalse(mController.isPackageHibernationExemptByUser());
    }
}
