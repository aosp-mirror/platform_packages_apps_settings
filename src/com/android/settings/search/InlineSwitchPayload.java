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
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Payload for inline Switch results. Mappings from integer to boolean.
 */
public class InlineSwitchPayload extends InlinePayload {

    private static final int ON = 1;
    private static final int OFF = 0;

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
            int onValue, Intent intent, boolean isDeviceSupported, int defaultValue) {
        super(key, source, intent, isDeviceSupported, defaultValue);
        // If on is stored as TRUE then the switch is standard.
        mIsStandard = onValue == TRUE;
    }

    private InlineSwitchPayload(Parcel in) {
        super(in);
        mIsStandard = in.readInt() == TRUE;
    }

    @Override
    @PayloadType public int getType() {
        return PayloadType.INLINE_SWITCH;
    }

    @Override
    protected int standardizeInput(int value) {
        if (value != OFF && value != ON) {
            throw new IllegalArgumentException("Invalid input for InlineSwitch. Expected: "
                    + ON + " or " + OFF
                    + " but found: " + value);
        }
        return mIsStandard
                ? value
                : 1 - value;
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

    public boolean isStandard() {
        return mIsStandard;
    }
}
