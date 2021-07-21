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

import com.android.settings.applications.AppStateAlarmsAndRemindersBridge;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class AlarmsAndRemindersDetailsTest {

    @Mock
    private RestrictedSwitchPreference mSwitchPref;
    @Mock
    private PackageInfo mPackageInfo;
    @Mock
    private AppStateAlarmsAndRemindersBridge mAppStateBridge;
    @Mock
    private AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState mPermissionState;

    private AlarmsAndRemindersDetails mFragment = new AlarmsAndRemindersDetails();

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
        when(mAppStateBridge.createPermissionState(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);

        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isTrue();
    }

    @Test
    public void refreshUi_switchPreferenceEnabled() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.createPermissionState(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);
        when(mPermissionState.shouldBeVisible()).thenReturn(false);

        mFragment.refreshUi();
        verify(mSwitchPref).setEnabled(false);

        when(mPermissionState.shouldBeVisible()).thenReturn(true);

        mFragment.refreshUi();
        verify(mSwitchPref).setEnabled(true);
    }

    @Test
    public void refreshUi_switchPreferenceChecked() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        when(mAppStateBridge.createPermissionState(nullable(String.class), anyInt()))
                .thenReturn(mPermissionState);

        when(mPermissionState.isAllowed()).thenReturn(true);
        mFragment.refreshUi();
        verify(mSwitchPref).setChecked(true);

        when(mPermissionState.isAllowed()).thenReturn(false);
        mFragment.refreshUi();
        verify(mSwitchPref).setChecked(false);
    }
}
