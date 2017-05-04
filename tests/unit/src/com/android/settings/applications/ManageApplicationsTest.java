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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.pm.ApplicationInfo;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import org.junit.Test;

public class ManageApplicationsTest {
    @Test
    public void getOverrideFilter_filtersVolumeForAudio() {
        AppFilter filter =
                ManageApplications.getOverrideFilter(
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
    public void getOverrideFilter_filtersVolumeForVideo() {
        AppFilter filter =
                ManageApplications.getOverrideFilter(
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
    public void getOverrideFilter_filtersVolumeForGames() {
        ApplicationsState.AppFilter filter =
                ManageApplications.getOverrideFilter(
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
    public void getOverrideFilter_isEmptyNormally() {
        ApplicationsState.AppFilter filter =
                ManageApplications.getOverrideFilter(
                        ManageApplications.LIST_TYPE_MAIN,
                        ManageApplications.STORAGE_TYPE_DEFAULT,
                        "uuid");
        assertThat(filter).isNull();
    }
}
