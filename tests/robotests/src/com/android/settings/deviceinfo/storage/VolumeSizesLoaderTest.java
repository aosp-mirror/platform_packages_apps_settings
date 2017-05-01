package com.android.settings.deviceinfo.storage;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.storage.VolumeInfo;

import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

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
