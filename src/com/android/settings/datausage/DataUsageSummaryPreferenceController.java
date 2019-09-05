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

package com.android.settings.datausage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.text.TextUtils;
import android.util.Log;
import android.util.RecurrenceRule;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.net.DataUsageController;

import java.util.List;

/**
 * This is the controller for a data usage header that retrieves carrier data from the new
 * subscriptions framework API if available. The controller reads subscription information from the
 * framework and falls back to legacy usage data if none are available.
 */
public class DataUsageSummaryPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart {

    private static final String TAG = "DataUsageController";
    private static final String KEY = "status_header";
    private static final long PETA = 1000000000000000L;
    private static final float RELATIVE_SIZE_LARGE = 1.25f * 1.25f;  // (1/0.8)^2
    private static final float RELATIVE_SIZE_SMALL = 1.0f / RELATIVE_SIZE_LARGE;  // 0.8^2

    private final Activity mActivity;
    private final EntityHeaderController mEntityHeaderController;
    private final Lifecycle mLifecycle;
    private final PreferenceFragmentCompat mFragment;
    protected final DataUsageController mDataUsageController;
    protected final DataUsageInfoController mDataInfoController;
    private final NetworkTemplate mDefaultTemplate;
    protected final NetworkPolicyEditor mPolicyEditor;
    private final int mDataUsageTemplate;
    private final boolean mHasMobileData;
    private final SubscriptionManager mSubscriptionManager;

    /** Name of the carrier, or null if not available */
    private CharSequence mCarrierName;

    /** The number of registered plans, [0,N] */
    private int mDataplanCount;

    /** The time of the last update in milliseconds since the epoch, or -1 if unknown */
    private long mSnapshotTime;

    /**
     * The size of the first registered plan if one exists or the size of the warning if it is set.
     * -1 if no information is available.
     */
    private long mDataplanSize;
    /** The "size" of the data usage bar, i.e. the amount of data its rhs end represents */
    private long mDataBarSize;
    /** The number of bytes used since the start of the cycle. */
    private long mDataplanUse;
    /** The starting time of the billing cycle in ms since the epoch */
    private long mCycleStart;
    /** The ending time of the billing cycle in ms since the epoch */
    private long mCycleEnd;
    /** The subscription that we should show usage for. */
    private int mSubscriptionId;

    private Intent mManageSubscriptionIntent;

