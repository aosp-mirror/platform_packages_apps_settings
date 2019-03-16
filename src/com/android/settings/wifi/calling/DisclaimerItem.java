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

package com.android.settings.wifi.calling;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Interface to control disclaimer item from {@link WifiCallingDisclaimerFragment}.
 */
@VisibleForTesting
public abstract class DisclaimerItem {
    private static final String SHARED_PREFERENCES_NAME = "wfc_disclaimer_prefs";

    protected final Context mContext;
    protected final int mSubId;
    private final CarrierConfigManager mCarrierConfigManager;

    DisclaimerItem(Context context, int subId) {
        mContext = context;
        mSubId = subId;
        mCarrierConfigManager = mContext.getSystemService(CarrierConfigManager.class);
    }

    /**
     * Called by the {@link WifiCallingDisclaimerFragment} when a user has clicked the agree button.
     */
    void onAgreed() {
        setBooleanSharedPrefs(getPrefKey(), true);
    }

    /**
     * Checks whether the disclaimer item need to be displayed or not.
     *
     * @return Returns {@code true} if disclaimer item need to be displayed,
     * {@code false} if not displayed.
     */
    boolean shouldShow() {
        if (getBooleanSharedPrefs(getPrefKey(), false)) {
            logd("shouldShow: false due to a user has already agreed.");
            return false;
        }
        logd("shouldShow: true");
        return true;
    }

    /**
     * Gets the configuration values for a particular sub id.
     *
     * @return The {@link PersistableBundle} instance containing the config value for a
     * particular phone id, or default values.
     */
    protected PersistableBundle getCarrierConfig() {
        PersistableBundle config = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (config != null) {
            return config;
        }
        // Return static default defined in CarrierConfigManager.
        return CarrierConfigManager.getDefaultConfig();
    }

    protected void logd(String msg) {
        Log.d(getName(), "[" + mSubId +  "] " + msg);
    }

    /**
     * Gets a title id for disclaimer item.
     *
     * @return Title id for disclaimer item.
     */
    protected abstract int getTitleId();

    /**
     * Gets a message id for disclaimer item.
     *
     * @return Message id for disclaimer item.
     */
    protected abstract int getMessageId();

    /**
     * Gets a name of disclaimer item.
     *
     * @return Name of disclaimer item.
     */
    protected abstract String getName();

    /**
     * Gets a preference key to keep user's consent.
     *
     * @return Preference key to keep user's consent.
     */
    protected abstract String getPrefKey();

    /**
     * Gets the boolean value from shared preferences.
     *
     * @param key The key for the preference item.
     * @param defValue Value to return if this preference does not exist.
     * @return The boolean value of corresponding key, or defValue.
     */
    private boolean getBooleanSharedPrefs(String key, boolean defValue) {
        SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return prefs.getBoolean(key + mSubId, defValue);
    }

    /**
     * Sets the boolean value to shared preferences.
     *
     * @param key The key for the preference item.
     * @param value The value to be set for shared preferences.
     */
    private void setBooleanSharedPrefs(String key, boolean value) {
        SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key + mSubId, value).apply();
    }
}
