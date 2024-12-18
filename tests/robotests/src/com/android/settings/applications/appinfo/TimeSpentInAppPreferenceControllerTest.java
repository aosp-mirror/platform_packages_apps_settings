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

import static android.content.Intent.EXTRA_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class TimeSpentInAppPreferenceControllerTest {

    private static final String TEST_KEY = "test_tey";
    private static final Intent TEST_INTENT = new Intent(
            TimeSpentInAppPreferenceController.SEE_TIME_IN_APP_TEMPLATE)
            .setPackage("com.wellbeing")
            .putExtra(EXTRA_PACKAGE_NAME, "com.android.settings");

    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private ShadowPackageManager mPackageManager;
    private TimeSpentInAppPreferenceController mController;
    private Preference mPreference;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = spy(RuntimeEnvironment.application);
        PackageManager pm = spy(mContext.getPackageManager());
        doReturn(pm).when(mContext).getPackageManager();
        doReturn(TEST_INTENT.getPackage()).when(pm).getWellbeingPackageName();
        mPackageManager = Shadows.shadowOf(pm);
        mController = new TimeSpentInAppPreferenceController(mContext, TEST_KEY);
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void noPackageName_shouldBeDisabled() {
        mController.setPackageName(null);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void noIntentHandler_shouldBeDisabled() {
        mController.setPackageName(TEST_INTENT.getStringExtra(EXTRA_PACKAGE_NAME));

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void hasIntentHandler_notSystemApp_shouldBeDisabled() {
        mPackageManager.addResolveInfoForIntent(TEST_INTENT, new ResolveInfo());
        mController.setPackageName(TEST_INTENT.getStringExtra(EXTRA_PACKAGE_NAME));

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void hasIntentHandler_resolvedToSystemApp_shouldBeAvailable() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.applicationInfo = new ApplicationInfo();
        info.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        mPackageManager.addResolveInfoForIntent(TEST_INTENT, info);
        mController.setPackageName(TEST_INTENT.getStringExtra(EXTRA_PACKAGE_NAME));

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
        mController.displayPreference(mScreen);

        final Intent intent = mPreference.getIntent();
        assertThat(intent.getAction()).isEqualTo(TEST_INTENT.getAction());
        assertThat(intent.getStringExtra(EXTRA_PACKAGE_NAME))
                .isEqualTo(TEST_INTENT.getStringExtra(EXTRA_PACKAGE_NAME));
    }

    @Test
    public void getSummaryTextInBackground_shouldQueryAppFeatureProvider() {
        mController.getSummaryTextInBackground();

        verify(mFeatureFactory.applicationFeatureProvider).getTimeSpentInApp(
                nullable(String.class));
    }

    @Test
    public void displayPreference_noEntry_preferenceShouldNotEnable() {
        mController.mAppEntry = null;
        Preference preference = new Preference(mContext);
        when(mScreen.findPreference(any())).thenReturn(preference);

        mController.displayPreference(mScreen);

        assertThat(preference.isEnabled()).isFalse();
    }

    @Test
    public void displayPreference_appIsInstalled_preferenceShouldEnable() {
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = new ApplicationInfo();
        appEntry.info.flags = ApplicationInfo.FLAG_INSTALLED;
        mController.mAppEntry = appEntry;
        Preference preference = new Preference(mContext);
        when(mScreen.findPreference(any())).thenReturn(preference);

        mController.displayPreference(mScreen);

        assertThat(preference.isEnabled()).isTrue();
    }

    @Test
    public void displayPreference_appIsNotInstalled_preferenceShouldDisable() {
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = new ApplicationInfo();
        mController.mAppEntry = appEntry;
        Preference preference = new Preference(mContext);
        when(mScreen.findPreference(any())).thenReturn(preference);

        mController.displayPreference(mScreen);

        assertThat(preference.isEnabled()).isFalse();
    }
}
