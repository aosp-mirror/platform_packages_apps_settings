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

import android.content.Context;
import android.hardware.usb.UsbPort;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.RadioButtonPreference;

/**
 * This class controls the radio buttons for switching between
 * USB device and host mode.
 */
public class UsbDetailsDataRoleController extends UsbDetailsController
        implements RadioButtonPreference.OnClickListener {

    private PreferenceCategory mPreferenceCategory;
    private RadioButtonPreference mDevicePref;
    private RadioButtonPreference mHostPref;

    private RadioButtonPreference mNextRolePref;

    private final Runnable mFailureCallback = () -> {
        if (mNextRolePref != null) {
            mNextRolePref.setSummary(R.string.usb_switching_failed);
            mNextRolePref = null;
        }
    };

    public UsbDetailsDataRoleController(Context context, UsbDetailsFragment fragment,
            UsbBackend backend) {
        super(context, fragment, backend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = (PreferenceCategory) screen.findPreference(getPreferenceKey());
        mHostPref = makeRadioPreference(UsbBackend.dataRoleToString(UsbPort.DATA_ROLE_HOST),
                R.string.usb_control_host);
        mDevicePref = makeRadioPreference(UsbBackend.dataRoleToString(UsbPort.DATA_ROLE_DEVICE),
                R.string.usb_control_device);
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        if (dataRole == UsbPort.DATA_ROLE_DEVICE) {
            mDevicePref.setChecked(true);
            mHostPref.setChecked(false);
            mPreferenceCategory.setEnabled(true);
        } else if (dataRole == UsbPort.DATA_ROLE_HOST) {
            mDevicePref.setChecked(false);
            mHostPref.setChecked(true);
            mPreferenceCategory.setEnabled(true);
        } else if (!connected || dataRole == UsbPort.DATA_ROLE_NONE){
            mPreferenceCategory.setEnabled(false);
            if (mNextRolePref == null) {
                // Disconnected with no operation pending, so clear subtexts
                mHostPref.setSummary("");
                mDevicePref.setSummary("");
            }
        }

        if (mNextRolePref != null && dataRole != UsbPort.DATA_ROLE_NONE) {
            if (UsbBackend.dataRoleFromString(mNextRolePref.getKey()) == dataRole) {
                // Clear switching text if switch succeeded
                mNextRolePref.setSummary("");
            } else {
                // Set failure text if switch failed
                mNextRolePref.setSummary(R.string.usb_switching_failed);
            }
            mNextRolePref = null;
            mHandler.removeCallbacks(mFailureCallback);
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int role = UsbBackend.dataRoleFromString(preference.getKey());
        if (role != mUsbBackend.getDataRole() && mNextRolePref == null
                && !Utils.isMonkeyRunning()) {
            mUsbBackend.setDataRole(role);
            mNextRolePref = preference;
            preference.setSummary(R.string.usb_switching);

            mHandler.postDelayed(mFailureCallback,
                    mUsbBackend.areAllRolesSupported() ? UsbBackend.PD_ROLE_SWAP_TIMEOUT_MS
                            : UsbBackend.NONPD_ROLE_SWAP_TIMEOUT_MS);
        }
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return "usb_details_data_role";
    }

    private RadioButtonPreference makeRadioPreference(String key, int titleId) {
        RadioButtonPreference pref = new RadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }
}
