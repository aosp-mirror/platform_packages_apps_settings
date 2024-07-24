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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.settings.R;

import java.io.File;

/**
 * This object contains a {@link VolumeInfo} for a mountable storage or a {@link DiskInfo} for an
 * unsupported disk which is not able to be mounted automatically.
 */
public class StorageEntry implements Comparable<StorageEntry>, Parcelable {

    private final VolumeInfo mVolumeInfo;
    private final DiskInfo mUnsupportedDiskInfo;
    private final VolumeRecord mMissingVolumeRecord;

    private final String mVolumeInfoDescription;

    public StorageEntry(@NonNull Context context, @NonNull VolumeInfo volumeInfo) {
        mVolumeInfo = volumeInfo;
        mUnsupportedDiskInfo = null;
        mMissingVolumeRecord = null;

        if (isDefaultInternalStorage()) {
            // Shows "This device" for default internal storage.
            mVolumeInfoDescription = context.getResources()
                    .getString(R.string.storage_default_internal_storage);
        } else {
            mVolumeInfoDescription = context.getSystemService(StorageManager.class)
                    .getBestVolumeDescription(mVolumeInfo);
        }
    }

    public StorageEntry(@NonNull DiskInfo diskInfo) {
        mVolumeInfo = null;
        mUnsupportedDiskInfo = diskInfo;
        mMissingVolumeRecord = null;
        mVolumeInfoDescription = null;
    }

    public StorageEntry(@NonNull VolumeRecord volumeRecord) {
        mVolumeInfo = null;
        mUnsupportedDiskInfo = null;
        mMissingVolumeRecord = volumeRecord;
        mVolumeInfoDescription = null;
    }

