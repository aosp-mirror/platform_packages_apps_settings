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

import static android.hardware.usb.UsbPortStatus.POWER_ROLE_NONE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SOURCE;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * This class controls the switch for changing USB power direction.
 */
public class UsbDetailsPowerRoleController extends UsbDetailsController
        implements OnPreferenceClickListener {

    private PreferenceCategory mPreferenceCategory;
    private SwitchPreference mSwitchPreference;

    private int mNextPowerRole;

    private final Runnable mFailureCallback = () -> {
        if (mNextPowerRole != POWER_ROLE_NONE) {
            mSwitchPreference.setSummary(R.string.usb_switching_failed);
            mNextPowerRole = POWER_ROLE_NONE;
        }
    };

    public UsbDetailsPowerRoleController(Context context, UsbDetailsFragment fragment,
            UsbBackend backend) {
        super(context, fragment, backend);
        mNextPowerRole = POWER_ROLE_NONE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mSwitchPreference = new SwitchPreference(mPreferenceCategory.getContext());
        mSwitchPreference.setTitle(R.string.usb_use_power_only);
        mSwitchPreference.setOnPreferenceClickListener(this);
        mPreferenceCategory.addPreference(mSwitchPreference);
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        // Hide this option if this is not a PD compatible connection
        if (connected && !mUsbBackend.areAllRolesSupported()) {
            mFragment.getPreferenceScreen().removePreference(mPreferenceCategory);
        } else if (connected && mUsbBackend.areAllRolesSupported()) {
            mFragment.getPreferenceScreen().addPreference(mPreferenceCategory);
        }
        if (powerRole == POWER_ROLE_SOURCE) {
            mSwitchPreference.setChecked(true);
            mPreferenceCategory.setEnabled(true);
        } else if (powerRole == POWER_ROLE_SINK) {
            mSwitchPreference.setChecked(false);
            mPreferenceCategory.setEnabled(true);
        } else if (!connected || powerRole == POWER_ROLE_NONE) {
            mPreferenceCategory.setEnabled(false);
            if (mNextPowerRole == POWER_ROLE_NONE) {
                mSwitchPreference.setSummary("");
            }
        }

        if (mNextPowerRole != POWER_ROLE_NONE
                && powerRole != POWER_ROLE_NONE) {
            if (mNextPowerRole == powerRole) {
                // Clear switching text if switch succeeded
                mSwitchPreference.setSummary("");
            } else {
                // Set failure text if switch failed
                mSwitchPreference.setSummary(R.string.usb_switching_failed);
            }
            mNextPowerRole = POWER_ROLE_NONE;
            mHandler.removeCallbacks(mFailureCallback);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        int newRole = mSwitchPreference.isChecked() ? POWER_ROLE_SOURCE
                : POWER_ROLE_SINK;
        if (mUsbBackend.getPowerRole() != newRole && mNextPowerRole == POWER_ROLE_NONE
                && !Utils.isMonkeyRunning()) {
            mUsbBackend.setPowerRole(newRole);

            mNextPowerRole = newRole;
            mSwitchPreference.setSummary(R.string.usb_switching);

            mHandler.postDelayed(mFailureCallback,
                    mUsbBackend.areAllRolesSupported() ? UsbBackend.PD_ROLE_SWAP_TIMEOUT_MS
                            : UsbBackend.NONPD_ROLE_SWAP_TIMEOUT_MS);
        }

        // We don't know that the action succeeded until called back in refresh()
        mSwitchPreference.setChecked(!mSwitchPreference.isChecked());
        return true;
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning()
                && !mUsbBackend.isSinglePowerRoleSupported();
    }

    @Override
    public String getPreferenceKey() {
        return "usb_details_power_role";
    }
}
