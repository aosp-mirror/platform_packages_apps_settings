/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.rtt.TelecomUtil;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

/** A controller to control the status for RTT setting in Accessibility screen. */
public class RTTSettingPreferenceController extends BasePreferenceController {

    private static final String TAG = "RTTSettingsCtr";

    private static final String DIALER_RTT_CONFIGURATION = "dialer_rtt_configuration";
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final CarrierConfigManager mCarrierConfigManager;
    private final CharSequence[] mModes;
    private final String mDialerPackage;

    @VisibleForTesting
    Intent mRTTIntent;

    public RTTSettingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mModes = mContext.getResources().getTextArray(R.array.rtt_setting_mode);
        mDialerPackage = mContext.getString(R.string.config_rtt_setting_package_name);
        mPackageManager = mContext.getPackageManager();
        mCarrierConfigManager = mContext.getSystemService(CarrierConfigManager.class);
        mRTTIntent = new Intent(context.getString(R.string.config_rtt_setting_intent_action));
        Log.d(TAG, "init controller");
    }

    @Override
    public int getAvailabilityStatus() {
        final List<ResolveInfo> resolved =
                mPackageManager.queryIntentActivities(mRTTIntent, 0 /* flags */);
        return resolved != null && !resolved.isEmpty() && isRttSettingSupported()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(getPreferenceKey());
        pref.setIntent(mRTTIntent);
    }

    @Override
    public CharSequence getSummary() {
        final int option = Settings.Secure.getInt(mContext.getContentResolver(),
                DIALER_RTT_CONFIGURATION, 0 /* Invalid value */);
        Log.d(TAG, "DIALER_RTT_CONFIGURATION value =  " + option);
        return mModes[option];
    }

    @VisibleForTesting
    boolean isRttSettingSupported() {
        Log.d(TAG, "isRttSettingSupported [start]");
        if (!isDefaultDialerSupportedRTT(mContext)) {
            Log.d(TAG, "Dialer doesn't support RTT.");
            return false;
        }
        // At least one PhoneAccount must have both isRttSupported and
        // ignore_rtt_mode_setting_bool being true
        for (PhoneAccountHandle phoneAccountHandle :
                TelecomUtil.getCallCapablePhoneAccounts(mContext)) {
            final int subId =
                    TelecomUtil.getSubIdForPhoneAccountHandle(mContext, phoneAccountHandle);
            Log.d(TAG, "subscription id for the device: " + subId);

            final boolean isRttCallingSupported = isRttSupportedByTelecom(phoneAccountHandle);
            Log.d(TAG, "rtt calling supported by telecom:: " + isRttCallingSupported);

            if (isRttCallingSupported) {
                PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
                // If IGNORE_RTT_MODE_SETTING_BOOL=true, RTT visibility is not supported because
                // this means we must use the legacy Telecom setting, which does not support RTT
                // visibility.
                if (carrierConfig != null
                        && getBooleanCarrierConfig(
                        CarrierConfigManager.KEY_IGNORE_RTT_MODE_SETTING_BOOL)) {
                    Log.d(TAG, "RTT visibility setting is supported.");
                    return true;
                }
                Log.d(TAG, "IGNORE_RTT_MODE_SETTING_BOOL is false.");
            }
        }
        Log.d(TAG, "isRttSettingSupported [Not support]");
        return false;
    }

    private boolean isRttSupportedByTelecom(PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccount =
                TelecomUtil.getTelecomManager(mContext).getPhoneAccount(phoneAccountHandle);
        if (phoneAccount != null && phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_RTT)) {
            Log.d(TAG, "Phone account has RTT capability.");
            return true;
        }
        return false;
    }

    /**
     * Gets the boolean config from carrier config manager.
     *
     * @param key config key defined in CarrierConfigManager.
     * @return boolean value of corresponding key.
     */
    private boolean getBooleanCarrierConfig(String key) {
        if (mCarrierConfigManager == null) {
            // Return static default defined in CarrierConfigManager.
            return CarrierConfigManager.getDefaultConfig().getBoolean(key);
        }

        // If an invalid subId is used, this bundle will contain default values.
        final int subId = SubscriptionManager.getDefaultVoiceSubscriptionId();
        final PersistableBundle bundle = mCarrierConfigManager.getConfigForSubId(subId);

        return bundle != null
                ? bundle.getBoolean(key)
                : CarrierConfigManager.getDefaultConfig().getBoolean(key);
    }

    /** Returns whether is a correct default dialer which supports RTT. */
    private static boolean isDefaultDialerSupportedRTT(Context context) {
        return TextUtils.equals(
                context.getString(R.string.config_rtt_setting_package_name),
                TelecomUtil.getTelecomManager(context).getDefaultDialerPackage());
    }
}