    private StorageEntry(Parcel in) {
        mVolumeInfo = in.readParcelable(VolumeInfo.class.getClassLoader());
        mUnsupportedDiskInfo = in.readParcelable(DiskInfo.class.getClassLoader());
        mMissingVolumeRecord = in.readParcelable(VolumeRecord.class.getClassLoader());
        mVolumeInfoDescription = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mVolumeInfo, 0 /* parcelableFlags */);
        out.writeParcelable(mUnsupportedDiskInfo, 0 /* parcelableFlags */);
        out.writeParcelable(mMissingVolumeRecord , 0 /* parcelableFlags */);
        out.writeString(mVolumeInfoDescription);
    }

    public static final Parcelable.Creator<StorageEntry> CREATOR =
            new Parcelable.Creator<StorageEntry>() {
                public StorageEntry createFromParcel(Parcel in) {
                    return new StorageEntry(in);
                }

                public StorageEntry[] newArray(int size) {
                    return new StorageEntry[size];
                }
            };

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StorageEntry)) {
            return false;
        }

        final StorageEntry StorageEntry = (StorageEntry) o;
        if (isVolumeInfo()) {
            return mVolumeInfo.equals(StorageEntry.mVolumeInfo);
        }
        if (isDiskInfoUnsupported()) {
            return mUnsupportedDiskInfo.equals(StorageEntry.mUnsupportedDiskInfo);
        }
        return mMissingVolumeRecord.equals(StorageEntry.mMissingVolumeRecord);
    }

    @Override
    public int hashCode() {
        if (isVolumeInfo()) {
            return mVolumeInfo.hashCode();
        }
        if (isDiskInfoUnsupported()) {
            return mUnsupportedDiskInfo.hashCode();
        }
        return mMissingVolumeRecord.hashCode();
    }

    @Override
    public String toString() {
        if (isVolumeInfo()) {
            return mVolumeInfo.toString();
        }
        if (isDiskInfoUnsupported()) {
            return mUnsupportedDiskInfo.toString();
        }
        return mMissingVolumeRecord.toString();
    }

    @Override
    public int compareTo(StorageEntry other) {
        if (isDefaultInternalStorage() && !other.isDefaultInternalStorage()) {
            return -1;
        }
        if (!isDefaultInternalStorage() && other.isDefaultInternalStorage()) {
            return 1;
        }

        if (isVolumeInfo() && !other.isVolumeInfo()) {
            return -1;
        }
        if (!isVolumeInfo() && other.isVolumeInfo()) {
            return 1;
        }

        if (isPrivate() && !other.isPrivate()) {
            return -1;
        }
        if (!isPrivate() && other.isPrivate()) {
            return 1;
        }

        if (isMounted() && !other.isMounted()) {
            return -1;
        }
        if (!isMounted() && other.isMounted()) {
            return 1;
        }

        if (!isVolumeRecordMissed() && other.isVolumeRecordMissed()) {
            return -1;
        }
        if (isVolumeRecordMissed() && !other.isVolumeRecordMissed()) {
            return 1;
        }

        if (getDescription() == null) {
            return 1;
        }
        if (other.getDescription() == null) {
            return -1;
        }
        return getDescription().compareTo(other.getDescription());
    }

    /** Returns default internal storage. */
    public static StorageEntry getDefaultInternalStorageEntry(Context context) {
        return new StorageEntry(context, context.getSystemService(StorageManager.class)
                .findVolumeById(VolumeInfo.ID_PRIVATE_INTERNAL));
    }

    /** If it's a VolumeInfo. */
    public boolean isVolumeInfo() {
        return mVolumeInfo != null;
    }

    /** If it's an unsupported DiskInfo. */
    public boolean isDiskInfoUnsupported() {
        return mUnsupportedDiskInfo != null;
    }

    /** If it's a missing VolumeRecord. */
    public boolean isVolumeRecordMissed() {
        return mMissingVolumeRecord != null;
    }

    /** If it's a default internal storage. */
    public boolean isDefaultInternalStorage() {
        if (isVolumeInfo()) {
            return mVolumeInfo.getType() == VolumeInfo.TYPE_PRIVATE
                    && TextUtils.equals(mVolumeInfo.getId(), VolumeInfo.ID_PRIVATE_INTERNAL);
        }
        return false;
    }

    /** If it's a mounted storage. */
    public boolean isMounted() {
        return mVolumeInfo == null ? false : (mVolumeInfo.getState() == VolumeInfo.STATE_MOUNTED
                || mVolumeInfo.getState() == VolumeInfo.STATE_MOUNTED_READ_ONLY);
    }

    /** If it's an unmounted storage. */
    public boolean isUnmounted() {
        return mVolumeInfo == null ? false : (mVolumeInfo.getState() == VolumeInfo.STATE_UNMOUNTED);
    }

    /** If it's an unmountable storage. */
    public boolean isUnmountable() {
        return mVolumeInfo == null ? false : mVolumeInfo.getState() == VolumeInfo.STATE_UNMOUNTABLE;
    }

    /** If it's a private storage. */
    public boolean isPrivate() {
        return mVolumeInfo == null ? false : mVolumeInfo.getType() == VolumeInfo.TYPE_PRIVATE;
    }

    /** If it's a public storage. */
    public boolean isPublic() {
        return mVolumeInfo == null ? false : mVolumeInfo.getType() == VolumeInfo.TYPE_PUBLIC;
    }

    /** Returns description. */
    public String getDescription() {
        if (isVolumeInfo()) {
            return mVolumeInfoDescription;
        }
        if (isDiskInfoUnsupported()) {
            return mUnsupportedDiskInfo.getDescription();
        }
        return mMissingVolumeRecord.getNickname();
    }

    /** Returns ID. */
    public String getId() {
        if (isVolumeInfo()) {
            return mVolumeInfo.getId();
        }
        if (isDiskInfoUnsupported()) {
            return mUnsupportedDiskInfo.getId();
        }
        return mMissingVolumeRecord.getFsUuid();
    }

    /** Returns disk ID. */
    public String getDiskId() {
        if (isVolumeInfo()) {
            return mVolumeInfo.getDiskId();
        }
        if (isDiskInfoUnsupported()) {
            return mUnsupportedDiskInfo.getId();
        }
        return null;
    }

    /** Returns fsUuid. */
    public String getFsUuid() {
        if (isVolumeInfo()) {
            return mVolumeInfo.getFsUuid();
        }
        if (isDiskInfoUnsupported()) {
            return null;
        }
        return mMissingVolumeRecord.getFsUuid();
    }

    /** Returns root file if it's a VolumeInfo. */
    public File getPath() {
        return mVolumeInfo == null ? null : mVolumeInfo.getPath();
    }

    /** Returns VolumeInfo of the StorageEntry. */
    public VolumeInfo getVolumeInfo() {
        return mVolumeInfo;
    }
}

