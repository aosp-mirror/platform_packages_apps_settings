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
 *
 */

package com.android.settings.search;

import android.content.Intent;

import android.content.Context;
import android.os.Parcel;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Abstract Payload for inline settings results.
 */
public abstract class InlinePayload extends ResultPayload {

    public static final int FALSE = 0;
    public static final int TRUE = 1;

    /**
     * Defines the key to access and store the Setting the inline result represents.
     */
    @VisibleForTesting
    final String mSettingKey;

    /**
     * The UI type for the inline result.
     */
    @PayloadType final int mInlineType;

    /**
     * Defines where the Setting is stored.
     */
    @SettingsSource final int mSettingSource;

    /**
     * True when the setting is available for the device.
     */
    final boolean mIsDeviceSupported;

    /**
     * @param key uniquely identifies the stored setting.
     * @param payloadType of the setting being stored.
     * @param source of the setting. Used to determine where to get and set the setting.
     * @param intent to the setting page.
     * @param isDeviceSupported is true when the setting is valid for the given device.
     */
    public InlinePayload(String key, @PayloadType int payloadType, @SettingsSource int source,
            Intent intent, boolean isDeviceSupported) {
        super(intent);
        mSettingKey = key;
        mInlineType = payloadType;
        mSettingSource = source;
        mIsDeviceSupported = isDeviceSupported;
    }

    InlinePayload(Parcel parcel) {
        super((Intent) parcel.readParcelable(Intent.class.getClassLoader()));
        mSettingKey = parcel.readString();
        mInlineType = parcel.readInt();
        mSettingSource = parcel.readInt();
        mIsDeviceSupported = parcel.readInt() == TRUE;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mSettingKey);
        dest.writeInt(mInlineType);
        dest.writeInt(mSettingSource);
        dest.writeInt(mIsDeviceSupported ? TRUE : FALSE);
    }

    /**
     * @returns the status of the underlying setting. See {@link ResultPayload.Availability} for
     * possible values.
     */
    @Availability public int getAvailability() {
        if (mIsDeviceSupported) {
            return Availability.AVAILABLE;
        }
        return Availability.DISABLED_UNSUPPORTED;
    }

    /**
     * @returns the current value of the setting.
     */
    public abstract int getValue(Context context);

    /**
     * Attempts to set the setting value.
     *
     * @param newValue is the requested new value for the setting.
     * @returns true when the setting was changed, and false otherwise.
     */
    public abstract boolean setValue(Context context, int newValue);
}