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

import com.android.settings.applications.AppStorageSettings;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.StorageStatsSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppStoragePreferenceControllerTest {

    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private AppInfoDashboardFragment mFragment;

    private Context mContext;
    private AppStoragePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mController = spy(new AppStoragePreferenceController(mContext, "key"));
        mController.setParentFragment(mFragment);
    }

    @Test
    public void onResume_shouldRestartStorageLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onResume();

        verify(mLoaderManager).restartLoader(AppInfoDashboardFragment.LOADER_STORAGE, Bundle.EMPTY,
                mController);
    }

    @Test
    public void onPause_shouldDestroyStorageLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onPause();

        verify(mLoaderManager).destroyLoader(AppInfoDashboardFragment.LOADER_STORAGE);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnAppStorageSettings() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(AppStorageSettings.class);
    }

    @Test
    public void updateState_shouldUpdatePreferenceSummary() {
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);
        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setSummary(any());
    }

    @Test
    public void updateState_entryIsNull_shouldNotUpdatePreferenceSummary() {
        when(mFragment.getAppEntry()).thenReturn(null);
        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference, never()).setSummary(any());
    }

    @Test
    public void getStorageSummary_shouldWorkForExternal() {
        final StorageStatsSource.AppStorageStats stats =
                mock(StorageStatsSource.AppStorageStats.class);
        when(stats.getTotalBytes()).thenReturn(1L);

        assertThat(mController.getStorageSummary(stats, true))
                .isEqualTo("1 B used in external storage");
    }

    @Test
    public void getStorageSummary_shouldWorkForInternal() {
        final StorageStatsSource.AppStorageStats stats =
                mock(StorageStatsSource.AppStorageStats.class);
        when(stats.getTotalBytes()).thenReturn(1L);

        assertThat(mController.getStorageSummary(stats, false))
                .isEqualTo("1 B used in internal storage");
    }
}
