/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.bluetooth.BluetoothPairingDetail;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;

import androidx.annotation.VisibleForTesting;

/** Preference controller for all bluetooth device preference. */
public class ViewAllBluetoothDevicesPreferenceController extends BasePreferenceController implements
        PreferenceManager.OnActivityResultListener {

    private static final int REQUEST_CODE_BONDED_DEVICE = 270;
    private DashboardFragment mFragment;

    public ViewAllBluetoothDevicesPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Initializes objects in this controller. Needs to call this before using the controller.
     *
     * @param fragment The {@link DashboardFragment} uses the controller
     */
    public void init(DashboardFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            launchBluetoothPairingDetail();
            return true;
        }

        return false;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        // If back from BONDED device page, then no need to show scan result again.
        // Finish the fragment.
        if (requestCode == REQUEST_CODE_BONDED_DEVICE && resultCode == RESULT_OK) {
            mFragment.finish();
        }
        return false;
    }

    @VisibleForTesting
    void launchBluetoothPairingDetail() {
        new SubSettingLauncher(mContext)
                .setDestination(BluetoothPairingDetail.class.getName())
                .setSourceMetricsCategory(mFragment.getMetricsCategory())
                .setResultListener(mFragment, REQUEST_CODE_BONDED_DEVICE)
                .launch();
    }
}
