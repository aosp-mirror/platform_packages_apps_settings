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

package com.android.settings.network;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.safetycenter.SafetyCenterManager;
import android.safetycenter.SafetySourceData;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.network.telephony.CellularSecuritySettingsFragment;

import java.util.List;

/**
 * {@link BasePreferenceController} for accessing Cellular Security settings from Network &
 * Internet Settings menu.
 */
public class CellularSecurityPreferenceController extends BasePreferenceController {

    private static final String LOG_TAG = "CellularSecurityPreferenceController";
    private static final String SAFETY_SOURCE_ID = "AndroidCellularNetworkSecurity";

    private @Nullable TelephonyManager mTelephonyManager;

    /**
     * Class constructor of "Cellular Security" preference.
     *
     * @param context of settings
     * @param prefKey     assigned within UI entry of XML file
     */
    public CellularSecurityPreferenceController(@NonNull Context context, @NonNull String prefKey) {
        super(context, prefKey);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                || !Flags.enableIdentifierDisclosureTransparencyUnsolEvents()
                || !Flags.enableModemCipherTransparencyUnsolEvents()
                || !Flags.enableModemCipherTransparency()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "Telephony manager not yet initialized");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }

        // Check there are valid SIM cards which can be displayed to the user, otherwise this
        // settings should not be shown.
        List<SubscriptionInfo> availableSubs = SubscriptionUtil.getAvailableSubscriptions(mContext);
        if (availableSubs.isEmpty()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        boolean isNullCipherDisablementAvailable = false;
        boolean areCellSecNotificationsAvailable = false;
        try {
            mTelephonyManager.isNullCipherAndIntegrityPreferenceEnabled();
            isNullCipherDisablementAvailable = true; // true because it doesn't throw an exception,
                                                     // we don't want the value of
                                                     // isNullCipherAndIntegrityEnabled()
        } catch (UnsupportedOperationException e) {
            Log.i(LOG_TAG, "Null cipher enablement is unsupported, hiding divider: "
                    + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG,
                    "Failed isNullCipherAndIntegrityEnabled. Setting availability to "
                            + "UNSUPPORTED_ON_DEVICE. Exception: "
                            + e.getMessage());
        }

        try {
            // Must call both APIs, as we can't use the combined toggle if both aren't available
            areNotificationsEnabled();
            areCellSecNotificationsAvailable = true; // true because it doesn't throw an exception
                                                     // and we don't want the value of
                                                     // areNotificationsEnabled()
        } catch (UnsupportedOperationException e) {
            Log.i(LOG_TAG, "Cellular security notifications are unsupported, hiding divider: "
                    + e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG,
                    "Failed isNullCipherNotificationsEnabled, "
                            + "isCellularIdentifierDisclosureNotificationsEnabled. "
                            + "Setting availability to UNSUPPORTED_ON_DEVICE. Exception: "
                            + e.getMessage());
        }

        if (isNullCipherDisablementAvailable || areCellSecNotificationsAvailable) {
            return AVAILABLE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        if (!isSafetyCenterSupported()) {
            // Realistically, it's unlikely to end up in handlePreferenceTreeClick with SafetyCenter
            // being not supported on the device.
            return false;
        }
        // Need to check that both Safety Center is available on device, and also that the HALs are
        // enabled before showing the Safety Center UI. Otherwise, we need to take them to the page
        // where the HALs can be enabled.
        SafetyCenterManager safetyCenterManager = mContext.getSystemService(
                SafetyCenterManager.class);
        SafetySourceData data = null;
        if (safetyCenterManager.isSafetyCenterEnabled()) {
            data = safetyCenterManager.getSafetySourceData(SAFETY_SOURCE_ID);
        }
        // Can only redirect to SafetyCenter if it has received data via the SafetySource, as
        // SafetyCenter doesn't support redirecting to a specific page associated with a source
        // if it hasn't received data from that source. See b/373942609 for details.
        if (data != null && areNotificationsEnabled()) {
            Intent safetyCenterIntent = new Intent(Intent.ACTION_SAFETY_CENTER);
            safetyCenterIntent.putExtra(SafetyCenterManager.EXTRA_SAFETY_SOURCES_GROUP_ID,
                    "AndroidCellularNetworkSecuritySources");
            mContext.startActivity(safetyCenterIntent);
        } else {
            Log.v(LOG_TAG, "Hardware APIs not enabled, or data source is null.");
            final Bundle bundle = new Bundle();
            bundle.putString(CellularSecuritySettingsFragment.KEY_CELLULAR_SECURITY_PREFERENCE, "");

            new SubSettingLauncher(mContext)
                     .setDestination(CellularSecuritySettingsFragment.class.getName())
                     .setArguments(bundle)
                     .setSourceMetricsCategory(SettingsEnums.CELLULAR_SECURITY_SETTINGS)
                     .launch();
        }
        return true;
    }

    @VisibleForTesting
    protected boolean isSafetyCenterSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }
        SafetyCenterManager safetyCenterManager = mContext.getSystemService(
                SafetyCenterManager.class);
        if (safetyCenterManager == null) {
            return false;
        }
        return safetyCenterManager.isSafetyCenterEnabled();
    }

    @VisibleForTesting
    protected boolean areNotificationsEnabled() {
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "Telephony manager not yet initialized");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }

        return mTelephonyManager.isNullCipherNotificationsEnabled()
            && mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled();
    }
}
