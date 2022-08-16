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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import androidx.loader.app.LoaderManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.datausage.AppDataUsage;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppDataUsagePreferenceControllerTest {

    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private AppDataUsagePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = spy(new AppDataUsagePreferenceController(mContext, "test_key"));
        mController.setParentFragment(mFragment);
    }

    @Test
    public void getAvailabilityStatus_bandwidthControlEnabled_shouldReturnAvailable() {
        doReturn(true).when(mController).isBandwidthControlEnabled();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_bandwidthControlDisabled_shouldReturnDisabled() {
        doReturn(false).when(mController).isBandwidthControlEnabled();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void onResume_notAvailable_shouldNotRestartDataLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();
        doReturn(BasePreferenceController.CONDITIONALLY_UNAVAILABLE).when(
                mController).getAvailabilityStatus();

        mController.onResume();

        verify(mLoaderManager, never()).restartLoader(
                AppInfoDashboardFragment.LOADER_CHART_DATA, Bundle.EMPTY, mController);
    }

    @Test
    public void onResume_isAvailable_shouldRestartDataLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();
        doReturn(BasePreferenceController.AVAILABLE).when(mController).getAvailabilityStatus();
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.onResume();

        verify(mLoaderManager).restartLoader(eq(AppInfoDashboardFragment.LOADER_CHART_DATA),
                nullable(Bundle.class), eq(mController));
    }

    @Test
    public void onPause_shouldDestroyDataLoader() {
        doReturn(BasePreferenceController.AVAILABLE).when(mController).getAvailabilityStatus();
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onPause();

        verify(mLoaderManager).destroyLoader(AppInfoDashboardFragment.LOADER_CHART_DATA);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnAppDataUsage() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(AppDataUsage.class);
    }

    @Test
    public void updateState_shouldUpdatePreferenceSummary() {
        final Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setSummary(any());
    }

    @Test
    public void displayPreference_noEntry_preferenceShouldNotEnable() {
        mController.mAppEntry = null;
        Preference preference = new Preference(mContext);
        when(mScreen.findPreference(any())).thenReturn(preference);
        doReturn(true).when(mController).isBandwidthControlEnabled();

        mController.displayPreference(mScreen);

        assertThat(preference.isEnabled()).isFalse();
    }

    @Test
    public void displayPreference_appIsInstalled_preferenceShouldEnable() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        appEntry.info.flags = ApplicationInfo.FLAG_INSTALLED;
        mController.mAppEntry = appEntry;
        Preference preference = new Preference(mContext);
        when(mScreen.findPreference(any())).thenReturn(preference);
        doReturn(true).when(mController).isBandwidthControlEnabled();

        mController.displayPreference(mScreen);

        assertThat(preference.isEnabled()).isTrue();
    }

    @Test
    public void displayPreference_appIsNotInstalled_preferenceShouldDisable() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        mController.mAppEntry = appEntry;
        Preference preference = new Preference(mContext);
        when(mScreen.findPreference(any())).thenReturn(preference);
        doReturn(true).when(mController).isBandwidthControlEnabled();

        mController.displayPreference(mScreen);

        assertThat(preference.isEnabled()).isFalse();
    }
}
