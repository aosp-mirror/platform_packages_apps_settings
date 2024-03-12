/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * This class controls the switch for setting if we should transcode files transferred via MTP over
 * USB.
 */
public class UsbDetailsTranscodeMtpController extends UsbDetailsController
        implements Preference.OnPreferenceClickListener {
    private static final String TRANSCODE_MTP_SYS_PROP_KEY = "sys.fuse.transcode_mtp";
    private static final String PREFERENCE_KEY = "usb_transcode_mtp";
    private static final String KEY_USB_TRANSCODE_FILES = "usb_transcode_files";

    private PreferenceCategory mPreferenceCategory;
    private TwoStatePreference mSwitchPreference;

    public UsbDetailsTranscodeMtpController(Context context, UsbDetailsFragment fragment,
            UsbBackend backend) {
        super(context, fragment, backend);
    }


    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mSwitchPreference = new SwitchPreferenceCompat(mPreferenceCategory.getContext());
        mSwitchPreference.setTitle(R.string.usb_transcode_files);
        mSwitchPreference.setKey(KEY_USB_TRANSCODE_FILES);
        mSwitchPreference.setOnPreferenceClickListener(this);
        mSwitchPreference.setSummaryOn(R.string.usb_transcode_files_summary);
        mPreferenceCategory.addPreference(mSwitchPreference);
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        if (mUsbBackend.areFunctionsSupported(UsbManager.FUNCTION_MTP | UsbManager.FUNCTION_PTP)) {
            mFragment.getPreferenceScreen().addPreference(mPreferenceCategory);
        } else {
            mFragment.getPreferenceScreen().removePreference(mPreferenceCategory);
        }

        mSwitchPreference.setChecked(
                SystemProperties.getBoolean(TRANSCODE_MTP_SYS_PROP_KEY, false));
        mPreferenceCategory.setEnabled(
                connected && isDeviceInFileTransferMode(functions, dataRole));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        SystemProperties.set(TRANSCODE_MTP_SYS_PROP_KEY,
                Boolean.toString(mSwitchPreference.isChecked()));

        final long previousFunctions = mUsbBackend.getCurrentFunctions();
        // Toggle the MTP connection to reload file sizes for files shared via MTP clients
        mUsbBackend.setCurrentFunctions(previousFunctions & ~UsbManager.FUNCTION_MTP);
        mUsbBackend.setCurrentFunctions(previousFunctions);

        return true;
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    private static boolean isDeviceInFileTransferMode(long functions, int dataRole) {
        return dataRole == DATA_ROLE_DEVICE && ((functions & UsbManager.FUNCTION_MTP) != 0
                || (functions & UsbManager.FUNCTION_PTP) != 0);
    }
}
