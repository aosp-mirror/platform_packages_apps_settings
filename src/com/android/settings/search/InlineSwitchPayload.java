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

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;

/**
 * Payload for inline Switch results. Mappings from integer to boolean.
 */
public class InlineSwitchPayload extends InlinePayload {

    /**
     * Provides a mapping for how switches are stored.
     * If mIsStandard is true, then (0 == false) and (1 == true)
     * If mIsStandard is false, then (1 == false) and (0 == true)
     */
    private boolean mIsStandard;

    /**
     *
     * @param key uniquely identifies the stored setting.
     * @param source of the setting. Used to determine where to get and set the setting.
     * @param onValue is the value stored as on for the switch. Should be 0 or 1.
     * @param intent to the setting page.
     * @param isDeviceSupported is true when the setting is valid for the given device.
     */
    public InlineSwitchPayload(String key, @SettingsSource int source,
            int onValue, Intent intent, boolean isDeviceSupported) {
        super(key, PayloadType.INLINE_SWITCH, source, intent, isDeviceSupported);
        // If on is stored as TRUE then the switch is standard.
        mIsStandard = onValue == TRUE;
    }

    private InlineSwitchPayload(Parcel in) {
        super(in);
        mIsStandard = in.readInt() == TRUE;
    }

    @Override
    public int getType() {
        return mInlineType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mIsStandard ? TRUE : FALSE);
    }

    public static final Parcelable.Creator<InlineSwitchPayload> CREATOR =
            new Parcelable.Creator<InlineSwitchPayload>() {
        @Override
        public InlineSwitchPayload createFromParcel(Parcel in) {
            return new InlineSwitchPayload(in);
        }

        @Override
        public InlineSwitchPayload[] newArray(int size) {
            return new InlineSwitchPayload[size];
        }
    };

    @Override
    public int getValue(Context context) {
        int settingsValue = -1;
        switch(mSettingSource) {
            case SettingsSource.SECURE:
                settingsValue = Settings.Secure.getInt(context.getContentResolver(),
                        mSettingKey, -1);
                break;
            case SettingsSource.SYSTEM:
                settingsValue = Settings.System.getInt(context.getContentResolver(),
                        mSettingKey, -1);
                break;

            case SettingsSource.GLOBAL:
                settingsValue = Settings.Global.getInt(context.getContentResolver(),
                        mSettingKey, -1);
                break;
        }

        if (settingsValue == -1) {
            throw new IllegalStateException("Unable to find setting from uri: "
                    + mSettingKey.toString());
        }

        settingsValue = standardizeInput(settingsValue);

        return settingsValue;
    }

    @Override
    public boolean setValue(Context context, int newValue) {
        if (newValue != 0 && newValue != 1) {
            throw new IllegalArgumentException("newValue should be 0 for off and 1 for on."
                    + "The passed value was: " + newValue);
        }

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

    public boolean isStandard() {
        return mIsStandard;
    }

    private int standardizeInput(int value) {
        return mIsStandard
                ? value
                : 1 - value;
    }
}
