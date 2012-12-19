/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.net;

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.wifi.WifiInfo.removeDoubleQuotes;
import static com.android.settings.DataUsageSummary.hasReadyMobileRadio;
import static com.android.settings.DataUsageSummary.hasWifiRadio;

import android.content.Context;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Panel to configure {@link NetworkPolicy#metered} for networks.
 */
public class DataUsageMeteredSettings extends SettingsPreferenceFragment {

    private static final boolean SHOW_MOBILE_CATEGORY = false;

    private NetworkPolicyManager mPolicyManager;
    private WifiManager mWifiManager;

    private NetworkPolicyEditor mPolicyEditor;

    private PreferenceCategory mMobileCategory;
    private PreferenceCategory mWifiCategory;
    private Preference mWifiDisabled;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getActivity();

        mPolicyManager = NetworkPolicyManager.from(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
        mPolicyEditor.read();

        addPreferencesFromResource(R.xml.data_usage_metered_prefs);
        mMobileCategory = (PreferenceCategory) findPreference("mobile");
        mWifiCategory = (PreferenceCategory) findPreference("wifi");
        mWifiDisabled = findPreference("wifi_disabled");

        updateNetworks(context);
    }

    private void updateNetworks(Context context) {
        if (SHOW_MOBILE_CATEGORY && hasReadyMobileRadio(context)) {
            mMobileCategory.removeAll();
            mMobileCategory.addPreference(buildMobilePref(context));
        } else {
            getPreferenceScreen().removePreference(mMobileCategory);
        }

        mWifiCategory.removeAll();
        if (hasWifiRadio(context) && mWifiManager.isWifiEnabled()) {
            for (WifiConfiguration config : mWifiManager.getConfiguredNetworks()) {
                if (config.SSID != null) {
                    mWifiCategory.addPreference(buildWifiPref(context, config));
                }
            }
        } else {
            mWifiCategory.addPreference(mWifiDisabled);
        }
    }

    private Preference buildMobilePref(Context context) {
        final TelephonyManager tele = TelephonyManager.from(context);
        final NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(
                tele.getSubscriberId());
        final MeteredPreference pref = new MeteredPreference(context, template);
        pref.setTitle(tele.getNetworkOperatorName());
        return pref;
    }

    private Preference buildWifiPref(Context context, WifiConfiguration config) {
        final String networkId = config.SSID;
        final NetworkTemplate template = NetworkTemplate.buildTemplateWifi(networkId);
        final MeteredPreference pref = new MeteredPreference(context, template);
        pref.setTitle(removeDoubleQuotes(networkId));
        return pref;
    }

    private class MeteredPreference extends CheckBoxPreference {
        private final NetworkTemplate mTemplate;
        private boolean mBinding;

        public MeteredPreference(Context context, NetworkTemplate template) {
            super(context);
            mTemplate = template;

            setPersistent(false);

            mBinding = true;
            final NetworkPolicy policy = mPolicyEditor.getPolicyMaybeUnquoted(template);
            if (policy != null) {
                if (policy.limitBytes != LIMIT_DISABLED) {
                    setChecked(true);
                    setEnabled(false);
                } else {
                    setChecked(policy.metered);
                }
            } else {
                setChecked(false);
            }
            mBinding = false;
        }

        @Override
        protected void notifyChanged() {
            super.notifyChanged();
            if (!mBinding) {
                mPolicyEditor.setPolicyMetered(mTemplate, isChecked());
            }
        }
    }
}
