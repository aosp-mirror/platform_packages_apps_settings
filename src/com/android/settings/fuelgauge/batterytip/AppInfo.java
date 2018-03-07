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

package com.android.settings.fuelgauge.batterytip;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.anomaly.Anomaly;

/**
 * Model class stores app info(e.g. package name, type..) that used in battery tip
 */
public class AppInfo implements Comparable<AppInfo>, Parcelable {
    public final String packageName;
    /**
     * Anomaly type of the app
     * @see Anomaly.AnomalyType
     */
    public final int anomalyType;
    public final long screenOnTimeMs;
    public final int uid;

    private AppInfo(AppInfo.Builder builder) {
        packageName = builder.mPackageName;
        anomalyType = builder.mAnomalyType;
        screenOnTimeMs = builder.mScreenOnTimeMs;
        uid = builder.mUid;
    }

    @VisibleForTesting
    AppInfo(Parcel in) {
        packageName = in.readString();
        anomalyType = in.readInt();
        screenOnTimeMs = in.readLong();
        uid = in.readInt();
    }

    @Override
    public int compareTo(AppInfo o) {
        return Long.compare(screenOnTimeMs, o.screenOnTimeMs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeInt(anomalyType);
        dest.writeLong(screenOnTimeMs);
        dest.writeInt(uid);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AppInfo createFromParcel(Parcel in) {
            return new AppInfo(in);
        }

        public AppInfo[] newArray(int size) {
            return new AppInfo[size];
        }
    };

    public static final class Builder {
        private int mAnomalyType;
        private String mPackageName;
        private long mScreenOnTimeMs;
        private int mUid;

        public Builder setAnomalyType(int type) {
            mAnomalyType = type;
            return this;
        }

        public Builder setPackageName(String packageName) {
            mPackageName = packageName;
            return this;
        }

        public Builder setScreenOnTimeMs(long screenOnTimeMs) {
            mScreenOnTimeMs = screenOnTimeMs;
            return this;
        }

        public Builder setUid(int uid) {
            mUid = uid;
            return this;
        }

        public AppInfo build() {
            return new AppInfo(this);
        }
    }
}