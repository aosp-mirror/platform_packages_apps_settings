/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search2;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;

import java.util.Map;

/**
 * Payload for inline Switch results. Mappings from integer to boolean.
 */
public class InlineSwitchPayload extends InlinePayload {
    /**
     * Maps Inline values to UI-consumable Values.
     * For example, if you have a switch preference whose values are stored as ints, the two valid
     * list of mappings would be:
     * < (0,True), (1, false) >
     * < (1,True), (0, false) >
     */
    public final Map<Integer, Boolean> valueMap;

    public InlineSwitchPayload(String newUri, @SettingsSource int settingsSource,
            Map<Integer, Boolean> map) {
        super(newUri, PayloadType.INLINE_SWITCH, settingsSource);
        valueMap = map;
    }

    private InlineSwitchPayload(Parcel in) {
        super(in.readString() /* Uri */ , in.readInt() /* Payload Type */,
                in.readInt() /* Settings Source */);
        valueMap = in.readHashMap(Integer.class.getClassLoader());
    }

    @Override
    public int getType() {
        return inlineType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(settingsUri);
        dest.writeInt(inlineType);
        dest.writeInt(settingSource);
        dest.writeMap(valueMap);
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

    public boolean getSwitchValue(Context context) {
        if (valueMap == null) {
            throw new IllegalStateException("Value map is null");
        }

        int settingsValue = -1;
        switch(settingSource) {
            case SettingsSource.SECURE:
                settingsValue = Settings.Secure.getInt(context.getContentResolver(),
                        settingsUri, 0);
                break;
            case SettingsSource.SYSTEM:
                settingsValue = Settings.System.getInt(context.getContentResolver(),
                        settingsUri, 0);
                break;

            case SettingsSource.GLOBAL:
                settingsValue = Settings.Global.getInt(context.getContentResolver(),
                        settingsUri, 0);
                break;
        }

        if (settingsValue == -1) {
            throw new IllegalStateException("Unable to find setting from uri: "
                    + settingsUri.toString());
        }

        for (Integer key : valueMap.keySet()) {
            if ((key == settingsValue)) {
                return valueMap.get(key);
            }
        }

        throw new IllegalStateException("No results matched the key: " + settingsValue);
    }

    public void setSwitchValue(Context context, boolean isChecked) {
        if (valueMap == null) {
            throw new IllegalStateException("Value map is null");
        }
        int switchValue = -1;

        for (Map.Entry<Integer, Boolean> pair : valueMap.entrySet()) {
            if (pair.getValue() == isChecked) {
                switchValue = pair.getKey();
                break;
            }
        }

        if (switchValue == -1) {
            throw new IllegalStateException("Switch value is not set");
        }

        switch(settingSource) {
            case SettingsSource.GLOBAL:
                Settings.Global.putInt(context.getContentResolver(), settingsUri, switchValue);
                return;
            case SettingsSource.SECURE:
                Settings.Secure.putInt(context.getContentResolver(), settingsUri, switchValue);
                return;
            case SettingsSource.SYSTEM:
                Settings.System.putInt(context.getContentResolver(), settingsUri, switchValue);
                return;
            case SettingsSource.UNKNOWN:
                return;
        }
    }
}
