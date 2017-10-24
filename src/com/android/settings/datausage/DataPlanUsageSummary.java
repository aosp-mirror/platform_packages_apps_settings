/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.net.NetworkPolicy.LIMIT_DISABLED;

import android.annotation.IdRes;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;
import java.util.ArrayList;
import java.util.List;

public class DataPlanUsageSummary extends DataUsageBase {

    public static final String KEY_DATA_PLAN_USAGE = "data_plan_usage";

    public static final String KEY_STATUS_HEADER = "status_header";
    public static final String KEY_LIMIT_SUMMARY = "plan_summary";

    // Mobile data keys
    public static final String KEY_MOBILE_USAGE_TITLE = "data_usage_mobile_category";
    public static final String KEY_MOBILE_DATA_USAGE_TOGGLE = "data_usage_enable";

    // Wifi keys
    public static final String KEY_WIFI_USAGE_TITLE = "wifi_category";
    public static final String KEY_WIFI_DATA_USAGE = "wifi_data_usage";
    public static final String KEY_NETWORK_RESTRICTIONS = "network_restrictions";

    private DataUsageController mDataUsageController;
    private DataUsageInfoController mDataInfoController;
    private List<DataPlanSummaryPreference> mDataPlanSummaryPreferenceList;
    private Preference mLimitPreference;
    private NetworkTemplate mDefaultTemplate;
    private NetworkRestrictionsPreference mNetworkRestrictionPreference;
    private WifiManager mWifiManager;
    private NetworkPolicyEditor mPolicyEditor;

