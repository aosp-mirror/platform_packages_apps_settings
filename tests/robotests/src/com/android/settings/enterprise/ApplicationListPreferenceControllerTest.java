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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.UserAppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ApplicationListPreferenceControllerTest {

    private static final int MAIN_USER_ID = 0;

    private static final int MANAGED_PROFILE_ID = 10;
    private static final int PER_USER_UID_RANGE = 100000;
    private static final int MAIN_USER_APP_UID = MAIN_USER_ID * PER_USER_UID_RANGE;
    private static final int MANAGED_PROFILE_APP_UID = MANAGED_PROFILE_ID * PER_USER_UID_RANGE;

    private static final String APP_1 = "APP_1";
    private static final String APP_2 = "APP_2";
    private static final String APP_3 = "APP_3";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PackageManager mPackageManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private SettingsPreferenceFragment mFragment;
    @Mock
    private UserManager mUserManager;

    private Context mContext;
    private ApplicationListPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mPackageManager.getText(eq(APP_1), anyInt(), any())).thenReturn(APP_1);
        when(mPackageManager.getText(eq(APP_2), anyInt(), any())).thenReturn(APP_2);
        when(mPackageManager.getText(eq(APP_3), anyInt(), any())).thenReturn(APP_3);

        mController = new ApplicationListPreferenceController(mContext, new ThreeAppsBuilder(),
                mPackageManager, mFragment);
    }

    @Test
    public void checkNumberAndTitlesOfApps() {
        ArgumentCaptor<Preference> apps = ArgumentCaptor.forClass(Preference.class);
        verify(mScreen, times(3)).addPreference(apps.capture());
        final Set<String> expectedPackages = new HashSet<>(Arrays.asList(APP_1, APP_2, APP_3));
        final Set<String> packages = new HashSet<>();

        for (Preference p : apps.getAllValues()) {
            packages.add(p.getTitle().toString());
        }
        assertThat(packages).isEqualTo(expectedPackages);
    }

    @Test
    public void isAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isNull();
    }

    private static class ThreeAppsBuilder
            implements ApplicationListPreferenceController.ApplicationListBuilder {
        @Override
        public void buildApplicationList(Context context,
                ApplicationFeatureProvider.ListOfAppsCallback callback) {
            final List<UserAppInfo> apps = new ArrayList<>();
            final UserInfo user = new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN);
            apps.add(new UserAppInfo(user, buildInfo(MAIN_USER_APP_UID, APP_1, 0, 0)));
            apps.add(new UserAppInfo(user, buildInfo(MAIN_USER_APP_UID, APP_2, 0, 0)));
            apps.add(new UserAppInfo(user, buildInfo(MANAGED_PROFILE_APP_UID, APP_3, 0, 0)));
            callback.onListOfAppsResult(apps);
        }
    }
}
