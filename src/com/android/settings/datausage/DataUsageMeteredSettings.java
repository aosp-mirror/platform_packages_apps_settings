/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import static android.net.wifi.WifiInfo.removeDoubleQuotes;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.NetworkPolicyEditor;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel to configure {@link WifiConfiguration#meteredOverride}.
 */
public class DataUsageMeteredSettings extends SettingsPreferenceFragment implements Indexable {

    private NetworkPolicyManager mPolicyManager;
    private WifiManager mWifiManager;

    private NetworkPolicyEditor mPolicyEditor;

    private PreferenceCategory mMobileCategory;
    private PreferenceCategory mWifiCategory;
    private Preference mWifiDisabled;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NET_DATA_USAGE_METERED;
    }

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
        getPreferenceScreen().removePreference(mMobileCategory);

        mWifiCategory.removeAll();
        if (DataUsageUtils.hasWifiRadio(context) && mWifiManager.isWifiEnabled()) {
            for (WifiConfiguration config : mWifiManager.getConfiguredNetworks()) {
                final Preference pref = new MeteredPreference(getPrefContext(), config);
                if (!TextUtils.isEmpty(pref.getTitle())) {
                    mWifiCategory.addPreference(pref);
                }
            }
        } else {
            mWifiCategory.addPreference(mWifiDisabled);
        }
    }

    private class MeteredPreference extends DropDownPreference {
        private final WifiConfiguration mConfig;

        public MeteredPreference(Context context, WifiConfiguration config) {
            super(context);
            mConfig = config;

            setPersistent(false);
            setEntries(new CharSequence[] {
                    getString(R.string.data_usage_metered_auto),
                    getString(R.string.data_usage_metered_yes),
                    getString(R.string.data_usage_metered_no),
            });
            setEntryValues(new CharSequence[] {
                    Integer.toString(WifiConfiguration.METERED_OVERRIDE_NONE),
                    Integer.toString(WifiConfiguration.METERED_OVERRIDE_METERED),
                    Integer.toString(WifiConfiguration.METERED_OVERRIDE_NOT_METERED),
            });
            setValue(Integer.toString(mConfig.meteredOverride));
            setTitle(NetworkPolicyManager.resolveNetworkId(mConfig));
            setSummary(getEntries()[mConfig.meteredOverride]);

            setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mConfig.meteredOverride = Integer.parseInt((String) newValue);
                    setSummary(getEntries()[mConfig.meteredOverride]);

                    mWifiManager.updateNetwork(mConfig);
                    // Stage the backup of the SettingsProvider package which backs this up
                    BackupManager.dataChanged("com.android.providers.settings");
                    return true;
                }
            });
        }
    }

    /**
     * For search
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.data_usage_menu_metered);
                data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                result.add(data);

                // Body
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.data_usage_metered_body);
                data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                result.add(data);

                // Wi-Fi networks category
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.data_usage_metered_wifi);
                data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                result.add(data);

                final WifiManager wifiManager =
                        (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (DataUsageUtils.hasWifiRadio(context) && wifiManager.isWifiEnabled()) {
                    for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
                        if (config.SSID != null) {
                            final String networkId = config.SSID;

                            data = new SearchIndexableRaw(context);
                            data.title = removeDoubleQuotes(networkId);
                            data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                            result.add(data);
                        }
                    }
                } else {
                    data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.data_usage_metered_wifi_disabled);
                    data.screenTitle = res.getString(R.string.data_usage_menu_metered);
                    result.add(data);
                }

                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> result = super.getNonIndexableKeys(context);
                result.add("mobile");
                return result;
            }
        };
}
