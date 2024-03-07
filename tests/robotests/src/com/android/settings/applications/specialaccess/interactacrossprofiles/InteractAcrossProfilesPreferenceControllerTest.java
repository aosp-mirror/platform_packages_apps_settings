/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.interactacrossprofiles;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageInfo;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowApplicationPackageManager;
import com.android.settings.testutils.shadow.ShadowCrossProfileApps;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowApplicationPackageManager.class,
        ShadowCrossProfileApps.class,
})
public class InteractAcrossProfilesPreferenceControllerTest {

    private static final String CROSS_PROFILE_PACKAGE_NAME = "crossProfilePackage";
    private static final String NOT_CROSS_PROFILE_PACKAGE_NAME = "notCrossProfilePackage";
    public static final String INTERACT_ACROSS_PROFILES_PERMISSION =
            "android.permission.INTERACT_ACROSS_PROFILES";
    private static final int PROFILE_ID = 0;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ShadowApplicationPackageManager mShadowPackageManager;
    private final InteractAcrossProfilesDetailsPreferenceController mController =
            new InteractAcrossProfilesDetailsPreferenceController(mContext, "test_key");

    @Before
    public void setUp() {
        mShadowPackageManager = (ShadowApplicationPackageManager) shadowOf(
                mContext.getPackageManager()
        );
    }

    @Test
    public void getAvailabilityStatus_requestedCrossProfilePermission_returnsAvailable() {
        mController.setPackageName(CROSS_PROFILE_PACKAGE_NAME);
        mShadowPackageManager.setInstalledPackagesForUserId(
                PROFILE_ID, ImmutableList.of(CROSS_PROFILE_PACKAGE_NAME));
        ShadowCrossProfileApps shadowCrossProfileApps = (ShadowCrossProfileApps) shadowOf(
                mContext.getSystemService(CrossProfileApps.class)
        );
        shadowCrossProfileApps.addCrossProfilePackage(CROSS_PROFILE_PACKAGE_NAME);
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                CROSS_PROFILE_PACKAGE_NAME);
        packageInfo.requestedPermissions = new String[]{
                INTERACT_ACROSS_PROFILES_PERMISSION};

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notRequestedCrossProfilePermission_returnsDisabled() {
        mController.setPackageName(NOT_CROSS_PROFILE_PACKAGE_NAME);
        mShadowPackageManager.setInstalledPackagesForUserId(
                PROFILE_ID, ImmutableList.of(NOT_CROSS_PROFILE_PACKAGE_NAME));
        ShadowCrossProfileApps shadowCrossProfileApps = (ShadowCrossProfileApps) shadowOf(
                mContext.getSystemService(CrossProfileApps.class)
        );
        shadowCrossProfileApps.addCrossProfilePackage(NOT_CROSS_PROFILE_PACKAGE_NAME);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnInteractAcrossProfilesDetails() {
        assertThat(mController.getDetailFragmentClass())
                .isEqualTo(InteractAcrossProfilesDetails.class);
    }
}