    public DataUsageSummaryPreferenceController(Activity activity,
            Lifecycle lifecycle, PreferenceFragmentCompat fragment, int subscriptionId) {
        super(activity, KEY);

        mActivity = activity;
        mEntityHeaderController = EntityHeaderController.newInstance(activity,
                fragment, null);
        mLifecycle = lifecycle;
        mFragment = fragment;
        mSubscriptionId = subscriptionId;

        mDefaultTemplate = DataUsageUtils.getDefaultTemplate(activity, mSubscriptionId);
        NetworkPolicyManager policyManager = activity.getSystemService(NetworkPolicyManager.class);
        mPolicyEditor = new NetworkPolicyEditor(policyManager);

        mHasMobileData = DataUsageUtils.hasMobileData(activity)
                && mSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        mDataUsageController = new DataUsageController(activity);
        mDataUsageController.setSubscriptionId(mSubscriptionId);
        mDataInfoController = new DataUsageInfoController();

        if (mHasMobileData) {
            mDataUsageTemplate = R.string.cell_data_template;
        } else if (DataUsageUtils.hasWifiRadio(activity)) {
            mDataUsageTemplate = R.string.wifi_data_template;
        } else {
            mDataUsageTemplate = R.string.ethernet_data_template;
        }

        mSubscriptionManager = (SubscriptionManager)
                mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    @VisibleForTesting
    DataUsageSummaryPreferenceController(
            DataUsageController dataUsageController,
            DataUsageInfoController dataInfoController,
            NetworkTemplate defaultTemplate,
            NetworkPolicyEditor policyEditor,
            int dataUsageTemplate,
            boolean hasMobileData,
            SubscriptionManager subscriptionManager,
            Activity activity,
            Lifecycle lifecycle,
            EntityHeaderController entityHeaderController,
            PreferenceFragmentCompat fragment,
            int subscriptionId) {
        super(activity, KEY);
        mDataUsageController = dataUsageController;
        mDataInfoController = dataInfoController;
        mDefaultTemplate = defaultTemplate;
        mPolicyEditor = policyEditor;
        mDataUsageTemplate = dataUsageTemplate;
        mHasMobileData = hasMobileData;
        mSubscriptionManager = subscriptionManager;
        mActivity = activity;
        mLifecycle = lifecycle;
        mEntityHeaderController = entityHeaderController;
        mFragment = fragment;
        mSubscriptionId = subscriptionId;
    }

    @Override
    public void onStart() {
        RecyclerView view = mFragment.getListView();
        mEntityHeaderController.setRecyclerView(view, mLifecycle);
        mEntityHeaderController.styleActionBar(mActivity);
    }

    @VisibleForTesting
    void setPlanValues(int dataPlanCount, long dataPlanSize, long dataPlanUse) {
        mDataplanCount = dataPlanCount;
        mDataplanSize = dataPlanSize;
        mDataBarSize = dataPlanSize;
        mDataplanUse = dataPlanUse;
    }

    @VisibleForTesting
    void setCarrierValues(String carrierName, long snapshotTime, long cycleEnd, Intent intent) {
        mCarrierName = carrierName;
        mSnapshotTime = snapshotTime;
        mCycleEnd = cycleEnd;
        mManageSubscriptionIntent = intent;
    }

    @Override
    public int getAvailabilityStatus() {
        return DataUsageUtils.hasSim(mActivity)
                || DataUsageUtils.hasWifiRadio(mContext) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        DataUsageSummaryPreference summaryPreference = (DataUsageSummaryPreference) preference;

        final DataUsageController.DataUsageInfo info;
        if (DataUsageUtils.hasSim(mActivity)) {
            info = mDataUsageController.getDataUsageInfo(mDefaultTemplate);
            mDataInfoController.updateDataLimit(info, mPolicyEditor.getPolicy(mDefaultTemplate));
            summaryPreference.setWifiMode(/* isWifiMode */ false,
                    /* usagePeriod */ null, /* isSingleWifi */ false);
        } else {
            info = mDataUsageController.getDataUsageInfo(
                    NetworkTemplate.buildTemplateWifiWildcard());
            summaryPreference.setWifiMode(/* isWifiMode */ true, /* usagePeriod */
                    info.period, /* isSingleWifi */ false);
            summaryPreference.setLimitInfo(null);
            summaryPreference.setUsageNumbers(info.usageLevel,
                    /* dataPlanSize */ -1L,
                    /* hasMobileData */ true);
            summaryPreference.setChartEnabled(false);
            summaryPreference.setUsageInfo(info.cycleEnd,
                    /* snapshotTime */ -1L,
                    /* carrierName */ null,
                    /* numPlans */ 0,
                    /* launchIntent */ null);
            return;
        }

        if (mSubscriptionManager != null) {
            refreshDataplanInfo(info);
        }

        if (info.warningLevel > 0 && info.limitLevel > 0) {
            summaryPreference.setLimitInfo(TextUtils.expandTemplate(
                    mContext.getText(R.string.cell_data_warning_and_limit),
                    DataUsageUtils.formatDataUsage(mContext, info.warningLevel),
                    DataUsageUtils.formatDataUsage(mContext, info.limitLevel)));
        } else if (info.warningLevel > 0) {
            summaryPreference.setLimitInfo(TextUtils.expandTemplate(
                    mContext.getText(R.string.cell_data_warning),
                    DataUsageUtils.formatDataUsage(mContext, info.warningLevel)));
        } else if (info.limitLevel > 0) {
            summaryPreference.setLimitInfo(TextUtils.expandTemplate(
                    mContext.getText(R.string.cell_data_limit),
                    DataUsageUtils.formatDataUsage(mContext, info.limitLevel)));
        } else {
            summaryPreference.setLimitInfo(null);
        }

        summaryPreference.setUsageNumbers(mDataplanUse, mDataplanSize, mHasMobileData);

        if (mDataBarSize <= 0) {
            summaryPreference.setChartEnabled(false);
        } else {
            summaryPreference.setChartEnabled(true);
            summaryPreference.setLabels(DataUsageUtils.formatDataUsage(mContext, 0 /* sizeBytes */),
                    DataUsageUtils.formatDataUsage(mContext, mDataBarSize));
            summaryPreference.setProgress(mDataplanUse / (float) mDataBarSize);
        }
        summaryPreference.setUsageInfo(mCycleEnd, mSnapshotTime, mCarrierName,
                mDataplanCount, mManageSubscriptionIntent);
    }

    // TODO(b/70950124) add test for this method once the robolectric shadow run script is
    // completed (b/3526807)
    private void refreshDataplanInfo(DataUsageController.DataUsageInfo info) {
        // reset data before overwriting
        mCarrierName = null;
        mDataplanCount = 0;
        mDataplanSize = -1L;
        mDataBarSize = mDataInfoController.getSummaryLimit(info);
        mDataplanUse = info.usageLevel;
        mCycleStart = info.cycleStart;
        mCycleEnd = info.cycleEnd;
        mSnapshotTime = -1L;

        SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfo(mSubscriptionId);
        if (subInfo == null) {
            subInfo = mSubscriptionManager.getAllSubscriptionInfoList().stream().filter(
                    i -> i.getSubscriptionId() == mSubscriptionId).findFirst().orElse(null);
        }
        if (subInfo != null && mHasMobileData) {
            mCarrierName = subInfo.getCarrierName();
            List<SubscriptionPlan> plans = mSubscriptionManager.getSubscriptionPlans(
                    mSubscriptionId);
            final SubscriptionPlan primaryPlan = getPrimaryPlan(mSubscriptionManager,
                    mSubscriptionId);

            if (primaryPlan != null) {
                mDataplanCount = plans.size();
                mDataplanSize = primaryPlan.getDataLimitBytes();
                if (unlimited(mDataplanSize)) {
                    mDataplanSize = -1L;
                }
                mDataBarSize = mDataplanSize;
                mDataplanUse = primaryPlan.getDataUsageBytes();

                RecurrenceRule rule = primaryPlan.getCycleRule();
                if (rule != null && rule.start != null && rule.end != null) {
                    mCycleStart = rule.start.toEpochSecond() * 1000L;
                    mCycleEnd = rule.end.toEpochSecond() * 1000L;
                }
                mSnapshotTime = primaryPlan.getDataUsageTime();
            }
        }
        mManageSubscriptionIntent =
                mSubscriptionManager.createManageSubscriptionIntent(mSubscriptionId);
        Log.i(TAG, "Have " + mDataplanCount + " plans, dflt sub-id " + mSubscriptionId
                + ", intent " + mManageSubscriptionIntent);
    }

    public static SubscriptionPlan getPrimaryPlan(SubscriptionManager subManager, int primaryId) {
        List<SubscriptionPlan> plans = subManager.getSubscriptionPlans(primaryId);
        if (CollectionUtils.isEmpty(plans)) {
            return null;
        }
        // First plan in the list is the primary plan
        SubscriptionPlan plan = plans.get(0);
        return plan.getDataLimitBytes() > 0
                && saneSize(plan.getDataUsageBytes())
                && plan.getCycleRule() != null ? plan : null;
    }

    private static boolean saneSize(long value) {
        return value >= 0L && value < PETA;
    }

    public static boolean unlimited(long size) {
        return size == SubscriptionPlan.BYTES_UNLIMITED;
    }
}
