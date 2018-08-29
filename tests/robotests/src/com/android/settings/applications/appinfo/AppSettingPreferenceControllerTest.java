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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import androidx.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(SettingsRobolectricTestRunner.class)
public class AppSettingPreferenceControllerTest {

    private static final String TEST_PKG_NAME = "test_pkg";
    private static final String TEST_CLASS_NAME = "name";
    private static final Intent TEST_INTENT =
        new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
            .setClassName(TEST_PKG_NAME, TEST_CLASS_NAME);
    private static final Intent RESOLVED_INTENT =
        new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
            .setPackage(TEST_PKG_NAME);

    @Mock
    private AppInfoDashboardFragment mParent;
    private Application mApplication;
    private ShadowPackageManager mPackageManager;
    private AppSettingPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplication = RuntimeEnvironment.application;
        mPackageManager = shadowOf(mApplication.getPackageManager());
        mController = new AppSettingPreferenceController(mApplication, "test_key");
        mController.setPackageName(TEST_PKG_NAME).setParentFragment(mParent);
        mPreference = new Preference(mApplication);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_noAppSetting_shouldNotBeAvailable() {
        assertThat(mController.isAvailable())
                .isFalse();
    }

    @Test
    public void getAvailabilityStatus_noPackageName_shouldNotBeAvailable() {
        mController.setPackageName(null);

        assertThat(mController.isAvailable())
                .isFalse();
    }

    @Test
    public void getAvailabilityStatus_hasAppSetting_shouldBeAvailable() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = TEST_PKG_NAME;
        info.activityInfo.name = TEST_CLASS_NAME;

        mPackageManager.addResolveInfoForIntent(RESOLVED_INTENT, info);

        assertThat(mController.isAvailable())
                .isTrue();
    }

    @Test
    public void clickPreference_noAppSetting_shouldDoNothing() {
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }

    @Test
    public void clickPreference_hasAppSetting_shouldLaunchIntent() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = TEST_PKG_NAME;
        info.activityInfo.name = TEST_CLASS_NAME;

        mPackageManager.addResolveInfoForIntent(RESOLVED_INTENT, info);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
        assertThat(shadowOf(mApplication).getNextStartedActivity().getComponent())
            .isEqualTo(TEST_INTENT.getComponent());
    }
}
