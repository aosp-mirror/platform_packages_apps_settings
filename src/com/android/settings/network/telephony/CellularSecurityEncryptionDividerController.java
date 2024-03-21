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
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.flags.FeatureFlagsImpl;
import com.android.settings.core.BasePreferenceController;

/**
 * {@link BasePreferenceController} for visibility of Encryption divider on Cellular Security
 * settings page.
 */
public class CellularSecurityEncryptionDividerController extends
                BasePreferenceController {

    private static final String LOG_TAG = "CellularSecurityEncryptionDividerController";

    private TelephonyManager mTelephonyManager;

    protected final FeatureFlags mFeatureFlags = new FeatureFlagsImpl();

    /**
     * Class constructor of "Cellular Security" preference.
     *
     * @param context of settings
     * @param prefKey assigned within UI entry of XML file
     */
    public CellularSecurityEncryptionDividerController(
            @NonNull Context context, @NonNull String prefKey) {
        super(context, prefKey);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
    }

    /**
     * Initialization.
     */
    public CellularSecurityEncryptionDividerController init() {
        return this;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG,
                    "Telephony manager not yet initialized. Marking availability as "
                            + "CONDITIONALLY_UNAVAILABLE");
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
            return CONDITIONALLY_UNAVAILABLE;
        }

        try {
            mTelephonyManager.isNullCipherAndIntegrityPreferenceEnabled();
        } catch (UnsupportedOperationException e) {
            Log.i(LOG_TAG, "Null cipher enablement is unsupported, hiding divider: "
                    + e.getMessage());
            return UNSUPPORTED_ON_DEVICE;
        } catch (Exception e) {
            Log.e(LOG_TAG,
                    "Failed isNullCipherAndIntegrityEnabled. Setting availability to "
                            + "CONDITIONALLY_UNAVAILABLE. Exception: "
                            + e.getMessage());
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }
}
