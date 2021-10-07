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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import androidx.preference.SwitchPreference;

import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateMediaManagementAppsBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class MediaManagementAppsDetailsTest {

    @Mock
    private SwitchPreference mSwitchPref;
    @Mock
    private PackageInfo mPackageInfo;
    @Mock
    private AppStateMediaManagementAppsBridge mAppStateBridge;
    @Mock
    private AppStateAppOpsBridge.PermissionState mPermissionState;

    private MediaManagementAppsDetails mFragment = new MediaManagementAppsDetails();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ReflectionHelpers.setField(mFragment, "mSwitchPref", mSwitchPref);
        ReflectionHelpers.setField(mFragment, "mAppBridge", mAppStateBridge);
    }

    @Test
    public void refreshUi_noPackageInfo_returnFalse() {
        assertThat(mFragment.refreshUi()).isFalse();
    }

    @Test
    public void refreshUi_noApplicationInfo_returnFalse() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);

        assertThat(mFragment.refreshUi()).isFalse();
    }

    @Test
    public void refreshUi_hasApplicationInfo_returnTrue() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.createPermissionState(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);

        assertThat(mFragment.refreshUi()).isTrue();
    }

    @Test
    public void refreshUi_permissionDeclaredFalse_switchPreferenceUnEnabled() {
        assert_refreshUi_switchPreferenceSetEnabled(false /* isPermissionDeclared */);
    }

    @Test
    public void refreshUi_permissionDeclaredTrue_switchPreferenceEnabled() {
        assert_refreshUi_switchPreferenceSetEnabled(true /* isPermissionDeclared */);
    }

    @Test
    public void refreshUi_isPermissibleFalse_switchPreferenceUnChecked() {
        assert_refreshUi_switchPreferenceSetChecked(false /* isPermissible */);
    }

    @Test
    public void refreshUi_isPermissibleTrue_switchPreferenceChecked() {
        assert_refreshUi_switchPreferenceSetChecked(true /* isPermissible */);
    }

    private void assert_refreshUi_switchPreferenceSetEnabled(boolean isPermissionDeclared) {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.createPermissionState(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);
        mPermissionState.permissionDeclared = isPermissionDeclared;

        mFragment.refreshUi();
        verify(mSwitchPref).setEnabled(isPermissionDeclared);
    }

    private void assert_refreshUi_switchPreferenceSetChecked(boolean isPermissible) {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.createPermissionState(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);
        when(mPermissionState.isPermissible()).thenReturn(isPermissible);

        mFragment.refreshUi();
        verify(mSwitchPref).setChecked(isPermissible);
    }
}
