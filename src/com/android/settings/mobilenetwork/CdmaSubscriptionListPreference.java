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

package com.android.settings.mobilenetwork;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.ListPreference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.settingslib.utils.ThreadUtils;

public class CdmaSubscriptionListPreference extends ListPreference {

    private static final String LOG_TAG = "CdmaSubListPref";

    // Used for CDMA subscription mode
    private static final int CDMA_SUBSCRIPTION_RUIM_SIM = 0;
    private static final int CDMA_SUBSCRIPTION_NV = 1;

    //preferredSubscriptionMode  0 - RUIM/SIM, preferred
    //                           1 - NV
    static final int preferredSubscriptionMode = Phone.PREFERRED_CDMA_SUBSCRIPTION;

    private TelephonyManager mTelephonyManager;

    public CdmaSubscriptionListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTelephonyManager = TelephonyManager.from(context);
        setCurrentCdmaSubscriptionModeValue();
    }

    private void setCurrentCdmaSubscriptionModeValue() {
        int cdmaSubscriptionMode = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE, preferredSubscriptionMode);
        setValue(Integer.toString(cdmaSubscriptionMode));
    }

    public CdmaSubscriptionListPreference(Context context) {
        this(context, null);
    }

    /**
     * Sets the subscription id associated with this preference.
     *
     * @param subId the subscription id.
     */
    public void setSubscriptionId(int subId) {
        mTelephonyManager = TelephonyManager.from(getContext()).createForSubscriptionId(subId);
    }

    //TODO(b/114749736): move this logic to preference controller
    protected void showDialog(Bundle state) {
        setCurrentCdmaSubscriptionModeValue();
    }

    //TODO(b/114749736): move this logic to preference controller
    protected void onDialogClosed(boolean positiveResult) {
        if (!positiveResult) {
            //The button was dismissed - no need to set new value
            return;
        }

        int buttonCdmaSubscriptionMode = Integer.parseInt(getValue());
        Log.d(LOG_TAG, "Setting new value " + buttonCdmaSubscriptionMode);
        int statusCdmaSubscriptionMode;
        switch(buttonCdmaSubscriptionMode) {
            case CDMA_SUBSCRIPTION_NV:
                statusCdmaSubscriptionMode = Phone.CDMA_SUBSCRIPTION_NV;
                break;
            case CDMA_SUBSCRIPTION_RUIM_SIM:
                statusCdmaSubscriptionMode = Phone.CDMA_SUBSCRIPTION_RUIM_SIM;
                break;
            default:
                statusCdmaSubscriptionMode = Phone.PREFERRED_CDMA_SUBSCRIPTION;
        }

        // Set the CDMA subscription mode, when mode has been successfully changed, update the
        // mode to the global setting.
        ThreadUtils.postOnBackgroundThread(() -> {
            // The subscription mode selected by user.
            int cdmaSubscriptionMode = Integer.parseInt(getValue());

            boolean isSuccessed = mTelephonyManager.setCdmaSubscriptionMode(
                    statusCdmaSubscriptionMode);

            // Update the global settings if successed.
            if (isSuccessed) {
                Settings.Global.putInt(getContext().getContentResolver(),
                        Settings.Global.CDMA_SUBSCRIPTION_MODE,
                        cdmaSubscriptionMode);
            } else {
                Log.e(LOG_TAG, "Setting Cdma subscription source failed");
            }
        });
    }
}
