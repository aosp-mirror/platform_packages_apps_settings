/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.connecteddevice.virtual;

import android.companion.AssociationInfo;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.Objects;

/** Parcelable representing a virtual device along with its association properties. */
class VirtualDeviceWrapper implements Parcelable {

    /** The CDM Association for this device. */
    @NonNull
    private final AssociationInfo mAssociationInfo;
    /** The unique VDM identifier for the device, persisted even when the device is inactive. */
    @NonNull
    private final String mPersistentDeviceId;
    /** The identifier for the device if it's active, Context.DEVICE_ID_INVALID otherwise. */
    private int mDeviceId;

    VirtualDeviceWrapper(@NonNull AssociationInfo associationInfo,
            @NonNull String persistentDeviceId, int deviceId) {
        mAssociationInfo = associationInfo;
        mPersistentDeviceId = persistentDeviceId;
        mDeviceId = deviceId;
    }

    @NonNull
    AssociationInfo getAssociationInfo() {
        return mAssociationInfo;
    }

    @NonNull
    String getPersistentDeviceId() {
        return mPersistentDeviceId;
    }

    @NonNull
    CharSequence getDeviceName(Context context) {
        return mAssociationInfo.getDisplayName() != null
                ? mAssociationInfo.getDisplayName()
                : context.getString(R.string.virtual_device_unknown);
    }

    int getDeviceId() {
        return mDeviceId;
    }

    void setDeviceId(int deviceId) {
        mDeviceId = deviceId;
    }

    private VirtualDeviceWrapper(Parcel in) {
        mAssociationInfo = in.readTypedObject(AssociationInfo.CREATOR);
        mPersistentDeviceId = in.readString8();
        mDeviceId = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedObject(mAssociationInfo, flags);
        dest.writeString8(mPersistentDeviceId);
        dest.writeInt(mDeviceId);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof VirtualDeviceWrapper that)) return false;
        return Objects.equals(mAssociationInfo, that.mAssociationInfo)
                && Objects.equals(mPersistentDeviceId, that.mPersistentDeviceId)
                && mDeviceId == that.mDeviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAssociationInfo, mPersistentDeviceId, mDeviceId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<VirtualDeviceWrapper> CREATOR =
            new Parcelable.Creator<>() {
                @NonNull
                public VirtualDeviceWrapper createFromParcel(@NonNull Parcel in) {
                    return new VirtualDeviceWrapper(in);
                }

                @NonNull
                public VirtualDeviceWrapper[] newArray(int size) {
                    return new VirtualDeviceWrapper[size];
                }
            };
}
