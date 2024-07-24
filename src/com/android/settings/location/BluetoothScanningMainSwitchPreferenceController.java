/*
 * Copyright 2021 The Android Open Source Project
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
package com.android.settings.location;

import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;

/**
 * Preference controller for Bluetooth scanning main switch.
 */
public class BluetoothScanningMainSwitchPreferenceController extends TogglePreferenceController
        implements OnCheckedChangeListener {

    private static final String KEY_BLUETOOTH_SCANNING_SWITCH = "bluetooth_always_scanning_switch";
    private final UserManager mUserManager;

    public BluetoothScanningMainSwitchPreferenceController(Context context) {
        super(context, KEY_BLUETOOTH_SCANNING_SWITCH);
        mUserManager = UserManager.get(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        MainSwitchPreference pref = screen.findPreference(getPreferenceKey());
        pref.addOnSwitchChangeListener(this);
        pref.updateStatus(isChecked());
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_location_scanning)
                ? (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_LOCATION)
                        ? DISABLED_DEPENDENT_SETTING
                        : AVAILABLE)
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, isChecked ? 1 : 0);
        // Returning true means the underlying setting is updated.
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_location;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != isChecked()) {
            setChecked(isChecked);
        }
    }
}
