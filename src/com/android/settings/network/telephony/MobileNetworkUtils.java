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

package com.android.settings.network.telephony;

import static android.provider.Telephony.Carriers.ENFORCE_MANAGED_URI;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.telephony.ims.feature.ImsFeature;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;

import java.util.Arrays;
import java.util.List;

public class MobileNetworkUtils {

    private static final String TAG = "MobileNetworkUtils";

    // CID of the device.
    private static final String KEY_CID = "ro.boot.cid";
    // CIDs of devices which should not show anything related to eSIM.
    private static final String KEY_ESIM_CID_IGNORE = "ro.setupwizard.esim_cid_ignore";
    // System Property which is used to decide whether the default eSIM UI will be shown,
    // the default value is false.
    private static final String KEY_ENABLE_ESIM_UI_BY_DEFAULT =
            "esim.enable_esim_system_ui_by_default";
    private static final String LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT =
            "android.telecom.action.CONNECTION_SERVICE_CONFIGURE";

    /**
     * Returns if DPC APNs are enforced.
     */
    public static boolean isDpcApnEnforced(Context context) {
        try (Cursor enforceCursor = context.getContentResolver().query(ENFORCE_MANAGED_URI,
                null, null, null, null)) {
            if (enforceCursor == null || enforceCursor.getCount() != 1) {
                return false;
            }
            enforceCursor.moveToFirst();
            return enforceCursor.getInt(0) > 0;
        }
    }

