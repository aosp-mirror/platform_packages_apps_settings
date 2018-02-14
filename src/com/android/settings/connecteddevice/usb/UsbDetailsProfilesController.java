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

package com.android.settings.connecteddevice.usb;

import com.android.settings.R;
import com.android.settings.Utils;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import java.util.List;

/**
 * This class adds switches for toggling individual USB options, such as "transfer files",
 * "supply power", "usb tethering", etc.
 */
public class UsbDetailsProfilesController extends UsbDetailsController
        implements Preference.OnPreferenceClickListener {

    static final String KEY_POWER = "power";

    private PreferenceCategory mProfilesContainer;
    private List<String> mOptions;
    private String mKey;

    public UsbDetailsProfilesController(Context context, PreferenceFragment fragment,
            UsbBackend backend, List<String> options, String key) {
        super(context, fragment, backend);
        mOptions = options;
        mKey = key;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mProfilesContainer = (PreferenceCategory) screen.findPreference(getPreferenceKey());
    }

    /**
     * Gets a switch preference for the particular option, creating it if needed.
     */
    private SwitchPreference getProfilePreference(String key, int titleId) {
        SwitchPreference pref = (SwitchPreference) mProfilesContainer.findPreference(key);
        if (pref == null) {
            pref = new SwitchPreference(mProfilesContainer.getContext());
            pref.setKey(key);
            pref.setTitle(titleId);
            pref.setOnPreferenceClickListener(this);
            mProfilesContainer.addPreference(pref);
        }
        return pref;
    }

    @Override
    protected void refresh(int mode) {
        SwitchPreference pref;
        for (String option : mOptions) {
            int newMode;
            int summary = -1;
            int title;
            if (option.equals(UsbManager.USB_FUNCTION_MTP)) {
                newMode = UsbBackend.MODE_DATA_MTP;
                title = R.string.usb_use_file_transfers;
            } else if (option.equals(KEY_POWER)) {
                newMode = UsbBackend.MODE_POWER_SOURCE;
                title = R.string.usb_use_power_only;
                summary = R.string.usb_use_power_only_desc;
            } else if (option.equals(UsbManager.USB_FUNCTION_PTP)) {
                newMode = UsbBackend.MODE_DATA_PTP;
                title = R.string.usb_use_photo_transfers;
            } else if (option.equals(UsbManager.USB_FUNCTION_MIDI)) {
                newMode = UsbBackend.MODE_DATA_MIDI;
                title = R.string.usb_use_MIDI;
            } else if (option.equals(UsbManager.USB_FUNCTION_RNDIS)) {
                newMode = UsbBackend.MODE_DATA_TETHER;
                title = R.string.usb_use_tethering;
            } else {
                continue;
            }

            pref = getProfilePreference(option, title);
            // Only show supported and allowed options
            if (mUsbBackend.isModeSupported(newMode)
                    && !mUsbBackend.isModeDisallowedBySystem(newMode)
                    && !mUsbBackend.isModeDisallowed(newMode)) {
                if (summary != -1) {
                    pref.setSummary(summary);
                }
                pref.setChecked((mode & newMode) != 0);
            } else {
                mProfilesContainer.removePreference(pref);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        SwitchPreference profilePref = (SwitchPreference) preference;
        String key = profilePref.getKey();
        int mode = mUsbBackend.getCurrentMode();
        int thisMode = 0;
        if (key.equals(KEY_POWER)) {
            thisMode = UsbBackend.MODE_POWER_SOURCE;
        } else if (key.equals(UsbManager.USB_FUNCTION_MTP)) {
            thisMode = UsbBackend.MODE_DATA_MTP;
        } else if (key.equals(UsbManager.USB_FUNCTION_PTP)) {
            thisMode = UsbBackend.MODE_DATA_PTP;
        } else if (key.equals(UsbManager.USB_FUNCTION_RNDIS)) {
            thisMode = UsbBackend.MODE_DATA_TETHER;
        } else if (key.equals(UsbManager.USB_FUNCTION_MIDI)) {
            thisMode = UsbBackend.MODE_DATA_MIDI;
        }
        if (profilePref.isChecked()) {
            if (!key.equals(KEY_POWER)) {
                // Only one non power mode can currently be set at once.
                mode &= UsbBackend.MODE_POWER_MASK;
            }
            mode |= thisMode;
        } else {
            mode &= ~thisMode;
        }
        if (!Utils.isMonkeyRunning()) {
            mUsbBackend.setMode(mode);
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }
}
