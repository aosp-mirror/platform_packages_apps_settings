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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.UserManager;

import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settings.applications.AppStateInstallAppsBridge.InstallAppsState;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class ExternalSourcesDetailsTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private RestrictedSwitchPreference mSwitchPref;
    @Mock
    private PackageInfo mPackageInfo;

    private ExternalSourcesDetails mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = new ExternalSourcesDetails();
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mSwitchPref", mSwitchPref);
    }

    @Test
    public void refreshUi_noPackageInfo_shouldReturnFalseAndNoCrash() {
        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isFalse();
        // should not crash
    }

    @Test
    public void refreshUi_noApplicationInfo_shouldReturnFalseAndNoCrash() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);

        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isFalse();
        // should not crash
    }

    @Test
    public void refreshUi_hasApplicationInfo_shouldReturnTrue() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        final AppStateInstallAppsBridge appBridge = mock(AppStateInstallAppsBridge.class);
        ReflectionHelpers.setField(mFragment, "mAppBridge", appBridge);
        when(appBridge.createInstallAppsStateFor(nullable(String.class), anyInt()))
                .thenReturn(mock(InstallAppsState.class));

        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isTrue();
    }
}
