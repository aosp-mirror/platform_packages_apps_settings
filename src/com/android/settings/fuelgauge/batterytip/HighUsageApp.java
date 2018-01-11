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

/**
 * Class representing app with high screen usage
 */
public class HighUsageApp implements Comparable<HighUsageApp>, Parcelable {
    public final String packageName;
    public final long screenOnTimeMs;

    public HighUsageApp(String packageName, long screenOnTimeMs) {
        this.packageName = packageName;
        this.screenOnTimeMs = screenOnTimeMs;
    }

    private HighUsageApp(Parcel in) {
        packageName = in.readString();
        screenOnTimeMs = in.readLong();
    }

    @Override
    public int compareTo(HighUsageApp o) {
        return Long.compare(screenOnTimeMs, o.screenOnTimeMs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeLong(screenOnTimeMs);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public HighUsageApp createFromParcel(Parcel in) {
            return new HighUsageApp(in);
        }

        public HighUsageApp[] newArray(int size) {
            return new HighUsageApp[size];
        }
    };
}