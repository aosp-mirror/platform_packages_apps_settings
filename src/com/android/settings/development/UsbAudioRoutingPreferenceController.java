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

package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class UsbAudioRoutingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String USB_AUDIO_KEY = "usb_audio";

    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    private SwitchPreference mPreference;

    public UsbAudioRoutingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return USB_AUDIO_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int usbAudioRoutingMode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, SETTING_VALUE_OFF);
        mPreference.setChecked(usbAudioRoutingMode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, SETTING_VALUE_OFF);
        mPreference.setChecked(false);
        mPreference.setEnabled(false);
    }
}
