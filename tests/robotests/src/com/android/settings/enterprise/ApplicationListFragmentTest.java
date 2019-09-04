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

import static com.android.settings.testutils.ApplicationTestUtils.buildInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.UserAppInfo;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ApplicationListFragmentTest {

    private static final int USER_ID = 0;
    private static final int USER_APP_UID = 0;

    private static final String APP = "APP";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    private ApplicationListFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mPreferenceManager.getContext()).thenReturn(mContext);

        mFragment = new ApplicationListFragmentTestable(mPreferenceManager, mScreen);
    }

    @Test
    public void getLogTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("EnterprisePrivacySettings");
    }

    @Test
    public void getScreenResource() {
        assertThat(mFragment.getPreferenceScreenResId())
            .isEqualTo(R.xml.app_list_disclosure_settings);
    }

    @Test
    public void getPreferenceControllers() {
        final List<AbstractPreferenceController> controllers =
            mFragment.createPreferenceControllers(mContext);
        assertThat(controllers).isNotNull();
        assertThat(controllers.size()).isEqualTo(1);
        assertThat(controllers.get(0)).isInstanceOf(ApplicationListPreferenceController.class);
    }

    @Test
    public void getCategories() {
        assertThat(new ApplicationListFragment.AdminGrantedPermissionCamera().getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_PERMISSIONS);
        assertThat(new ApplicationListFragment.AdminGrantedPermissionLocation().
                getMetricsCategory()).isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_PERMISSIONS);
        assertThat(new ApplicationListFragment.AdminGrantedPermissionMicrophone().
                getMetricsCategory()).isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_PERMISSIONS);
        assertThat(new ApplicationListFragment.EnterpriseInstalledPackages().getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_INSTALLED_APPS);
    }

    private static class ApplicationListFragmentTestable extends ApplicationListFragment {

        private final PreferenceManager mPreferenceManager;
        private final PreferenceScreen mPreferenceScreen;

        private ApplicationListFragmentTestable(PreferenceManager preferenceManager,
                PreferenceScreen screen) {
            this.mPreferenceManager = preferenceManager;
            this.mPreferenceScreen = screen;
        }

        @Override
        public void buildApplicationList(Context context,
                ApplicationFeatureProvider.ListOfAppsCallback callback) {
            final UserInfo user = new UserInfo(USER_ID, "main", UserInfo.FLAG_ADMIN);
            final List<UserAppInfo> apps = new ArrayList<>();
            apps.add(new UserAppInfo(user, buildInfo(USER_APP_UID, APP, 0, 0)));
            callback.onListOfAppsResult(apps);
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceScreen;
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.VIEW_UNKNOWN;
        }
    }
}
