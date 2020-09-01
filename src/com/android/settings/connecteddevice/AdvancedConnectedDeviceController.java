/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.android.settingslib.drawer.TileUtils.IA_SETTINGS_ACTION;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.nfc.NfcPreferenceController;

/**
 * Controller that used to show which component is available
 */
public class AdvancedConnectedDeviceController extends BasePreferenceController {

    private static final String DRIVING_MODE_SETTINGS_ENABLED =
            "gearhead:driving_mode_settings_enabled";
    private static final String GEARHEAD_PACKAGE = "com.google.android.projection.gearhead";

    public AdvancedConnectedDeviceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(getConnectedDevicesSummaryResourceId(mContext));
    }

    /**
     * Get Connected Devices summary that depend on {@link NfcPreferenceController} or
     * diving mode are available
     */
    public static int getConnectedDevicesSummaryResourceId(Context context) {
        final NfcPreferenceController nfcPreferenceController =
                new NfcPreferenceController(context, NfcPreferenceController.KEY_TOGGLE_NFC);

        return getConnectedDevicesSummaryResourceId(nfcPreferenceController,
                isDrivingModeAvailable(context), isAndroidAutoSettingAvailable(context));
    }

    @VisibleForTesting
    static boolean isDrivingModeAvailable(Context context) {
        return Settings.System.
                getInt(context.getContentResolver(), DRIVING_MODE_SETTINGS_ENABLED, 0) == 1;
    }

    @VisibleForTesting
    static boolean isAndroidAutoSettingAvailable(Context context) {
        final Intent intent = new Intent(IA_SETTINGS_ACTION);
        intent.setPackage(GEARHEAD_PACKAGE);
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    @VisibleForTesting
    static int getConnectedDevicesSummaryResourceId(NfcPreferenceController
            nfcPreferenceController,
            boolean isDrivingModeAvailable,
            boolean isAndroidAutoAvailable) {
        final int resId;

        if (isAndroidAutoAvailable) {
            if (nfcPreferenceController.isAvailable()) {
                if (isDrivingModeAvailable) {
                    // NFC available, driving mode available
                    resId = R.string.connected_devices_dashboard_android_auto_summary;
                } else {
                    // NFC available, driving mode not available
                    resId =
                        R.string.connected_devices_dashboard_android_auto_no_driving_mode_summary;
                }
            } else {
                if (isDrivingModeAvailable) {
                    // NFC not available, driving mode available
                    resId = R.string.connected_devices_dashboard_android_auto_no_nfc_summary;
                } else {
                    // NFC not available, driving mode not available
                    resId =
                        R.string.connected_devices_dashboard_android_auto_no_nfc_no_driving_mode;
                }
            }
        } else {
            if (nfcPreferenceController.isAvailable()) {
                if (isDrivingModeAvailable) {
                    // NFC available, driving mode available
                    resId = R.string.connected_devices_dashboard_summary;
                } else {
                    // NFC available, driving mode not available
                    resId = R.string.connected_devices_dashboard_no_driving_mode_summary;
                }
            } else {
                if (isDrivingModeAvailable) {
                    // NFC not available, driving mode available
                    resId = R.string.connected_devices_dashboard_no_nfc_summary;
                } else {
                    // NFC not available, driving mode not available
                    resId = R.string.connected_devices_dashboard_no_driving_mode_no_nfc_summary;
                }
            }
        }

        return resId;
    }
}
