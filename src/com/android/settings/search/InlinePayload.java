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
import android.provider.Settings;
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
     * Defines where the Setting is stored.
     */
    @SettingsSource final int mSettingSource;

    /**
     * True when the setting is available for the device.
     */
    final boolean mIsDeviceSupported;

    /**
     * The default value for the setting.
     */
    final int mDefaultvalue;

    /**
     * @param key uniquely identifies the stored setting.
     * @param source of the setting. Used to determine where to get and set the setting.
     * @param intent to the setting page.
     * @param isDeviceSupported is true when the setting is valid for the given device.
     */
    public InlinePayload(String key, @SettingsSource int source, Intent intent,
            boolean isDeviceSupported, int defaultValue) {
        super(intent);
        mSettingKey = key;
        mSettingSource = source;
        mIsDeviceSupported = isDeviceSupported;
        mDefaultvalue = defaultValue;
    }

    InlinePayload(Parcel parcel) {
        super(parcel.readParcelable(Intent.class.getClassLoader()));
        mSettingKey = parcel.readString();
        mSettingSource = parcel.readInt();
        mIsDeviceSupported = parcel.readInt() == TRUE;
        mDefaultvalue = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mSettingKey);
        dest.writeInt(mSettingSource);
        dest.writeInt(mIsDeviceSupported ? TRUE : FALSE);
        dest.writeInt(mDefaultvalue);
    }

    @Override
    @PayloadType public abstract int getType();

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
     * Checks if the input is valid for the given setting.
     *
     * @param input The number to be get or set for the setting.
     * @return {@param input} mapped to the public-facing API for settings.
     * @throws IllegalArgumentException when the input is not valid for the given inline type.
     */
    protected abstract int standardizeInput(int input) throws IllegalArgumentException;

    /**
     * @returns the current value of the setting.
     */
    public int getValue(Context context) {
        int settingsValue = -1;
        switch(mSettingSource) {
            case SettingsSource.SECURE:
                settingsValue = Settings.Secure.getInt(context.getContentResolver(),
                        mSettingKey, mDefaultvalue);
                break;
            case SettingsSource.SYSTEM:
                settingsValue = Settings.System.getInt(context.getContentResolver(),
                        mSettingKey, mDefaultvalue);
                break;

            case SettingsSource.GLOBAL:
                settingsValue = Settings.Global.getInt(context.getContentResolver(),
                        mSettingKey, mDefaultvalue);
                break;
        }

        return standardizeInput(settingsValue);
    }

    /**
     * Attempts to set the setting value.
     *
     * @param newValue is the requested value for the setting.
     * @returns true when the setting was changed, and false otherwise.
     */
    public boolean setValue(Context context, int newValue) {
        newValue = standardizeInput(newValue);

        switch(mSettingSource) {
            case SettingsSource.GLOBAL:
                return Settings.Global.putInt(context.getContentResolver(), mSettingKey, newValue);
            case SettingsSource.SECURE:
                return Settings.Secure.putInt(context.getContentResolver(), mSettingKey, newValue);
            case SettingsSource.SYSTEM:
                return Settings.System.putInt(context.getContentResolver(), mSettingKey, newValue);
            case SettingsSource.UNKNOWN:
                return false;
        }

        return false;
    }
}