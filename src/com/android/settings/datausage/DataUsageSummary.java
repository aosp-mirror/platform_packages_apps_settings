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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SummaryPreference;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

import java.util.ArrayList;
import java.util.List;

import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_WIFI;

public class DataUsageSummary extends DataUsageBase implements Indexable, DataUsageEditController {

    private static final String TAG = "DataUsageSummary";
    static final boolean LOGD = false;

    public static final boolean TEST_RADIOS = false;
    public static final String TEST_RADIOS_PROP = "test.radios";

    private static final String KEY_STATUS_HEADER = "status_header";
    private static final String KEY_LIMIT_SUMMARY = "limit_summary";
    private static final String KEY_RESTRICT_BACKGROUND = "restrict_background";

    private DataUsageController mDataUsageController;
    private DataUsageInfoController mDataInfoController;
    private SummaryPreference mSummaryPreference;
    private Preference mLimitPreference;
    private NetworkTemplate mDefaultTemplate;
    private int mDataUsageTemplate;

    @Override
    protected int getHelpResource() {
        return R.string.help_url_data_usage;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        boolean hasMobileData = hasMobileData(getContext());
        mDataUsageController = new DataUsageController(getContext());
        mDataInfoController = new DataUsageInfoController();
        addPreferencesFromResource(R.xml.data_usage);

        int defaultSubId = getDefaultSubscriptionId(getContext());
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            hasMobileData = false;
        }
        mDefaultTemplate = getDefaultTemplate(getContext(), defaultSubId);
        mSummaryPreference = (SummaryPreference) findPreference(KEY_STATUS_HEADER);

