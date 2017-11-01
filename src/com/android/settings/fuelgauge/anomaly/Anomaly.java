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

package com.android.settings.fuelgauge.anomaly;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Data that represents an app has been detected as anomaly. It contains
 *
 * 1. Basic information of the app(i.e. uid, package name)
 * 2. Type of anomaly
 * 3. Data that has been detected as anomaly(i.e wakelock time)
 */
public class Anomaly implements Parcelable {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AnomalyType.WAKE_LOCK,
            AnomalyType.WAKEUP_ALARM,
            AnomalyType.BLUETOOTH_SCAN})
    public @interface AnomalyType {
        int WAKE_LOCK = 0;
        int WAKEUP_ALARM = 1;
        int BLUETOOTH_SCAN = 2;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AnomalyActionType.FORCE_STOP,
            AnomalyActionType.BACKGROUND_CHECK,
            AnomalyActionType.LOCATION_CHECK,
            AnomalyActionType.STOP_AND_BACKGROUND_CHECK})
    public @interface AnomalyActionType {
        int FORCE_STOP = 0;
        int BACKGROUND_CHECK = 1;
        int LOCATION_CHECK = 2;
        int STOP_AND_BACKGROUND_CHECK = 3;
    }

    @AnomalyType
    public static final int[] ANOMALY_TYPE_LIST = {
            AnomalyType.WAKE_LOCK,
            AnomalyType.WAKEUP_ALARM,
            AnomalyType.BLUETOOTH_SCAN};

    /**
     * Type of this this anomaly
     */
    public final int type;
    public final int uid;
    public final int targetSdkVersion;
    public final long wakelockTimeMs;
    public final long bluetoothScanningTimeMs;
    public final int wakeupAlarmCount;
    /**
     * {@code true} if background restriction is enabled
     *
     * @see android.app.AppOpsManager.OP_RUN_IN_BACKGROUND
     */
    public final boolean backgroundRestrictionEnabled;
    /**
     * Display name of this anomaly, usually it is the app name
     */
    public final CharSequence displayName;
    public final String packageName;

    private Anomaly(Builder builder) {
        type = builder.mType;
        uid = builder.mUid;
        displayName = builder.mDisplayName;
        packageName = builder.mPackageName;
        wakelockTimeMs = builder.mWakeLockTimeMs;
        targetSdkVersion = builder.mTargetSdkVersion;
        backgroundRestrictionEnabled = builder.mBgRestrictionEnabled;
        bluetoothScanningTimeMs = builder.mBluetoothScanningTimeMs;
        wakeupAlarmCount = builder.mWakeupAlarmCount;
    }

    private Anomaly(Parcel in) {
        type = in.readInt();
        uid = in.readInt();
        displayName = in.readCharSequence();
        packageName = in.readString();
        wakelockTimeMs = in.readLong();
        targetSdkVersion = in.readInt();
        backgroundRestrictionEnabled = in.readBoolean();
        wakeupAlarmCount = in.readInt();
        bluetoothScanningTimeMs = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeInt(uid);
        dest.writeCharSequence(displayName);
        dest.writeString(packageName);
        dest.writeLong(wakelockTimeMs);
        dest.writeInt(targetSdkVersion);
        dest.writeBoolean(backgroundRestrictionEnabled);
        dest.writeInt(wakeupAlarmCount);
        dest.writeLong(bluetoothScanningTimeMs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Anomaly)) {
            return false;
        }

        Anomaly other = (Anomaly) obj;
        return type == other.type
                && uid == other.uid
                && wakelockTimeMs == other.wakelockTimeMs
                && TextUtils.equals(displayName, other.displayName)
                && TextUtils.equals(packageName, other.packageName)
                && targetSdkVersion == other.targetSdkVersion
                && backgroundRestrictionEnabled == other.backgroundRestrictionEnabled
                && wakeupAlarmCount == other.wakeupAlarmCount
                && bluetoothScanningTimeMs == other.bluetoothScanningTimeMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, uid, displayName, packageName, wakelockTimeMs, targetSdkVersion,
                backgroundRestrictionEnabled, wakeupAlarmCount, bluetoothScanningTimeMs);
    }

    @Override
    public String toString() {
        return "type=" + toAnomalyTypeText(type) + " uid=" + uid + " package=" + packageName +
                " displayName=" + displayName + " wakelockTimeMs=" + wakelockTimeMs +
                " wakeupAlarmCount=" + wakeupAlarmCount + " bluetoothTimeMs="
                + bluetoothScanningTimeMs;
    }

    private String toAnomalyTypeText(@AnomalyType int type) {
        switch (type) {
            case AnomalyType.WAKEUP_ALARM:
                return "wakeupAlarm";
            case AnomalyType.WAKE_LOCK:
                return "wakelock";
            case AnomalyType.BLUETOOTH_SCAN:
                return "unoptimizedBluetoothScan";
        }

        return "";
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Anomaly createFromParcel(Parcel in) {
            return new Anomaly(in);
        }

        public Anomaly[] newArray(int size) {
            return new Anomaly[size];
        }
    };

    public static final class Builder {
        @AnomalyType
        private int mType;
        private int mUid;
        private int mTargetSdkVersion;
        private CharSequence mDisplayName;
        private String mPackageName;
        private long mWakeLockTimeMs;
        private boolean mBgRestrictionEnabled;
        private int mWakeupAlarmCount;
        private long mBluetoothScanningTimeMs;

        public Builder setType(@AnomalyType int type) {
            mType = type;
            return this;
        }

        public Builder setUid(int uid) {
            mUid = uid;
            return this;
        }

        public Builder setDisplayName(CharSequence displayName) {
            mDisplayName = displayName;
            return this;
        }

        public Builder setPackageName(String packageName) {
            mPackageName = packageName;
            return this;
        }

        public Builder setWakeLockTimeMs(long wakeLockTimeMs) {
            mWakeLockTimeMs = wakeLockTimeMs;
            return this;
        }

        public Builder setTargetSdkVersion(int targetSdkVersion) {
            mTargetSdkVersion = targetSdkVersion;
            return this;
        }

        public Builder setBackgroundRestrictionEnabled(boolean bgRestrictionEnabled) {
            mBgRestrictionEnabled = bgRestrictionEnabled;
            return this;
        }

        public Builder setWakeupAlarmCount(int wakeupAlarmCount) {
            mWakeupAlarmCount = wakeupAlarmCount;
            return this;
        }

        public Builder setBluetoothScanningTimeMs(long bluetoothScanningTimeMs) {
            mBluetoothScanningTimeMs = bluetoothScanningTimeMs;
            return this;
        }

        public Anomaly build() {
            return new Anomaly(this);
        }
    }
}
