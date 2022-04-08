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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.util.SparseArray;

import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settingslib.applications.StorageStatsSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class StorageProfileFragmentTest {

    @Captor
    private ArgumentCaptor<SparseArray<StorageAsyncLoader.AppsStorageResult>> mCaptor;

    @Test
    public void verifyAppSizesAreNotZeroedOut() {
        StorageItemPreferenceController controller = mock(StorageItemPreferenceController.class);
        StorageProfileFragment fragment = new StorageProfileFragment();
        StorageAsyncLoader.AppsStorageResult result = new StorageAsyncLoader.AppsStorageResult();
        result.musicAppsSize = 100;
        result.otherAppsSize = 200;
        result.gamesSize = 300;
        result.videoAppsSize = 400;
        result.externalStats = new StorageStatsSource.ExternalStorageStats(6, 1, 2, 3, 0);
        SparseArray<StorageAsyncLoader.AppsStorageResult> resultsArray = new SparseArray<>();
        resultsArray.put(0, result);
        fragment.setPreferenceController(controller);

        fragment.onLoadFinished(null, resultsArray);

        MockitoAnnotations.initMocks(this);
        verify(controller).onLoadFinished(mCaptor.capture(), anyInt());

        StorageAsyncLoader.AppsStorageResult extractedResult = mCaptor.getValue().get(0);
        assertThat(extractedResult.musicAppsSize).isEqualTo(100);
        assertThat(extractedResult.videoAppsSize).isEqualTo(400);
        assertThat(extractedResult.otherAppsSize).isEqualTo(200);
        assertThat(extractedResult.gamesSize).isEqualTo(300);
        assertThat(extractedResult.externalStats.audioBytes).isEqualTo(1);
        assertThat(extractedResult.externalStats.videoBytes).isEqualTo(2);
        assertThat(extractedResult.externalStats.imageBytes).isEqualTo(3);
        assertThat(extractedResult.externalStats.totalBytes).isEqualTo(6);
    }
}