        if (!hasMobileData || !isAdmin()) {
            removePreference(KEY_RESTRICT_BACKGROUND);
        }
        if (hasMobileData) {
            mLimitPreference = findPreference(KEY_LIMIT_SUMMARY);
            List<SubscriptionInfo> subscriptions =
                    services.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptions == null || subscriptions.size() == 0) {
                addMobileSection(defaultSubId);
            }
            for (int i = 0; subscriptions != null && i < subscriptions.size(); i++) {
                addMobileSection(subscriptions.get(i).getSubscriptionId());
            }
            mSummaryPreference.setSelectable(true);
        } else {
            removePreference(KEY_LIMIT_SUMMARY);
            mSummaryPreference.setSelectable(false);
        }
        boolean hasWifiRadio = hasWifiRadio(getContext());
        if (hasWifiRadio) {
            addWifiSection();
        }
        if (hasEthernet(getContext())) {
            addEthernetSection();
        }
        mDataUsageTemplate = hasMobileData ? R.string.cell_data_template
                : hasWifiRadio ? R.string.wifi_data_template
                : R.string.ethernet_data_template;

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
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(new ComponentName("com.android.phone",
                        "com.android.phone.MobileNetworkSettings"));
                startActivity(intent);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == findPreference(KEY_STATUS_HEADER)) {
            BillingCycleSettings.BytesEditorFragment.show(this, false);
            return false;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void addMobileSection(int subId) {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_cellular);
        category.setTemplate(getNetworkTemplate(subId), subId, services);
        category.pushTemplates(services);
    }

    private void addWifiSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_wifi);
        category.setTemplate(NetworkTemplate.buildTemplateWifiWildcard(), 0, services);
    }

    private void addEthernetSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_ethernet);
        category.setTemplate(NetworkTemplate.buildTemplateEthernet(), 0, services);
    }

    private Preference inflatePreferences(int resId) {
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

    private static void verySmallSpanExcept(SpannableString s, CharSequence exception) {
        final float SIZE = 0.8f * 0.8f;
        final int FLAGS = Spannable.SPAN_INCLUSIVE_INCLUSIVE;
        final int exceptionStart = TextUtils.indexOf(s, exception);
        if (exceptionStart == -1) {
           s.setSpan(new RelativeSizeSpan(SIZE), 0, s.length(), FLAGS);
        } else {
            if (exceptionStart > 0) {
                s.setSpan(new RelativeSizeSpan(SIZE), 0, exceptionStart, FLAGS);
            }
            final int exceptionEnd = exceptionStart + exception.length();
            if (exceptionEnd < s.length()) {
                s.setSpan(new RelativeSizeSpan(SIZE), exceptionEnd, s.length(), FLAGS);
            }
        }
    }

    private static CharSequence formatTitle(Context context, String template, long usageLevel) {
        final SpannableString amountTemplate = new SpannableString(
                context.getString(com.android.internal.R.string.fileSizeSuffix)
                .replace("%1$s", "^1").replace("%2$s", "^2"));
        verySmallSpanExcept(amountTemplate, "^1");
        final Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(),
                usageLevel, Formatter.FLAG_SHORTER);
        final CharSequence formattedUsage = TextUtils.expandTemplate(amountTemplate,
                usedResult.value, usedResult.units);

        final SpannableString fullTemplate = new SpannableString(template.replace("%1$s", "^1"));
        verySmallSpanExcept(fullTemplate, "^1");
        return TextUtils.expandTemplate(fullTemplate,
                BidiFormatter.getInstance().unicodeWrap(formattedUsage));
    }

    private void updateState() {
        DataUsageController.DataUsageInfo info = mDataUsageController.getDataUsageInfo(
                mDefaultTemplate);
        Context context = getContext();

        mDataInfoController.updateDataLimit(info,
                services.mPolicyEditor.getPolicy(mDefaultTemplate));

        if (mSummaryPreference != null) {
            mSummaryPreference.setTitle(
                    formatTitle(context, getString(mDataUsageTemplate), info.usageLevel));
            long limit = mDataInfoController.getSummaryLimit(info);
            mSummaryPreference.setSummary(info.period);

            if (limit <= 0) {
                mSummaryPreference.setChartEnabled(false);
            } else {
                mSummaryPreference.setChartEnabled(true);
                mSummaryPreference.setLabels(Formatter.formatFileSize(context, 0),
                        Formatter.formatFileSize(context, limit));
                mSummaryPreference.setRatios(info.usageLevel / (float) limit, 0,
                        (limit - info.usageLevel) / (float) limit);
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

        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 1; i < screen.getPreferenceCount(); i++) {
            ((TemplatePreferenceCategory) screen.getPreference(i)).pushTemplates(services);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DATA_USAGE_SUMMARY;
    }

    @Override
    public NetworkPolicyEditor getNetworkPolicyEditor() {
        return services.mPolicyEditor;
    }

    @Override
    public NetworkTemplate getNetworkTemplate() {
        return mDefaultTemplate;
    }

    @Override
    public void updateDataUsage() {
        updateState();
    }

    /**
     * Test if device has an ethernet network connection.
     */
    public boolean hasEthernet(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("ethernet");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final boolean hasEthernet = conn.isNetworkSupported(TYPE_ETHERNET);

        final long ethernetBytes;
        try {
            INetworkStatsSession statsSession = services.mStatsService.openSession();
            if (statsSession != null) {
                ethernetBytes = statsSession.getSummaryForNetwork(
                        NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE)
                        .getTotalBytes();
                TrafficStats.closeQuietly(statsSession);
            } else {
                ethernetBytes = 0;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        // only show ethernet when both hardware present and traffic has occurred
        return hasEthernet && ethernetBytes > 0;
    }

    public static boolean hasMobileData(Context context) {
        return ConnectivityManager.from(context).isNetworkSupported(
                ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Test if device has a Wi-Fi data radio.
     */
    public static boolean hasWifiRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("wifi");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        return conn.isNetworkSupported(TYPE_WIFI);
    }

    public static int getDefaultSubscriptionId(Context context) {
        SubscriptionManager subManager = SubscriptionManager.from(context);
        if (subManager == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        SubscriptionInfo subscriptionInfo = subManager.getDefaultDataSubscriptionInfo();
        if (subscriptionInfo == null) {
            List<SubscriptionInfo> list = subManager.getAllSubscriptionInfoList();
            if (list.size() == 0) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            subscriptionInfo = list.get(0);
        }
        return subscriptionInfo.getSubscriptionId();
    }

    public static NetworkTemplate getDefaultTemplate(Context context, int defaultSubId) {
        if (hasMobileData(context) && defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                    telephonyManager.getSubscriberId(defaultSubId));
            return NetworkTemplate.normalize(mobileAll,
                    telephonyManager.getMergedSubscriberIds());
        } else if (hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        } else {
            return NetworkTemplate.buildTemplateEthernet();
        }
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
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    /**
     * For search
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                ArrayList<SearchIndexableResource> resources = new ArrayList<>();
                SearchIndexableResource resource = new SearchIndexableResource(context);
                resource.xmlResId = R.xml.data_usage;
                resources.add(resource);

                if (hasMobileData(context)) {
                    resource = new SearchIndexableResource(context);
                    resource.xmlResId = R.xml.data_usage_cellular;
                    resources.add(resource);
                }
                if (hasWifiRadio(context)) {
                    resource = new SearchIndexableResource(context);
                    resource.xmlResId = R.xml.data_usage_wifi;
                    resources.add(resource);
                }
                return resources;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                ArrayList<String> keys = new ArrayList<>();
                boolean hasMobileData = ConnectivityManager.from(context).isNetworkSupported(
                        ConnectivityManager.TYPE_MOBILE);

                if (hasMobileData) {
                    keys.add(KEY_RESTRICT_BACKGROUND);
                }

                return keys;
            }
        };
}
