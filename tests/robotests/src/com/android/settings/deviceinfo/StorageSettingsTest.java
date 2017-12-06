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

package com.android.settings.deviceinfo;


import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.usage.StorageStatsManager;
import android.icu.text.NumberFormat;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StorageSettingsTest {

    @Mock
    private StorageManagerVolumeProvider mStorageManagerVolumeProvider;
    @Mock
    private Activity mActivity;

    private List<VolumeInfo> mVolumes;

    private StorageSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mVolumes = new ArrayList<>();
        mVolumes.add(mock(VolumeInfo.class, RETURNS_DEEP_STUBS));
        mSettings = new StorageSettings();
        when(mStorageManagerVolumeProvider.getVolumes()).thenReturn(mVolumes);
    }

    @Test
    public void updateSummary_shouldDisplayUsedPercentAndFreeSpace() throws Exception {
        final SummaryLoader loader = mock(SummaryLoader.class);
        final SummaryLoader.SummaryProvider provider =
                StorageSettings.SUMMARY_PROVIDER_FACTORY.createSummaryProvider(mActivity, loader);
        final VolumeInfo volumeInfo = mVolumes.get(0);
        when(volumeInfo.isMountedReadable()).thenReturn(true);
        when(volumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mStorageManagerVolumeProvider.getTotalBytes(
                        nullable(StorageStatsManager.class), nullable(VolumeInfo.class)))
                .thenReturn(500L);
        when(mStorageManagerVolumeProvider.getFreeBytes(
                        nullable(StorageStatsManager.class), nullable(VolumeInfo.class)))
                .thenReturn(0L);

        ReflectionHelpers.setField(
                provider, "mStorageManagerVolumeProvider", mStorageManagerVolumeProvider);
        ReflectionHelpers.setField(provider, "mContext", RuntimeEnvironment.application);

        provider.setListening(true);

        final String percentage = NumberFormat.getPercentInstance().format(1);
        final String freeSpace = Formatter.formatFileSize(RuntimeEnvironment.application, 0);
        verify(loader).setSummary(provider,
                RuntimeEnvironment.application.getString(
                        R.string.storage_summary, percentage, freeSpace));
    }
}
