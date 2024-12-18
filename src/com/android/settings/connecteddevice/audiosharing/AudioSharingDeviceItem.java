/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.os.Parcel;
import android.os.Parcelable;

public final class AudioSharingDeviceItem implements Parcelable {
    private final String mName;
    private final int mGroupId;
    private final boolean mIsActive;

    public AudioSharingDeviceItem(String name, int groupId, boolean isActive) {
        mName = name;
        mGroupId = groupId;
        mIsActive = isActive;
    }

    public String getName() {
        return mName;
    }

    public int getGroupId() {
        return mGroupId;
    }

    public boolean isActive() {
        return mIsActive;
    }

    public AudioSharingDeviceItem(Parcel in) {
        mName = in.readString();
        mGroupId = in.readInt();
        mIsActive = in.readBoolean();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeInt(mGroupId);
        dest.writeBoolean(mIsActive);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AudioSharingDeviceItem> CREATOR =
            new Creator<AudioSharingDeviceItem>() {
                @Override
                public AudioSharingDeviceItem createFromParcel(Parcel in) {
                    return new AudioSharingDeviceItem(in);
                }

                @Override
                public AudioSharingDeviceItem[] newArray(int size) {
                    return new AudioSharingDeviceItem[size];
                }
            };
}
