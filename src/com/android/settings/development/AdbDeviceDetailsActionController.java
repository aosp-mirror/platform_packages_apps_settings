/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.debug.PairDevice;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.ActionButtonsPreference;

/**
 * Controller for logic pertaining to displaying adb device information for the
 * {@link AdbDeviceDetailsFragment}.
 */
public class AdbDeviceDetailsActionController extends AbstractPreferenceController {
    private static final String TAG = "AdbDeviceDetailsAction";

    @VisibleForTesting
    static final String KEY_BUTTONS_PREF = "buttons";

    private PairDevice mPairedDevice;
    private final Fragment mFragment;

    private ActionButtonsPreference mButtonsPref;

    public AdbDeviceDetailsActionController(
            PairDevice pairedDevice,
            Context context,
            Fragment fragment) {
        super(context);

        mPairedDevice = pairedDevice;
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BUTTONS_PREF;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mButtonsPref = ((ActionButtonsPreference) screen.findPreference(getPreferenceKey()))
                .setButton1Visible(false)
                .setButton2Icon(R.drawable.ic_settings_delete)
                .setButton2Text(com.android.settingslib.R.string.adb_device_forget)
                .setButton2OnClickListener(view -> forgetDevice());
    }

    /**
     * Forgets the device.
     */
    private void forgetDevice() {
        Intent intent = new Intent();
        intent.putExtra(
                WirelessDebuggingFragment.PAIRED_DEVICE_REQUEST_TYPE,
                WirelessDebuggingFragment.FORGET_ACTION);
        intent.putExtra(
                WirelessDebuggingFragment.PAIRED_DEVICE_EXTRA,
                mPairedDevice);
        mFragment.getActivity().setResult(Activity.RESULT_OK, intent);
        mFragment.getActivity().finish();
    }
}

