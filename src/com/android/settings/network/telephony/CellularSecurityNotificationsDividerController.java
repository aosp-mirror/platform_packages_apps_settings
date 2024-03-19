/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.content.Context;
import android.os.Build;
import android.safetycenter.SafetyCenterManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.core.BasePreferenceController;

/**
 * {@link BasePreferenceController} for visibility of Notifications divider on Cellular Security
 * settings page.
 */
public class CellularSecurityNotificationsDividerController extends
                BasePreferenceController {

    private static final String LOG_TAG = "CellularSecurityNotificationsDividerController";

    private TelephonyManager mTelephonyManager;
    @VisibleForTesting
    protected SafetyCenterManager mSafetyCenterManager;

    /**
     * Class constructor of "Cellular Security" preference.
     *
     * @param context of settings
     * @param prefKey assigned within UI entry of XML file
     */
    public CellularSecurityNotificationsDividerController(
            @NonNull Context context, @NonNull String prefKey) {
        super(context, prefKey);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mSafetyCenterManager = mContext.getSystemService(SafetyCenterManager.class);
    }

    /**
     * Initialization.
     */
    public CellularSecurityNotificationsDividerController init() {
        return this;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Flags.enableIdentifierDisclosureTransparencyUnsolEvents()
                || !Flags.enableModemCipherTransparencyUnsolEvents()
                || !Flags.enableIdentifierDisclosureTransparency()
                || !Flags.enableModemCipherTransparency()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!isSafetyCenterSupported()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "Telephony manager not yet initialized");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        // Checking for hardware support, i.e. IRadio AIDL version must be >= 2.2
        try {
            // Must call both APIs, as we can't use the combined toggle if both aren't available
            areNotificationsEnabled();
        } catch (UnsupportedOperationException e) {
            Log.i(LOG_TAG, "Cellular security notifications are unsupported, hiding divider: "
                    + e.getMessage());
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @VisibleForTesting
    protected boolean areNotificationsEnabled() {
        return mTelephonyManager.isNullCipherNotificationsEnabled()
            && mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled();
    }

    protected boolean isSafetyCenterSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }
        mSafetyCenterManager = mContext.getSystemService(
                SafetyCenterManager.class);
        if (mSafetyCenterManager == null) {
            return false;
        }
        return mSafetyCenterManager.isSafetyCenterEnabled();
    }
}
