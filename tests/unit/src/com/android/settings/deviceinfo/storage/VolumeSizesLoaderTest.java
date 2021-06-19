/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.deviceinfo.storage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.storage.VolumeInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VolumeSizesLoaderTest {
    @Test
    public void getVolumeSize_getsValidSizes() throws Exception {
        VolumeInfo info = mock(VolumeInfo.class);
        StorageVolumeProvider storageVolumeProvider = mock(StorageVolumeProvider.class);
        when(storageVolumeProvider.getTotalBytes(any(), any())).thenReturn(10000L);
        when(storageVolumeProvider.getFreeBytes(any(), any())).thenReturn(1000L);

        PrivateStorageInfo storageInfo =
                VolumeSizesLoader.getVolumeSize(storageVolumeProvider, null, info);

        assertThat(storageInfo.freeBytes).isEqualTo(1000L);
        assertThat(storageInfo.totalBytes).isEqualTo(10000L);
    }
}
