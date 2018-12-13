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
 * limitations under the License.
 */

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.AppLaunchSettings;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class AppOpenByDefaultPreferenceControllerTest {

    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    private Context mContext;
    private AppOpenByDefaultPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mController = spy(new AppOpenByDefaultPreferenceController(mContext, "preferred_app"));
        mController.setParentFragment(mFragment);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnAppLaunchSettings() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(AppLaunchSettings.class);
    }

    @Test
    public void displayPreference_noAppEntry_shouldDisablePreference() {
        mController.displayPreference(mScreen);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void displayPreference_noAppInfo_shouldDisablePreference() {
        final AppEntry appEntry = mock(AppEntry.class);
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.displayPreference(mScreen);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void displayPreference_appNotInstalled_shouldDisablePreference() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.displayPreference(mScreen);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void displayPreference_appDisabled_shouldDisablePreference() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        appEntry.info.flags &= ApplicationInfo.FLAG_INSTALLED;
        appEntry.info.enabled = false;
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.displayPreference(mScreen);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void displayPreference_appEnabled_shouldNotDisablePreference() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        appEntry.info.flags |= ApplicationInfo.FLAG_INSTALLED;
        appEntry.info.enabled = true;
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.displayPreference(mScreen);

        verify(mPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noPackageInfo_shouldNotShowPreference() {
        mController.updateState(mPreference);

        verify(mPreference).setVisible(false);
    }

    @Test
    public void updateState_isInstantApp_shouldNotShowPreference() {
        when(mFragment.getPackageInfo()).thenReturn(new PackageInfo());
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));

        mController.updateState(mPreference);

        verify(mPreference).setVisible(false);
    }

    @Test
    public void updateState_notInstantApp_shouldShowPreferenceAndSetSummary() {
        when(mFragment.getPackageInfo()).thenReturn(new PackageInfo());
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));

        mController.updateState(mPreference);

        verify(mPreference).setVisible(true);
        verify(mPreference).setSummary(any());
    }
}
