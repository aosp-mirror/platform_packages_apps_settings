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

import android.content.Context;
import android.debug.PairDevice;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.FooterPreference;

/**
 * Controller for logic pertaining to displaying adb device information for the
 * {@link AdbDeviceDetailsFragment}.
 */
public class AdbDeviceDetailsFingerprintController extends AbstractPreferenceController {

    private static final String TAG = "AdbDeviceDetailsFinger";

    @VisibleForTesting
    static final String KEY_FINGERPRINT_CATEGORY = "fingerprint_category";

    private PairDevice mPairedDevice;
    private final Fragment mFragment;

    private PreferenceCategory mFingerprintCategory;
    private FooterPreference mFingerprintPref;

    public AdbDeviceDetailsFingerprintController(
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
        return KEY_FINGERPRINT_CATEGORY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mFingerprintCategory = (PreferenceCategory) screen.findPreference(getPreferenceKey());
        mFingerprintPref = new FooterPreference(mFingerprintCategory.getContext());
        final CharSequence titleFormat = mContext.getText(
                R.string.adb_device_fingerprint_title_format);
        mFingerprintPref.setTitle(String.format(
                titleFormat.toString(), mPairedDevice.getGuid()));
        mFingerprintCategory.addPreference(mFingerprintPref);
    }
}

