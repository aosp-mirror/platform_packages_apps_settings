/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class StorageEntryTest {

    private static final String VOLUME_INFO_ID = "volume_info_id";
    private static final String DISK_INFO_ID = "disk_info_id";
    private static final String VOLUME_RECORD_UUID = "volume_record_id";

    @Mock
    private VolumeInfo mVolumeInfo;
    @Mock
    private DiskInfo mDiskInfo;
    @Mock
    private VolumeRecord mVolumeRecord;

    private Context mContext;
    @Mock
    private StorageManager mStorageManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(StorageManager.class)).thenReturn(mStorageManager);
    }

    @Test
    public void equals_volumesOfSameId_shouldBeTheSame() {
        final StorageEntry volumeStorage1 = new StorageEntry(mContext,
                new VolumeInfo(VOLUME_INFO_ID, 0 /* type */, null /* disk */, null /* partGuid */));
        final StorageEntry volumeStorage2 = new StorageEntry(mContext,
                new VolumeInfo(VOLUME_INFO_ID, 0 /* type */, null /* disk */, null /* partGuid */));
        final StorageEntry diskStorage1 =
                new StorageEntry(new DiskInfo(DISK_INFO_ID, 0 /* flags */));
        final StorageEntry diskStorage2 =
                new StorageEntry(new DiskInfo(DISK_INFO_ID, 0 /* flags */));
        final StorageEntry volumeRecordStorage1 = new StorageEntry(new VolumeRecord(0 /* flags */,
                VOLUME_RECORD_UUID));
        final StorageEntry volumeRecordStorage2 = new StorageEntry(new VolumeRecord(0 /* flags */,
                VOLUME_RECORD_UUID));

        assertThat(Objects.equals(volumeStorage1, volumeStorage2)).isTrue();
        assertThat(Objects.equals(diskStorage1, diskStorage2)).isTrue();
        assertThat(Objects.equals(volumeRecordStorage1, volumeRecordStorage2)).isTrue();
    }

    @Test
    public void equals_volumesOfDifferentId_shouldBeDifferent() {
        final StorageEntry volumeStorage1 = new StorageEntry(mContext,
                new VolumeInfo(VOLUME_INFO_ID, 0 /* type */, null /* disk */, null /* partGuid */));
        final StorageEntry volumeStorage2 = new StorageEntry(mContext,
                new VolumeInfo("id2", 0 /* type */, null /* disk */, null /* partGuid */));
        final StorageEntry diskStorage1 =
                new StorageEntry(new DiskInfo(DISK_INFO_ID, 0 /* flags */));
        final StorageEntry diskStorage2 =
                new StorageEntry(new DiskInfo("id2", 0 /* flags */));
        final StorageEntry volumeRecordStorage1 = new StorageEntry(new VolumeRecord(0 /* flags */,
                VOLUME_RECORD_UUID));
        final StorageEntry volumeRecordStorage2 = new StorageEntry(new VolumeRecord(0 /* flags */,
                "id2"));

        assertThat(Objects.equals(volumeStorage1, volumeStorage2)).isFalse();
        assertThat(Objects.equals(diskStorage1, diskStorage2)).isFalse();
        assertThat(Objects.equals(volumeRecordStorage1, volumeRecordStorage2)).isFalse();
    }

    @Test
    public void compareTo_defaultInternalStorage_shouldBeAtTopMost() {
        final StorageEntry storage1 = mock(StorageEntry.class);
        when(storage1.isDefaultInternalStorage()).thenReturn(true);
        final StorageEntry storage2 = mock(StorageEntry.class);
        when(storage2.isDefaultInternalStorage()).thenReturn(false);

        assertThat(storage1.compareTo(storage2) > 0).isTrue();
    }

    @Test
    public void getDefaultInternalStorageEntry_shouldReturnVolumeInfoStorageOfIdPrivateInternal() {
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(mStorageManager.findVolumeById(VolumeInfo.ID_PRIVATE_INTERNAL)).thenReturn(volumeInfo);

        assertThat(StorageEntry.getDefaultInternalStorageEntry(mContext))
                .isEqualTo(new StorageEntry(mContext, volumeInfo));
    }

    @Test
    public void isVolumeInfo_shouldReturnTrueForVolumeInfo() {
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        final StorageEntry storage = new StorageEntry(mContext, volumeInfo);

        assertThat(storage.isVolumeInfo()).isTrue();
        assertThat(storage.isDiskInfoUnsupported()).isFalse();
        assertThat(storage.isVolumeRecordMissed()).isFalse();
    }

    @Test
    public void isDiskInfoUnsupported_shouldReturnTrueForDiskInfo() {
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry storage = new StorageEntry(diskInfo);

        assertThat(storage.isVolumeInfo()).isFalse();
        assertThat(storage.isDiskInfoUnsupported()).isTrue();
        assertThat(storage.isVolumeRecordMissed()).isFalse();
    }

    @Test
    public void isVolumeRecordMissed_shouldReturnTrueForVolumeRecord() {
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry storage = new StorageEntry(volumeRecord);

        assertThat(storage.isVolumeInfo()).isFalse();
        assertThat(storage.isDiskInfoUnsupported()).isFalse();
        assertThat(storage.isVolumeRecordMissed()).isTrue();
    }

    @Test
    public void isMounted_mountedOrMountedReadOnly_shouldReturnTrue() {
        final VolumeInfo mountedVolumeInfo1 = mock(VolumeInfo.class);
        final StorageEntry mountedStorage1 = new StorageEntry(mContext, mountedVolumeInfo1);
        when(mountedVolumeInfo1.getState()).thenReturn(VolumeInfo.STATE_MOUNTED);
        final VolumeInfo mountedVolumeInfo2 = mock(VolumeInfo.class);
        when(mountedVolumeInfo2.getState()).thenReturn(VolumeInfo.STATE_MOUNTED_READ_ONLY);
        final StorageEntry mountedStorage2 = new StorageEntry(mContext, mountedVolumeInfo2);

        assertThat(mountedStorage1.isMounted()).isTrue();
        assertThat(mountedStorage2.isMounted()).isTrue();
    }

    @Test
    public void isMounted_nonVolumeInfo_shouldReturnFalse() {
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage2 = new StorageEntry(volumeRecord);

        assertThat(diskStorage.isMounted()).isFalse();
        assertThat(recordStorage2.isMounted()).isFalse();
    }

    @Test
    public void isUnmountable_unmountableVolume_shouldReturnTrue() {
        final VolumeInfo unmountableVolumeInfo = mock(VolumeInfo.class);
        final StorageEntry mountedStorage = new StorageEntry(mContext, unmountableVolumeInfo);
        when(unmountableVolumeInfo.getState()).thenReturn(VolumeInfo.STATE_UNMOUNTABLE);

        assertThat(mountedStorage.isUnmountable()).isTrue();
    }

    @Test
    public void isUnmountable_nonVolumeInfo_shouldReturnFalse() {
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage2 = new StorageEntry(volumeRecord);

        assertThat(diskStorage.isUnmountable()).isFalse();
        assertThat(recordStorage2.isUnmountable()).isFalse();
    }

    @Test
    public void isPrivate_privateVolume_shouldReturnTrue() {
        final VolumeInfo privateVolumeInfo = mock(VolumeInfo.class);
        final StorageEntry privateStorage = new StorageEntry(mContext, privateVolumeInfo);
        when(privateVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);

        assertThat(privateStorage.isPrivate()).isTrue();
    }

    @Test
    public void isPublic_prublicVolume_shouldReturnTrue() {
        final VolumeInfo publicVolumeInfo = mock(VolumeInfo.class);
        final StorageEntry publicStorage = new StorageEntry(mContext, publicVolumeInfo);
        when(publicVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PUBLIC);

        assertThat(publicStorage.isPublic()).isTrue();
    }

    @Test
    public void isPrivate_nonVolumeInfo_shouldReturnFalse() {
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage2 = new StorageEntry(volumeRecord);

        assertThat(diskStorage.isPrivate()).isFalse();
        assertThat(recordStorage2.isPrivate()).isFalse();
    }

    @Test
    public void getDescription_shouldReturnDescription() {
        final String description = "description";
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(mStorageManager.getBestVolumeDescription(volumeInfo)).thenReturn(description);
        final StorageEntry volumeStorage = new StorageEntry(mContext, volumeInfo);
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        when(diskInfo.getDescription()).thenReturn(description);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage = new StorageEntry(volumeRecord);
        when(volumeRecord.getNickname()).thenReturn(description);

        assertThat(volumeStorage.getDescription()).isEqualTo(description);
        assertThat(diskStorage.getDescription()).isEqualTo(description);
        assertThat(recordStorage.getDescription()).isEqualTo(description);
    }

    @Test
    public void getDescription_defaultInternalStorage_returnThisDevice() {
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        when(volumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(volumeInfo.getId()).thenReturn(VolumeInfo.ID_PRIVATE_INTERNAL);
        final StorageEntry volumeStorage = new StorageEntry(mContext, volumeInfo);

        assertThat(volumeStorage.getDescription()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "storage_default_internal_storage"));
    }

    @Test
    public void getDiskId_shouldReturnDiskId() {
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        final StorageEntry volumeStorage = new StorageEntry(mContext, volumeInfo);
        when(volumeInfo.getDiskId()).thenReturn(VOLUME_INFO_ID);
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        when(diskInfo.getId()).thenReturn(DISK_INFO_ID);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage = new StorageEntry(volumeRecord);

        assertThat(volumeStorage.getDiskId()).isEqualTo(VOLUME_INFO_ID);
        assertThat(diskStorage.getDiskId()).isEqualTo(DISK_INFO_ID);
        assertThat(recordStorage.getDiskId()).isEqualTo(null);
    }

    @Test
    public void getFsUuid_shouldReturnFsUuid() {
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        final StorageEntry volumeStorage = new StorageEntry(mContext, volumeInfo);
        when(volumeInfo.getFsUuid()).thenReturn(VOLUME_INFO_ID);
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage = new StorageEntry(volumeRecord);
        when(volumeRecord.getFsUuid()).thenReturn(VOLUME_RECORD_UUID);

        assertThat(volumeStorage.getFsUuid()).isEqualTo(VOLUME_INFO_ID);
        assertThat(diskStorage.getFsUuid()).isEqualTo(null);
        assertThat(recordStorage.getFsUuid()).isEqualTo(VOLUME_RECORD_UUID);
    }

    @Test
    public void getPath_shouldReturnPath() {
        final File file = new File("fakePath");
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        final StorageEntry volumeStorage = new StorageEntry(mContext, volumeInfo);
        when(volumeInfo.getPath()).thenReturn(file);
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage = new StorageEntry(volumeRecord);

        assertThat(volumeStorage.getPath()).isEqualTo(file);
        assertThat(diskStorage.getPath()).isEqualTo(null);
        assertThat(recordStorage.getPath()).isEqualTo(null);
    }

    @Test
    public void getVolumeInfo_shouldVolumeInfo() {
        final VolumeInfo volumeInfo = mock(VolumeInfo.class);
        final StorageEntry volumeStorage = new StorageEntry(mContext, volumeInfo);
        final DiskInfo diskInfo = mock(DiskInfo.class);
        final StorageEntry diskStorage = new StorageEntry(diskInfo);
        final VolumeRecord volumeRecord = mock(VolumeRecord.class);
        final StorageEntry recordStorage = new StorageEntry(volumeRecord);

        assertThat(volumeStorage.getVolumeInfo()).isEqualTo(volumeInfo);
        assertThat(diskStorage.getVolumeInfo()).isEqualTo(null);
        assertThat(recordStorage.getVolumeInfo()).isEqualTo(null);
    }
}