    @Override
    protected int getHelpResource() {
        return R.string.help_url_data_usage;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        final Context context = getContext();
        NetworkPolicyManager policyManager = NetworkPolicyManager.from(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mPolicyEditor = new NetworkPolicyEditor(policyManager);
        mDataUsageController = new DataUsageController(context);
        mDataInfoController = new DataUsageInfoController();

        int defaultSubId = DataUsageUtils.getDefaultSubscriptionId(context);
        boolean hasMobileData = DataUsageUtils.hasMobileData(context);
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            hasMobileData = false;
        }
        mDefaultTemplate = DataUsageUtils.getDefaultTemplate(context, defaultSubId);

        if (hasMobileData) {
            addDataPlanSection(defaultSubId);
        }

        if (DataUsageUtils.hasWifiRadio(context)) {
            addWifiSection();
        }

        if (hasEthernet(context)) {
            addEthernetSection();
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (UserManager.get(getContext()).isAdminUser()) {
            inflater.inflate(R.menu.data_usage, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.data_usage_menu_cellular_networks: {
                final Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                startActivity(intent);
                return true;
            }
        }
        return false;
    }

    private void addDataPlanSection(int defaultSubId) {
        Context context = getPrefContext();
        addPreferencesFromResource(R.xml.data_plan_usage);
        PreferenceScreen screen = getPreferenceScreen();
        screen.setTitle(context.getString(R.string.data_usage_summary_title));

        PreferenceCategory preferenceCategory =
                (PreferenceCategory) findPreference(KEY_DATA_PLAN_USAGE);
        screen.addPreference(preferenceCategory);

        Preference dataPlansSyncTimePreference = new Preference(context);
        dataPlansSyncTimePreference.setLayoutResource(R.layout.data_plans_sync_time_preference);
        dataPlansSyncTimePreference.setTitle(MockDataPlanUsage.SYNC_TIME);
        preferenceCategory.addPreference(dataPlansSyncTimePreference);

        mDataPlanSummaryPreferenceList = new ArrayList<>(MockDataPlanUsage.DATA_PLAN_USAGES.length);
        for (int i = 0; i < MockDataPlanUsage.DATA_PLAN_USAGES.length; i++) {
            DataPlanSummaryPreference dataPlanSummaryPreference =
                    new DataPlanSummaryPreference(context);
            dataPlanSummaryPreference.setKey(KEY_STATUS_HEADER + (i + 1));
            mDataPlanSummaryPreferenceList.add(dataPlanSummaryPreference);
            preferenceCategory.addPreference(dataPlanSummaryPreference);
        }

        Preference preference = new Preference(context);
        preference.setLayoutResource(R.layout.manage_data_plans_preference);
        preferenceCategory.addPreference(preference);
        setPreferenceScreen(screen);

        mLimitPreference = findPreference(KEY_LIMIT_SUMMARY);
        List<SubscriptionInfo> subscriptions =
                services.mSubscriptionManager.getActiveSubscriptionInfoList();

        if (subscriptions == null || subscriptions.isEmpty()) {
            addMobileSection(defaultSubId);
        }

        for (int i = 0, subscriptionsSize = subscriptions != null ? subscriptions.size() : 0;
                i < subscriptionsSize; i++) {
            SubscriptionInfo subInfo = subscriptions.get(i);
            if (subscriptionsSize > 1) {
                addMobileSection(subInfo.getSubscriptionId(), subInfo);
            } else {
                addMobileSection(subInfo.getSubscriptionId());
            }
        }
    }

    private void addMobileSection(int subId) {
        addMobileSection(subId, null);
    }

    private void addMobileSection(int subId, SubscriptionInfo subInfo) {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_plan_usage_cell_data_preference_screen);
        category.setTemplate(getNetworkTemplate(subId), subId, services);
        category.pushTemplates(services);
        if (subInfo != null && !TextUtils.isEmpty(subInfo.getDisplayName())) {
            Preference title = category.findPreference(KEY_MOBILE_USAGE_TITLE);
            title.setTitle(subInfo.getDisplayName());
        }
    }

    private void addWifiSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_wifi);
        category.setTemplate(NetworkTemplate.buildTemplateWifiWildcard(), 0 /* subId */, services);
        mNetworkRestrictionPreference =
                (NetworkRestrictionsPreference) category.findPreference(KEY_NETWORK_RESTRICTIONS);
    }

    private void addEthernetSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_ethernet);
        category.setTemplate(NetworkTemplate.buildTemplateEthernet(), 0 /* subId */, services);
    }

    private Preference inflatePreferences(@IdRes int resId) {
        PreferenceScreen rootPreferences = getPreferenceManager().inflateFromResource(
                getPrefContext(), resId, null);
        Preference pref = rootPreferences.getPreference(0);
        rootPreferences.removeAll();

        PreferenceScreen screen = getPreferenceScreen();
        pref.setOrder(screen.getPreferenceCount());
        screen.addPreference(pref);

        return pref;
    }

    private NetworkTemplate getNetworkTemplate(int subscriptionId) {
        NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                services.mTelephonyManager.getSubscriberId(subscriptionId));
        return NetworkTemplate.normalize(mobileAll,
                services.mTelephonyManager.getMergedSubscriberIds());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private void updateState() {
        DataUsageController.DataUsageInfo info = mDataUsageController.getDataUsageInfo(
                mDefaultTemplate);

        Context context = getContext();
        mDataInfoController.updateDataLimit(info,
                services.mPolicyEditor.getPolicy(mDefaultTemplate));

        // TODO(b/63391323): Get rid of MockDataPlanUsage once we integrate with data plan APIs
        if (mDataPlanSummaryPreferenceList != null && !mDataPlanSummaryPreferenceList.isEmpty()) {
            MockDataPlanUsage[] dataPlanUsages = MockDataPlanUsage.getDataPlanUsage();
            for (int i = 0; i < dataPlanUsages.length; i++) {
                DataPlanSummaryPreference dataPlanSummaryPreference =
                        mDataPlanSummaryPreferenceList.get(i);
                MockDataPlanUsage dataPlanUsage = dataPlanUsages[i];
                dataPlanSummaryPreference.setTitle(dataPlanUsage.mUsage);
                dataPlanSummaryPreference.setUsageTextColor(dataPlanUsage.mUsageTextColor);
                dataPlanSummaryPreference.setName(dataPlanUsage.mName);
                dataPlanSummaryPreference.setPercentageUsage(dataPlanUsage.mPercentageUsage);
                dataPlanSummaryPreference
                        .setMeterBackgroundColor(dataPlanUsage.mMeterBackgroundColor);
                dataPlanSummaryPreference.setMeterConsumedColor(dataPlanUsage.mMeterConsumedColor);
                dataPlanSummaryPreference.setDescription(dataPlanUsage.mDescription);
            }
        }

        if (mLimitPreference != null && (info.warningLevel > 0 || info.limitLevel > 0)) {
            String warning = Formatter.formatFileSize(context, info.warningLevel);
            String limit = Formatter.formatFileSize(context, info.limitLevel);
            mLimitPreference.setSummary(getString(info.limitLevel <= 0 ? R.string.cell_warning_only
                    : R.string.cell_warning_and_limit, warning, limit));
        } else if (mLimitPreference != null) {
            mLimitPreference.setSummary(null);
        }

        updateNetworkRestrictionSummary(mNetworkRestrictionPreference);

        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 1, preferenceCount = screen.getPreferenceCount(); i < preferenceCount; i++) {
            ((TemplatePreferenceCategory) screen.getPreference(i)).pushTemplates(services);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DATA_USAGE_SUMMARY;
    }

    @VisibleForTesting
    void updateNetworkRestrictionSummary(NetworkRestrictionsPreference preference) {
        if (preference == null) {
            return;
        }
        mPolicyEditor.read();
        int count = 0;
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        for (int i = 0, configuredNetworksSize = configuredNetworks.size();
                i < configuredNetworksSize; i++) {
            WifiConfiguration config = configuredNetworks.get(i);
            if (isMetered(config)) {
                count++;
            }
        }
        preference.setSummary(getResources().getQuantityString(
                R.plurals.network_restrictions_summary, count, count));
    }

    @VisibleForTesting
    boolean isMetered(WifiConfiguration config) {
        if (config.SSID == null) {
            return false;
        }
        final String networkId = config.isPasspoint() ? config.providerFriendlyName : config.SSID;
        final NetworkPolicy policy =
                mPolicyEditor.getPolicyMaybeUnquoted(NetworkTemplate.buildTemplateWifi(networkId));
        if (policy == null) {
            return false;
        }
        if (policy.limitBytes != LIMIT_DISABLED) {
            return true;
        }
        return policy.metered;
    }

    private static class SummaryProvider
            implements SummaryLoader.SummaryProvider {

        private final Activity mActivity;
        private final SummaryLoader mSummaryLoader;
        private final DataUsageController mDataController;

        public SummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            mActivity = activity;
            mSummaryLoader = summaryLoader;
            mDataController = new DataUsageController(activity);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                DataUsageController.DataUsageInfo info = mDataController.getDataUsageInfo();
                String used;
                if (info == null) {
                    used = Formatter.formatFileSize(mActivity, 0);
                } else if (info.limitLevel <= 0) {
                    used = Formatter.formatFileSize(mActivity, info.usageLevel);
                } else {
                    used = Utils.formatPercentage(info.usageLevel, info.limitLevel);
                }
                mSummaryLoader.setSummary(this,
                        mActivity.getString(R.string.data_usage_summary_format, used));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = SummaryProvider::new;
}

