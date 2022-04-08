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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;
import android.os.storage.VolumeInfo;

import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class StorageSettingsTest {

    @Mock
    private StorageManagerVolumeProvider mStorageManagerVolumeProvider;
    @Mock
    private Activity mActivity;

    private List<VolumeInfo> mVolumes;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mVolumes = new ArrayList<>();
        mVolumes.add(mock(VolumeInfo.class, RETURNS_DEEP_STUBS));
        when(mStorageManagerVolumeProvider.getVolumes()).thenReturn(mVolumes);
    }

    @Test
    public void handlePublicVolumeClick_startsANonNullActivityWhenVolumeHasNoBrowse() {
        VolumeInfo volumeInfo = mock(VolumeInfo.class, RETURNS_DEEP_STUBS);
        when(volumeInfo.isMountedReadable()).thenReturn(true);
        StorageSettings.handlePublicVolumeClick(mActivity, volumeInfo);

        verify(mActivity, never()).startActivity(null);
        verify(mActivity).startActivity(any(Intent.class));
    }

    @Test
    public void handleStubVolumeClick_startsANonNullActivityWhenVolumeHasNoBrowse() {
        VolumeInfo volumeInfo = mock(VolumeInfo.class, RETURNS_DEEP_STUBS);
        when(volumeInfo.isMountedReadable()).thenReturn(true);

        StorageSettings.handleStubVolumeClick(mActivity, volumeInfo);

        verify(mActivity, never()).startActivity(null);
        verify(mActivity).startActivity(any(Intent.class));
    }
}
