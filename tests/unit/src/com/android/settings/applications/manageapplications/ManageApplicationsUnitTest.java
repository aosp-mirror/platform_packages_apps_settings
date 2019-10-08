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

package com.android.settings.applications.manageapplications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.pm.ApplicationInfo;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.applications.ApplicationsState.CompoundFilter;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ManageApplicationsUnitTest {
    @Test
    public void getCompositeFilter_filtersVolumeForAudio() {
        AppFilter filter =
                ManageApplications.getCompositeFilter(
                        ManageApplications.LIST_TYPE_STORAGE,
                        ManageApplications.STORAGE_TYPE_MUSIC,
                        "uuid");
        final ApplicationInfo info = new ApplicationInfo();
        info.volumeUuid = "uuid";
        info.category = ApplicationInfo.CATEGORY_AUDIO;
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = info;

        assertThat(filter.filterApp(appEntry)).isTrue();
    }

    @Test
    public void getCompositeFilter_filtersVolumeForVideo() {
        AppFilter filter =
                ManageApplications.getCompositeFilter(
                        ManageApplications.LIST_TYPE_MOVIES,
                        ManageApplications.STORAGE_TYPE_DEFAULT,
                        "uuid");
        final ApplicationInfo info = new ApplicationInfo();
        info.volumeUuid = "uuid";
        info.category = ApplicationInfo.CATEGORY_VIDEO;
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = info;

        assertThat(filter.filterApp(appEntry)).isTrue();
    }

    @Test
    public void getCompositeFilter_filtersVolumeForGames() {
        ApplicationsState.AppFilter filter =
                ManageApplications.getCompositeFilter(
                        ManageApplications.LIST_TYPE_GAMES,
                        ManageApplications.STORAGE_TYPE_DEFAULT,
                        "uuid");
        final ApplicationInfo info = new ApplicationInfo();
        info.volumeUuid = "uuid";
        info.category = ApplicationInfo.CATEGORY_GAME;
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = info;

        assertThat(filter.filterApp(appEntry)).isTrue();
    }

    @Test
    public void getCompositeFilter_isEmptyNormally() {
        ApplicationsState.AppFilter filter =
                ManageApplications.getCompositeFilter(
                        ManageApplications.LIST_TYPE_MAIN,
                        ManageApplications.STORAGE_TYPE_DEFAULT,
                        "uuid");
        assertThat(filter).isNull();
    }

    @Test
    public void getCompositeFilter_worksWithInstantApps() throws Exception {
        Field field = AppUtils.class.getDeclaredField("sInstantAppDataProvider");
        field.setAccessible(true);
        field.set(AppUtils.class, (InstantAppDataProvider) (i -> true));

        AppFilter filter =
                ManageApplications.getCompositeFilter(
                        ManageApplications.LIST_TYPE_STORAGE,
                        ManageApplications.STORAGE_TYPE_MUSIC,
                        "uuid");
        AppFilter composedFilter = new CompoundFilter(ApplicationsState.FILTER_INSTANT, filter);

        final ApplicationInfo info = new ApplicationInfo();
        info.volumeUuid = "uuid";
        info.category = ApplicationInfo.CATEGORY_AUDIO;
        info.privateFlags = ApplicationInfo.PRIVATE_FLAG_INSTANT;
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = info;

        assertThat(composedFilter.filterApp(appEntry)).isTrue();
    }

    @Test
    public void getCompositeFilter_worksForLegacyPrivateSettings() throws Exception {
        ApplicationsState.AppFilter filter =
                ManageApplications.getCompositeFilter(
                        ManageApplications.LIST_TYPE_STORAGE,
                        ManageApplications.STORAGE_TYPE_LEGACY,
                        "uuid");
        final ApplicationInfo info = new ApplicationInfo();
        info.volumeUuid = "uuid";
        info.category = ApplicationInfo.CATEGORY_GAME;
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = info;

        assertThat(filter.filterApp(appEntry)).isTrue();
    }
}
