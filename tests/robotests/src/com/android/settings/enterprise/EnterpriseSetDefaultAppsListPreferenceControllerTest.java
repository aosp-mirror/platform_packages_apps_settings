/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.EnterpriseDefaultApps;
import com.android.settings.applications.UserAppInfo;
import com.android.settings.testutils.ApplicationTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class EnterpriseSetDefaultAppsListPreferenceControllerTest {

    private static final int USER_ID = 0;
    private static final int APP_UID = 0;

    private static final String APP_1 = "APP_1";
    private static final String APP_2 = "APP_2";
    private static final String BROWSER_TITLE = "Browser app";
    private static final String PHONE_TITLE = "Phone apps";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceManager mPrefenceManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PackageManager mPackageManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private SettingsPreferenceFragment mFragment;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mPrefenceManager.getContext()).thenReturn(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPrefenceManager);

        when(mContext.getString(R.string.default_browser_title)).thenReturn(BROWSER_TITLE);
        Resources resources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(resources);
        when(resources.getQuantityString(R.plurals.default_phone_app_title, 2))
                .thenReturn(PHONE_TITLE);
        when(mContext.getString(R.string.app_names_concatenation_template_2))
                .thenReturn("%1$s, %2$s");

        when(mPackageManager.getText(eq(APP_1), anyInt(), any())).thenReturn(APP_1);
        when(mPackageManager.getText(eq(APP_2), anyInt(), any())).thenReturn(APP_2);
    }

    @Test
    public void testMultipleAppsForOneTypeOfDefault() {
        final UserInfo user = new UserInfo(USER_ID, "main", UserInfo.FLAG_ADMIN);
        final ApplicationInfo appInfo1 = ApplicationTestUtils.buildInfo(APP_UID, APP_1, 0, 0);
        final ApplicationInfo appInfo2 = ApplicationTestUtils.buildInfo(APP_UID, APP_2, 0, 0);

        when(mFeatureFactory.userFeatureProvider.getUserProfiles())
                .thenReturn(Collections.singletonList(new UserHandle(USER_ID)));
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isInCompMode()).thenReturn(false);
        when(mFeatureFactory.applicationFeatureProvider
                .findPersistentPreferredActivities(anyInt(), any()))
                .thenReturn(Collections.emptyList());
        when(mFeatureFactory.applicationFeatureProvider
                .findPersistentPreferredActivities(eq(USER_ID),
                        eq(EnterpriseDefaultApps.BROWSER.getIntents())))
                .thenReturn(Collections.singletonList(new UserAppInfo(user, appInfo1)));
        when(mFeatureFactory.applicationFeatureProvider
                .findPersistentPreferredActivities(eq(USER_ID),
                        eq(EnterpriseDefaultApps.PHONE.getIntents()))).thenReturn(
                                Arrays.asList(new UserAppInfo(user, appInfo1),
                                        new UserAppInfo(user, appInfo2)));

        new EnterpriseSetDefaultAppsListPreferenceController(mContext, mFragment, mPackageManager);
        ShadowApplication.runBackgroundTasks();

        ArgumentCaptor<Preference> apps = ArgumentCaptor.forClass(Preference.class);
        verify(mScreen, times(2)).addPreference(apps.capture());

        assertThat(apps.getAllValues().get(0).getTitle()).isEqualTo(BROWSER_TITLE);
        assertThat(apps.getAllValues().get(0).getSummary()).isEqualTo(APP_1);

        assertThat(apps.getAllValues().get(1).getTitle()).isEqualTo(PHONE_TITLE);
        assertThat(apps.getAllValues().get(1).getSummary()).isEqualTo(APP_1 + ", " + APP_2);
    }

    @Test
    public void isAvailable() {
        when(mFeatureFactory.userFeatureProvider.getUserProfiles())
                .thenReturn(Collections.singletonList(new UserHandle(USER_ID)));
        when(mFeatureFactory.applicationFeatureProvider
                .findPersistentPreferredActivities(anyInt(), any()))
                .thenReturn(Collections.emptyList());
        final EnterpriseSetDefaultAppsListPreferenceController controller =
                new EnterpriseSetDefaultAppsListPreferenceController(mContext, mFragment,
                        mPackageManager);
        assertThat(controller.isAvailable()).isTrue();
    }

    @Test
    public void getPreferenceKey() {
        when(mFeatureFactory.userFeatureProvider.getUserProfiles())
                .thenReturn(Collections.singletonList(new UserHandle(USER_ID)));
        when(mFeatureFactory.applicationFeatureProvider
                .findPersistentPreferredActivities(anyInt(), any()))
                .thenReturn(Collections.emptyList());
        final EnterpriseSetDefaultAppsListPreferenceController controller =
                new EnterpriseSetDefaultAppsListPreferenceController(mContext, mFragment,
                        mPackageManager);
        assertThat(controller.getPreferenceKey()).isNull();
    }
}