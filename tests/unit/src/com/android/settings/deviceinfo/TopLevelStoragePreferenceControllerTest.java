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

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.icu.text.NumberFormat;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TopLevelStoragePreferenceControllerTest {

    @Mock
    private StorageManagerVolumeProvider mStorageManagerVolumeProvider;

    private Context mContext;
    private TopLevelStoragePreferenceController mController;
    private List<VolumeInfo> mVolumes;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mVolumes = new ArrayList<>();
        mVolumes.add(mock(VolumeInfo.class, RETURNS_DEEP_STUBS));
        when(mStorageManagerVolumeProvider.getVolumes()).thenReturn(mVolumes);

        mController = spy(new TopLevelStoragePreferenceController(mContext, "test_key"));
    }

    @Test
    public void updateSummary_shouldDisplayUsedPercentAndFreeSpace() throws Exception {
        final VolumeInfo volumeInfo = mVolumes.get(0);
        when(volumeInfo.isMountedReadable()).thenReturn(true);
        when(volumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mStorageManagerVolumeProvider
                .getTotalBytes(nullable(StorageStatsManager.class), nullable(VolumeInfo.class)))
                .thenReturn(500L);
        when(mStorageManagerVolumeProvider
                .getFreeBytes(nullable(StorageStatsManager.class), nullable(VolumeInfo.class)))
                .thenReturn(0L);
        when(mController.getStorageManagerVolumeProvider())
                .thenReturn(mStorageManagerVolumeProvider);
        final String percentage = NumberFormat.getPercentInstance().format(1);
        final String freeSpace = Formatter.formatFileSize(mContext, 0);
        final Preference preference = new Preference(mContext);

        // Wait for asynchronous thread to finish, otherwise test will flake.
        Future thread = mController.refreshSummaryThread(preference);
        try {
            thread.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            fail("Exception during automatic selection");
        }


        // Sleep for 5 seconds because a function is executed on the main thread from within
        // the background thread.
        TimeUnit.SECONDS.sleep(5);
        assertThat(preference.getSummary()).isEqualTo(ResourcesUtils.getResourcesString(
                mContext, "storage_summary", percentage, freeSpace));
    }
}
