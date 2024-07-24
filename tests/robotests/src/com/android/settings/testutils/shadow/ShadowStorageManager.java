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

package com.android.settings.testutils.shadow;

import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;

import androidx.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.ArrayList;
import java.util.List;

@Implements(StorageManager.class)
public class ShadowStorageManager extends org.robolectric.shadows.ShadowStorageManager {

    private static boolean sIsUnmountCalled;
    private static boolean sIsForgetCalled;
    private static boolean sIsFileEncrypted = true;

    public static boolean isUnmountCalled() {
        return sIsUnmountCalled;
    }

    public static boolean isForgetCalled() {
        return sIsForgetCalled;
    }

    public @NonNull List<VolumeInfo> getVolumes() {
        return new ArrayList<VolumeInfo>();
    }

    @Resetter
    public static void reset() {
        sIsUnmountCalled = false;
        sIsForgetCalled = false;
        sIsFileEncrypted = true;
    }

    @Implementation
    protected VolumeInfo findVolumeById(String id) {
        return createVolumeInfo(id);
    }

    @Implementation
    protected DiskInfo findDiskById(String id) {
        return new DiskInfo(id, DiskInfo.FLAG_SD);
    }

    @Implementation
    protected VolumeRecord findRecordByUuid(String fsUuid) {
        return createVolumeRecord(fsUuid);
    }

    @Implementation
    protected void unmount(String volId) {
        sIsUnmountCalled = true;
    }

    @Implementation
    protected void forgetVolume(String fsUuid) {
        sIsForgetCalled = true;
    }

    @Implementation
    protected static boolean isFileEncrypted() {
        return sIsFileEncrypted;
    }

    public static void setIsFileEncrypted(boolean encrypted) {
        sIsFileEncrypted = encrypted;
    }

    private VolumeInfo createVolumeInfo(String volumeId) {
        final DiskInfo disk = new DiskInfo("fakeid", 0);
        return new VolumeInfo(volumeId, 0, disk, "guid");
    }

    private VolumeRecord createVolumeRecord(String fsUuid) {
        VolumeRecord record = new VolumeRecord(VolumeRecord.USER_FLAG_INITED, fsUuid);
        record.nickname = "nickname";
        return record;
    }
}
