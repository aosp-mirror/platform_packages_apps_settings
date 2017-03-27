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
 * limitations under the License
 */
package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.util.SparseArray;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settingslib.applications.StorageStatsSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StorageProfileFragmentTest {
    @Test
    public void verifyAppSizesAreZeroedOut() {
        StorageItemPreferenceController controller = mock(StorageItemPreferenceController.class);
        StorageProfileFragment fragment = new StorageProfileFragment();
        StorageAsyncLoader.AppsStorageResult result = new StorageAsyncLoader.AppsStorageResult();
        result.musicAppsSize = 100;
        result.otherAppsSize = 200;
        result.gamesSize = 300;
        result.videoAppsSize = 400;
        result.externalStats = new StorageStatsSource.ExternalStorageStats(6, 1, 2, 3);
        SparseArray<StorageAsyncLoader.AppsStorageResult> resultsArray = new SparseArray<>();
        resultsArray.put(0, result);
        fragment.setPreferenceController(controller);

        fragment.onLoadFinished(null, resultsArray);

        ArgumentCaptor<StorageAsyncLoader.AppsStorageResult> resultCaptor = ArgumentCaptor.forClass(
                StorageAsyncLoader.AppsStorageResult.class);
        verify(controller).onLoadFinished(resultCaptor.capture());

        StorageAsyncLoader.AppsStorageResult extractedResult = resultCaptor.getValue();
        assertThat(extractedResult.musicAppsSize).isEqualTo(0);
        assertThat(extractedResult.videoAppsSize).isEqualTo(0);
        assertThat(extractedResult.otherAppsSize).isEqualTo(0);
        assertThat(extractedResult.gamesSize).isEqualTo(0);
        assertThat(extractedResult.externalStats.audioBytes).isEqualTo(1);
        assertThat(extractedResult.externalStats.videoBytes).isEqualTo(2);
        assertThat(extractedResult.externalStats.imageBytes).isEqualTo(3);
        assertThat(extractedResult.externalStats.totalBytes).isEqualTo(6);
    }
}
