/*
 * Copyright (C) 2023 The Android Open Source Project
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
import com.android.settings.applications.AppStateTurnScreenOnBridge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class TurnScreenOnDetailsTest {

    @Mock
    private SwitchPreference mSwitchPref;
    @Mock
    private PackageInfo mPackageInfo;
    @Mock
    private AppStateTurnScreenOnBridge mAppStateBridge;
    @Mock
    private AppStateAppOpsBridge.PermissionState mPermissionState;

    private final TurnScreenOnDetails mFragment = new TurnScreenOnDetails();



    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionHelpers.setField(mFragment, "mSwitchPref", mSwitchPref);
        ReflectionHelpers.setField(mFragment, "mAppBridge", mAppStateBridge);
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
        when(mAppStateBridge.getPermissionInfo(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);

        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isTrue();
    }

    @Test
    public void refreshUi_permissionNotDeclared_switchPreferenceDisabled() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.getPermissionInfo(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);
        mPermissionState.permissionDeclared = false;

        mFragment.refreshUi();
        verify(mSwitchPref).setEnabled(false);
    }

    @Test
    public void refreshUi_permissionDeclared_switchPreferenceEnabled() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.getPermissionInfo(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);
        mPermissionState.permissionDeclared = true;

        mFragment.refreshUi();
        verify(mSwitchPref).setEnabled(true);
    }

    @Test
    public void refreshUi_turnScreenOnAllowed_switchPreferenceChecked() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.getPermissionInfo(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);

        when(mPermissionState.isPermissible()).thenReturn(true);
        mFragment.refreshUi();
        verify(mSwitchPref).setChecked(true);
    }

    @Test
    public void refreshUi_turnScreenOnDisallowed_switchPreferenceUnchecked() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.getPermissionInfo(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);

        when(mPermissionState.isPermissible()).thenReturn(false);
        mFragment.refreshUi();
        verify(mSwitchPref).setChecked(false);
    }
}
