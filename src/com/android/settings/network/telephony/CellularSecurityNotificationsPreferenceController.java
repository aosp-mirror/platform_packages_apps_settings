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

import com.android.internal.telephony.flags.Flags;

/**
 * {@link TelephonyTogglePreferenceController} for accessing Cellular Security settings through
 * Safety Center.
 */
public class CellularSecurityNotificationsPreferenceController extends
                TelephonyTogglePreferenceController {

    private static final String LOG_TAG = "CellularSecurityNotificationsPreferenceController";

    private TelephonyManager mTelephonyManager;
    @VisibleForTesting
    protected SafetyCenterManager mSafetyCenterManager;

    /**
     * Class constructor of "Cellular Security" preference.
     *
     * @param context of settings
     * @param prefKey assigned within UI entry of XML file
     */
    public CellularSecurityNotificationsPreferenceController(
            @NonNull Context context, @NonNull String prefKey) {
        super(context, prefKey);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mSafetyCenterManager = mContext.getSystemService(SafetyCenterManager.class);
    }

    /**
     * Initialization based on a given subscription id.
     *
     * @param subId is the subscription id
     * @return this instance after initialization
     */
    @NonNull public CellularSecurityNotificationsPreferenceController init(@NonNull int subId) {
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!isSafetyCenterSupported()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (!areFlagsEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        // Checking for hardware support, i.e. IRadio AIDL version must be >= 2.2
        try {
            areNotificationsEnabled();
        } catch (UnsupportedOperationException e) {
            Log.i(LOG_TAG, "Cellular security notifications are unsupported: " + e.getMessage());
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    /**
     * Return {@code true} if cellular security notifications are on
     *
     * <p><b>NOTE:</b> This method returns the active state of the preference controller and is not
     * the parameter passed into {@link #setChecked(boolean)}, which is instead the requested future
     * state.
     */
    @Override
    public boolean isChecked() {
        if (!areFlagsEnabled()) {
            return false;
        }

        try {
            // Note: the default behavior for this toggle is disabled (as the underlying
            // TelephonyManager APIs are disabled by default)
            return areNotificationsEnabled();
        } catch (Exception e) {
            Log.e(LOG_TAG,
                    "Failed isNullCipherNotificationsEnabled and "
                            + "isCellularIdentifierDisclosureNotificationsEnabled."
                            + "Defaulting toggle to checked = true. Exception: "
                            + e.getMessage());
            return false;
        }
    }

    /**
     * Called when a user preference changes on the toggle. We pass this info on to the Telephony
     * Framework so that the modem can be updated with the user's preference.
     *
     * <p>See {@link com.android.settings.core.TogglePreferenceController#setChecked(boolean)} for
     * details.
     *
     * @param isChecked The toggle value that we're being requested to enforce. A value of {@code
     *                  true} denotes that both (1) null cipher/integrity notifications, and
     *                  (2) IMSI disclosure notifications will be enabled by the modem after this
     *                  function completes, if they are not already.
     */
    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            Log.i(LOG_TAG, "Enabling cellular security notifications.");
        } else {
            Log.i(LOG_TAG, "Disabling cellular security notifications.");
        }

        // Check flag status
        if (!areFlagsEnabled()) {
            return false;
        }

        try {
            setNotifications(isChecked);
        } catch (Exception e) {
            Log.e(LOG_TAG,
                    "Failed setCellularIdentifierDisclosureNotificationEnabled or "
                            + " setNullCipherNotificationsEnabled. Setting not updated. Exception: "
                            + e.getMessage());
            // Reset to defaults so we don't end up in an inconsistent state
            setNotifications(!isChecked);
            return false;
        }
        return true;
    }

    private void setNotifications(boolean isChecked) {
        mTelephonyManager.setEnableCellularIdentifierDisclosureNotifications(isChecked);
        mTelephonyManager.setNullCipherNotificationsEnabled(isChecked);
    }

    private boolean areNotificationsEnabled() {
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "Telephony manager not yet initialized");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        return mTelephonyManager.isNullCipherNotificationsEnabled()
            && mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled();
    }

    private boolean areFlagsEnabled() {
        if (!Flags.enableIdentifierDisclosureTransparencyUnsolEvents()
                || !Flags.enableModemCipherTransparencyUnsolEvents()
                || !Flags.enableIdentifierDisclosureTransparency()
                || !Flags.enableModemCipherTransparency()) {
            return false;
        }
        return true;
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