    /**
     * Returns true if Wifi calling is enabled for at least one phone.
     */
    public static boolean isWifiCallingEnabled(Context context) {
        int phoneCount = TelephonyManager.from(context).getPhoneCount();
        for (int i = 0; i < phoneCount; i++) {
            if (isWifiCallingEnabled(context, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if Wifi calling is enabled for the specific phone with id {@code phoneId}.
     */
    public static boolean isWifiCallingEnabled(Context context, int phoneId) {
        final PhoneAccountHandle simCallManager =
                TelecomManager.from(context).getSimCallManager();

        boolean isWifiCallingEnabled;
        if (simCallManager != null) {
            Intent intent = buildPhoneAccountConfigureIntent(
                    context, simCallManager);

            isWifiCallingEnabled = intent != null;
        } else {
            ImsManager imsMgr = ImsManager.getInstance(context, phoneId);
            isWifiCallingEnabled = imsMgr != null
                    && imsMgr.isWfcEnabledByPlatform()
                    && imsMgr.isWfcProvisionedOnDevice()
                    && isImsServiceStateReady(imsMgr);
        }

        return isWifiCallingEnabled;
    }

    @VisibleForTesting
    static Intent buildPhoneAccountConfigureIntent(
            Context context, PhoneAccountHandle accountHandle) {
        Intent intent = buildConfigureIntent(
                context, accountHandle, TelecomManager.ACTION_CONFIGURE_PHONE_ACCOUNT);

        if (intent == null) {
            // If the new configuration didn't work, try the old configuration intent.
            intent = buildConfigureIntent(context, accountHandle,
                    LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT);
        }
        return intent;
    }

    private static Intent buildConfigureIntent(
            Context context, PhoneAccountHandle accountHandle, String actionStr) {
        if (accountHandle == null || accountHandle.getComponentName() == null
                || TextUtils.isEmpty(accountHandle.getComponentName().getPackageName())) {
            return null;
        }

        // Build the settings intent.
        Intent intent = new Intent(actionStr);
        intent.setPackage(accountHandle.getComponentName().getPackageName());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);

        // Check to see that the phone account package can handle the setting intent.
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
        if (resolutions.size() == 0) {
            intent = null;  // set no intent if the package cannot handle it.
        }

        return intent;
    }

    public static boolean isImsServiceStateReady(ImsManager imsMgr) {
        boolean isImsServiceStateReady = false;

        try {
            if (imsMgr != null && imsMgr.getImsServiceState() == ImsFeature.STATE_READY) {
                isImsServiceStateReady = true;
            }
        } catch (ImsException ex) {
            Log.e(TAG, "Exception when trying to get ImsServiceStatus: " + ex);
        }

        Log.d(TAG, "isImsServiceStateReady=" + isImsServiceStateReady);
        return isImsServiceStateReady;
    }

    /**
     * Whether to show the entry point to eUICC settings.
     *
     * <p>We show the entry point on any device which supports eUICC as long as either the eUICC
     * was ever provisioned (that is, at least one profile was ever downloaded onto it), or if
     * the user has enabled development mode.
     */
    public static boolean showEuiccSettings(Context context) {
        EuiccManager euiccManager =
                (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
        if (!euiccManager.isEnabled()) {
            return false;
        }

        final ContentResolver cr = context.getContentResolver();

        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String currentCountry = tm.getNetworkCountryIso().toLowerCase();
        String supportedCountries =
                Settings.Global.getString(cr, Settings.Global.EUICC_SUPPORTED_COUNTRIES);
        boolean inEsimSupportedCountries = false;
        if (TextUtils.isEmpty(currentCountry)) {
            inEsimSupportedCountries = true;
        } else if (!TextUtils.isEmpty(supportedCountries)) {
            List<String> supportedCountryList =
                    Arrays.asList(TextUtils.split(supportedCountries.toLowerCase(), ","));
            if (supportedCountryList.contains(currentCountry)) {
                inEsimSupportedCountries = true;
            }
        }
        final boolean esimIgnoredDevice =
                Arrays.asList(TextUtils.split(SystemProperties.get(KEY_ESIM_CID_IGNORE, ""), ","))
                        .contains(SystemProperties.get(KEY_CID, null));
        final boolean enabledEsimUiByDefault =
                SystemProperties.getBoolean(KEY_ENABLE_ESIM_UI_BY_DEFAULT, true);
        final boolean euiccProvisioned =
                Settings.Global.getInt(cr, Settings.Global.EUICC_PROVISIONED, 0) != 0;
        final boolean inDeveloperMode =
                Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;

        return (inDeveloperMode || euiccProvisioned
                || (!esimIgnoredDevice && enabledEsimUiByDefault && inEsimSupportedCountries));
    }

    public static PersistableBundle getCarrierConfigBySubId(int mSubId) {
        //TODO(b/114749736): get carrier config from subId
        return new PersistableBundle();
    }

    /**
     * Set whether to enable data for {@code subId}, also whether to disable data for other
     * subscription
     */
    public static void setMobileDataEnabled(Context context, int subId, boolean enabled,
            boolean disableOtherSubscriptions) {
        final TelephonyManager telephonyManager = TelephonyManager.from(context)
                .createForSubscriptionId(subId);
        final SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        telephonyManager.setDataEnabled(enabled);

        if (disableOtherSubscriptions) {
            List<SubscriptionInfo> subInfoList =
                    subscriptionManager.getActiveSubscriptionInfoList();
            if (subInfoList != null) {
                for (SubscriptionInfo subInfo : subInfoList) {
                    if (subInfo.getSubscriptionId() != subId) {
                        TelephonyManager.from(context).createForSubscriptionId(
                                subInfo.getSubscriptionId()).setDataEnabled(false);
                    }
                }
            }
        }
    }

    /**
     * Return {@code true} if show CDMA category
     */
    public static boolean isCdmaOptions(Context context, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        final TelephonyManager telephonyManager = TelephonyManager.from(context)
                .createForSubscriptionId(subId);

        if (telephonyManager.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            return true;
        }

        if (isWorldMode(context, subId)) {
            final int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    context.getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subId,
                    Phone.PREFERRED_NT_MODE);
            if (settingsNetworkMode == TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA
                    || settingsNetworkMode == TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO) {
                return true;
            }

            if (settingsNetworkMode == TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA
                    && !isTdscdmaSupported(context, telephonyManager)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return {@code true} if it is world mode, and we may show advanced options in telephony
     * settings
     */
    public static boolean isWorldMode(Context context, int subId) {
        final TelephonyManager telephonyManager = TelephonyManager.from(context)
                .createForSubscriptionId(subId);
        boolean worldModeOn = false;
        final String configString = context.getString(R.string.config_world_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            // Check if we have World mode configuration set to True only or config is set to True
            // and SIM GID value is also set and matches to the current SIM GID.
            if (configArray != null &&
                    ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true"))
                            || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1])
                            && telephonyManager != null
                            && configArray[1].equalsIgnoreCase(
                            telephonyManager.getGroupIdLevel1())))) {
                worldModeOn = true;
            }
        }

        Log.d(TAG, "isWorldMode=" + worldModeOn);

        return worldModeOn;
    }

    //TODO(b/117651939): move it to telephony
    private static boolean isTdscdmaSupported(Context context, TelephonyManager telephonyManager) {
        if (context.getResources().getBoolean(R.bool.config_support_tdscdma)) {
            return true;
        }

        String operatorNumeric = telephonyManager.getServiceState().getOperatorNumeric();
        String[] numericArray = context.getResources().getStringArray(
                R.array.config_support_tdscdma_roaming_on_networks);
        if (numericArray.length == 0 || operatorNumeric == null) {
            return false;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.equals(numeric)) {
                return true;
            }
        }
        return false;
    }
}