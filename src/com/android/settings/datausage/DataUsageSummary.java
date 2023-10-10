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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.RelativeSizeSpan;
import android.util.EventLog;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.datausage.lib.DataUsageLib;
import com.android.settings.network.ProxySubscriptionManager;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings preference fragment that displays data usage summary.
 */
public class DataUsageSummary extends DashboardFragment {

    private static final String TAG = "DataUsageSummary";

    static final boolean LOGD = false;

    public static final String KEY_RESTRICT_BACKGROUND = "restrict_background";

    // Mobile data keys
    public static final String KEY_MOBILE_USAGE_TITLE = "mobile_category";

    private ProxySubscriptionManager mProxySubscriptionMgr;

    @Override
    public int getHelpResource() {
        return R.string.help_url_data_usage;
    }

    public boolean isSimHardwareVisible(Context context) {
        return SubscriptionUtil.isSimHardwareVisible(context);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getContext();
        if (isGuestUser(context)) {
            Log.e(TAG, "This setting isn't available due to user restriction.");
            EventLog.writeEvent(0x534e4554, "262243574", -1 /* UID */, "Guest user");
            finish();
            return;
        }

        if (!isSimHardwareVisible(context) ||
            MobileNetworkUtils.isMobileNetworkUserRestricted(context)) {
            finish();
            return;
        }
        enableProxySubscriptionManager(context);

        boolean hasMobileData = DataUsageUtils.hasMobileData(context);

        final int defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            hasMobileData = false;
        }

        if (!hasMobileData || !UserManager.get(context).isAdminUser()) {
            removePreference(KEY_RESTRICT_BACKGROUND);
        }
        boolean hasWifiRadio = DataUsageUtils.hasWifiRadio(context);
        if (hasMobileData) {
            addMobileSection(defaultSubId);
            if (hasActiveSubscription() && hasWifiRadio) {
                // If the device has active SIM, the data usage section shows usage for mobile,
                // and the WiFi section is added if there is a WiFi radio - legacy behavior.
                addWifiSection();
            }
            // Do not add the WiFi section if either there is no WiFi radio (obviously) or if no
            // SIM is installed. In the latter case the data usage section will show WiFi usage and
            // there should be no explicit WiFi section added.
        } else if (hasWifiRadio) {
            addWifiSection();
        }
        if (DataUsageUtils.hasEthernet(context)) {
            addEthernetSection();
        }
        setHasOptionsMenu(true);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.data_usage;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final Activity activity = getActivity();
        final ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();
        if (!isSimHardwareVisible(context) ||
            MobileNetworkUtils.isMobileNetworkUserRestricted(context)) {
            return controllers;
        }
        final var mSummaryController = new DataUsageSummaryPreferenceController(activity,
                DataUsageUtils.getDefaultSubscriptionId(activity));
        controllers.add(mSummaryController);
        return controllers;
    }

    @VisibleForTesting
    void addMobileSection(int subId) {
        addMobileSection(subId, null);
    }

    @VisibleForTesting
    void enableProxySubscriptionManager(Context context) {
        // Enable ProxySubscriptionMgr with Lifecycle support for all controllers
        // live within this fragment
        mProxySubscriptionMgr = ProxySubscriptionManager.getInstance(context);
        mProxySubscriptionMgr.setLifecycle(getLifecycle());
    }

    @VisibleForTesting
    boolean hasActiveSubscription() {
        final List<SubscriptionInfo> subInfoList =
                mProxySubscriptionMgr.getActiveSubscriptionsInfo();
        return ((subInfoList != null) && (subInfoList.size() > 0));
    }

    private void addMobileSection(int subId, SubscriptionInfo subInfo) {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_cellular);
        category.setTemplate(DataUsageLib.getMobileTemplate(getContext(), subId), subId);
        category.pushTemplates();
        final CharSequence displayName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                subInfo, getContext());
        if (subInfo != null && !TextUtils.isEmpty(displayName)) {
            Preference title  = category.findPreference(KEY_MOBILE_USAGE_TITLE);
            title.setTitle(displayName);
        }
    }

    @VisibleForTesting
    void addWifiSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_wifi);
        category.setTemplate(new NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build(), 0);
    }

    private void addEthernetSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_ethernet);
        category.setTemplate(
                new NetworkTemplate.Builder(NetworkTemplate.MATCH_ETHERNET).build(), 0);
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

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    @VisibleForTesting
    static CharSequence formatUsage(Context context, String template, long usageLevel) {
        final float LARGER_SIZE = 1.25f * 1.25f;  // (1/0.8)^2
        final float SMALLER_SIZE = 1.0f / LARGER_SIZE;  // 0.8^2
        return formatUsage(context, template, usageLevel, LARGER_SIZE, SMALLER_SIZE);
    }

    static CharSequence formatUsage(Context context, String template, long usageLevel,
                                    float larger, float smaller) {
        final int FLAGS = Spannable.SPAN_INCLUSIVE_INCLUSIVE;

        final Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(),
                usageLevel, Formatter.FLAG_CALCULATE_ROUNDED | Formatter.FLAG_IEC_UNITS);
        final SpannableString enlargedValue = new SpannableString(usedResult.value);
        enlargedValue.setSpan(new RelativeSizeSpan(larger), 0, enlargedValue.length(), FLAGS);

        final SpannableString amountTemplate = new SpannableString(
                context.getString(com.android.internal.R.string.fileSizeSuffix)
                .replace("%1$s", "^1").replace("%2$s", "^2"));
        final CharSequence formattedUsage = TextUtils.expandTemplate(amountTemplate,
                enlargedValue, usedResult.units);

        final SpannableString fullTemplate = new SpannableString(template);
        fullTemplate.setSpan(new RelativeSizeSpan(smaller), 0, fullTemplate.length(), FLAGS);
        return TextUtils.expandTemplate(fullTemplate,
                BidiFormatter.getInstance().unicodeWrap(formattedUsage.toString()));
    }

    private void updateState() {
        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 1; i < screen.getPreferenceCount(); i++) {
            Preference currentPreference = screen.getPreference(i);
            if (currentPreference instanceof TemplatePreferenceCategory) {
                ((TemplatePreferenceCategory) currentPreference).pushTemplates();
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DATA_USAGE_SUMMARY;
    }

    private static boolean isGuestUser(Context context) {
        if (context == null) return false;
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) return false;
        return userManager.isGuestUser();
    }
}
