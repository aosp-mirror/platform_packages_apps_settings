/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

/**
 * List of Network-specific settings screens.
 */
public class GsmUmtsOptions {
    private static final String LOG_TAG = "GsmUmtsOptions";

    private CarrierConfigManager mCarrierConfigManager;
    private RestrictedPreference mButtonAPNExpand;
    private Preference mCategoryAPNExpand;
    Preference mCarrierSettingPref;

    private NetworkOperators mNetworkOperator;

    private static final String BUTTON_APN_EXPAND_KEY = "button_gsm_apn_key";
    private static final String CATEGORY_APN_EXPAND_KEY = "category_gsm_apn_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";

    public static final String EXTRA_SUB_ID = "sub_id";
    private PreferenceFragmentCompat mPrefFragment;
    private PreferenceScreen mPrefScreen;

    public GsmUmtsOptions(PreferenceFragmentCompat prefFragment, PreferenceScreen prefScreen,
            final int subId) {
        final Context context = prefFragment.getContext();
        mPrefFragment = prefFragment;
        mPrefScreen = prefScreen;
        mCarrierConfigManager = new CarrierConfigManager(context);
        mPrefFragment.addPreferencesFromResource(R.xml.gsm_umts_options);
        mButtonAPNExpand = (RestrictedPreference) mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        mCategoryAPNExpand = mPrefScreen.findPreference(CATEGORY_APN_EXPAND_KEY);
        mNetworkOperator = (NetworkOperators) mPrefScreen
                .findPreference(NetworkOperators.CATEGORY_NETWORK_OPERATORS_KEY);
        mCarrierSettingPref = mPrefScreen.findPreference(BUTTON_CARRIER_SETTINGS_KEY);

        mNetworkOperator.initialize();

        update(subId);
    }

    // Unlike mPrefFragment or mPrefScreen, subId  may change during lifecycle of GsmUmtsOptions.
    // When that happens, we update GsmUmtsOptions with new parameters.
    protected void update(final int subId) {
        boolean addAPNExpand = true;
        boolean addNetworkOperatorsCategory = true;
        boolean addCarrierSettings = true;
        final TelephonyManager telephonyManager = TelephonyManager.from(mPrefFragment.getContext())
                .createForSubscriptionId(subId);
        //TODO(b/115429509): Get phone from subId
        Phone phone = null;
        if (phone == null) return;
        if (telephonyManager.getPhoneType() != PhoneConstants.PHONE_TYPE_GSM) {
            log("Not a GSM phone");
            addAPNExpand = false;
            mNetworkOperator.setEnabled(false);
        } else {
            log("Not a CDMA phone");
            PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);

            // Determine which options to display. For GSM these are defaulted to true in
            // CarrierConfigManager, but they maybe overriden by DefaultCarrierConfigService or a
            // carrier app.
            // Note: these settings used to be controlled with overlays in
            // Telephony/res/values/config.xml
            if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL)
                    && mCategoryAPNExpand != null) {
                addAPNExpand = false;
            }
            if (!carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_OPERATOR_SELECTION_EXPAND_BOOL)) {
                addNetworkOperatorsCategory = false;
            }

            if (carrierConfig.getBoolean(CarrierConfigManager.KEY_CSP_ENABLED_BOOL)) {
                if (phone.isCspPlmnEnabled()) {
                    log("[CSP] Enabling Operator Selection menu.");
                    mNetworkOperator.setEnabled(true);
                } else {
                    log("[CSP] Disabling Operator Selection menu.");
                    addNetworkOperatorsCategory = false;
                }
            }

            // Read platform settings for carrier settings
            addCarrierSettings = carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_SETTINGS_ENABLE_BOOL);
        }

        // Making no assumptions of whether they are added or removed at this point.
        // Calling add or remove explicitly to make sure they are updated.

        if (addAPNExpand) {
            log("update: addAPNExpand");
            mButtonAPNExpand.setDisabledByAdmin(
                    MobileNetworkUtils.isDpcApnEnforced(mButtonAPNExpand.getContext())
                            ? RestrictedLockUtilsInternal.getDeviceOwner(
                            mButtonAPNExpand.getContext())
                            : null);
            mButtonAPNExpand.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            MetricsLogger.action(mButtonAPNExpand.getContext(),
                                    MetricsEvent.ACTION_MOBILE_NETWORK_APN_SETTINGS);
                            // We need to build the Intent by hand as the Preference Framework
                            // does not allow to add an Intent with some extras into a Preference
                            // XML file
                            final Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);
                            // This will setup the Home and Search affordance
                            intent.putExtra(":settings:show_fragment_as_subsetting", true);
                            intent.putExtra(EXTRA_SUB_ID, subId);
                            mPrefFragment.startActivity(intent);
                            return true;
                        }
                    });
            mPrefScreen.addPreference(mCategoryAPNExpand);
        } else {
            mPrefScreen.removePreference(mCategoryAPNExpand);
        }

        if (addNetworkOperatorsCategory) {
            mPrefScreen.addPreference(mNetworkOperator);
            mNetworkOperator.update(subId);
        } else {
            mPrefScreen.removePreference(mNetworkOperator);
        }

        if (addCarrierSettings) {
            mPrefScreen.addPreference(mCarrierSettingPref);
        } else {
            mPrefScreen.removePreference(mCarrierSettingPref);
        }

    }

    protected boolean preferenceTreeClick(Preference preference) {
        return mNetworkOperator.preferenceTreeClick(preference);
    }

    protected void log(String s) {
        android.util.Log.d(LOG_TAG, s);
    }
}

