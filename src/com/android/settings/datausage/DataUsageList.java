/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.content.Intent;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.EventLog;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.datausage.lib.BillingCycleRepository;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.network.MobileNetworkRepository;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.net.NetworkCycleChartData;
import com.android.settingslib.net.NetworkCycleChartDataLoader;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import kotlin.Unit;

/**
 * Panel showing data usage history across various networks, including options
 * to inspect based on usage cycle and control through {@link NetworkPolicy}.
 */
public class DataUsageList extends DataUsageBaseFragment
        implements MobileDataEnabledListener.Client {

    static final String EXTRA_SUB_ID = "sub_id";
    static final String EXTRA_NETWORK_TEMPLATE = "network_template";

    private static final String TAG = "DataUsageList";
    private static final boolean LOGD = false;

    private static final String KEY_USAGE_AMOUNT = "usage_amount";
    private static final String KEY_CHART_DATA = "chart_data";
    private static final String KEY_TEMPLATE = "template";
    private static final String KEY_APP = "app";

    @VisibleForTesting
    static final int LOADER_CHART_DATA = 2;

    @VisibleForTesting
    MobileDataEnabledListener mDataStateListener;

    @VisibleForTesting
    NetworkTemplate mTemplate;
    @VisibleForTesting
    int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    @VisibleForTesting
    LoadingViewController mLoadingViewController;

    private ChartDataUsagePreference mChart;

    @Nullable
    private List<NetworkCycleChartData> mCycleData;

    // Spinner will keep the selected cycle even after paused, this only keeps the displayed cycle,
    // which need be cleared when resumed.
    private CycleAdapter.CycleItem mLastDisplayedCycle;
    private Preference mUsageAmount;
    private MobileNetworkRepository mMobileNetworkRepository;
    private SubscriptionInfoEntity mSubscriptionInfoEntity;
    private DataUsageListAppsController mDataUsageListAppsController;
    private BillingCycleRepository mBillingCycleRepository;
    @VisibleForTesting
    DataUsageListHeaderController mDataUsageListHeaderController;

    private boolean mIsBillingCycleModifiable = false;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DATA_USAGE_LIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isGuestUser(getContext())) {
            Log.e(TAG, "This setting isn't available for guest user");
            EventLog.writeEvent(0x534e4554, "262741858", -1 /* UID */, "Guest user");
            finish();
            return;
        }

        final Activity activity = getActivity();
        mBillingCycleRepository = createBillingCycleRepository();
        if (!mBillingCycleRepository.isBandwidthControlEnabled()) {
            Log.w(TAG, "No bandwidth control; leaving");
            activity.finish();
            return;
        }

        mUsageAmount = findPreference(KEY_USAGE_AMOUNT);
        mChart = findPreference(KEY_CHART_DATA);

        processArgument();
        if (mTemplate == null) {
            Log.e(TAG, "No template; leaving");
            finish();
            return;
        }
        updateSubscriptionInfoEntity();
        mDataStateListener = new MobileDataEnabledListener(activity, this);
        mDataUsageListAppsController = use(DataUsageListAppsController.class);
        mDataUsageListAppsController.init(mTemplate);
    }

    @VisibleForTesting
    @NonNull
    BillingCycleRepository createBillingCycleRepository() {
        return new BillingCycleRepository(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mDataUsageListHeaderController = new DataUsageListHeaderController(
                setPinnedHeaderView(R.layout.apps_filter_spinner),
                mTemplate,
                getMetricsCategory(),
                (cycle, position) -> {
                    updateSelectedCycle(cycle, position);
                    return Unit.INSTANCE;
                }
        );

        mLoadingViewController = new LoadingViewController(
                getView().findViewById(R.id.loading_container), getListView());
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoadingViewController.showLoadingViewDelayed();
        mDataStateListener.start(mSubId);
        mLastDisplayedCycle = null;
        updatePolicy();

        // kick off loader for network history
        // TODO: consider chaining two loaders together instead of reloading
        // network history when showing app detail.
        getLoaderManager().restartLoader(LOADER_CHART_DATA,
                buildArgs(mTemplate), mNetworkCycleDataCallbacks);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataStateListener.stop();

        getLoaderManager().destroyLoader(LOADER_CHART_DATA);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.data_usage_list;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    void processArgument() {
        final Bundle args = getArguments();
        if (args != null) {
            mSubId = args.getInt(EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mTemplate = args.getParcelable(EXTRA_NETWORK_TEMPLATE);
        }
        if (mTemplate == null && mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            final Intent intent = getIntent();
            mSubId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mTemplate = intent.getParcelableExtra(Settings.EXTRA_NETWORK_TEMPLATE);

            if (mTemplate == null) {
                Optional<NetworkTemplate> mobileNetworkTemplateFromSim =
                        DataUsageUtils.getMobileNetworkTemplateFromSubId(getContext(), getIntent());
                if (mobileNetworkTemplateFromSim.isPresent()) {
                    mTemplate = mobileNetworkTemplateFromSim.get();
                }
            }
        }
    }

    @VisibleForTesting
    void updateSubscriptionInfoEntity() {
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(getContext());
        ThreadUtils.postOnBackgroundThread(() -> {
            mSubscriptionInfoEntity = mMobileNetworkRepository.getSubInfoById(
                    String.valueOf(mSubId));
        });
    }

    /**
     * Implementation of {@code MobileDataEnabledListener.Client}
     */
    public void onMobileDataEnabledChange() {
        updatePolicy();
    }

    private Bundle buildArgs(NetworkTemplate template) {
        final Bundle args = new Bundle();
        args.putParcelable(KEY_TEMPLATE, template);
        args.putParcelable(KEY_APP, null);
        return args;
    }

    /**
     * Update chart sweeps and cycle list to reflect {@link NetworkPolicy} for
     * current {@link #mTemplate}.
     */
    @VisibleForTesting
    void updatePolicy() {
        mIsBillingCycleModifiable = isBillingCycleModifiable();
        if (mIsBillingCycleModifiable) {
            mChart.setNetworkPolicy(services.mPolicyEditor.getPolicy(mTemplate));
        } else {
            mChart.setNetworkPolicy(null);  // don't bind warning / limit sweeps
        }
        updateConfigButtonVisibility();
    }

    @VisibleForTesting
    boolean isBillingCycleModifiable() {
        return mBillingCycleRepository.isModifiable(mSubId)
                && SubscriptionManager.from(requireContext())
                .getActiveSubscriptionInfo(mSubId) != null;
    }

    private void updateConfigButtonVisibility() {
        mDataUsageListHeaderController.setConfigButtonVisible(
                mIsBillingCycleModifiable && mCycleData != null);
    }

    /**
     * Updates the chart and detail data when initial loaded or selected cycle changed.
     */
    private void updateSelectedCycle(CycleAdapter.CycleItem cycle, int position) {
        // Avoid from updating UI after #onStop.
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }

        // Avoid from updating UI when async query still on-going.
        // This could happen when a request from #onMobileDataEnabledChange.
        if (mCycleData == null) {
            return;
        }

        if (Objects.equals(cycle, mLastDisplayedCycle)) {
            // Avoid duplicate update to avoid page flash.
            return;
        }
        mLastDisplayedCycle = cycle;

        if (LOGD) {
            Log.d(TAG, "showing cycle " + cycle + ", [start=" + cycle.start + ", end="
                    + cycle.end + "]");
        }

        // update chart to show selected cycle, and update detail data
        // to match updated sweep bounds.
        NetworkCycleChartData cycleChartData = mCycleData.get(position);
        mChart.setNetworkCycleData(cycleChartData);

        updateDetailData(cycleChartData);
    }

    /**
     * Update details based on {@link #mChart} inspection range depending on
     * current mode. Updates {@link #mAdapter} with sorted list
     * of applications data usage.
     */
    private void updateDetailData(NetworkCycleChartData cycleChartData) {
        if (LOGD) Log.d(TAG, "updateDetailData()");

        // kick off loader for detailed stats
        mDataUsageListAppsController.update(
                mSubscriptionInfoEntity == null ? null : mSubscriptionInfoEntity.carrierId,
                cycleChartData.getStartTime(),
                cycleChartData.getEndTime()
        );

        final long totalBytes = cycleChartData.getTotalUsage();
        final CharSequence totalPhrase = DataUsageUtils.formatDataUsage(getActivity(), totalBytes);
        mUsageAmount.setTitle(getString(R.string.data_used_template, totalPhrase));
    }

    @VisibleForTesting
    final LoaderCallbacks<List<NetworkCycleChartData>> mNetworkCycleDataCallbacks =
            new LoaderCallbacks<>() {
                @Override
                @NonNull
                public Loader<List<NetworkCycleChartData>> onCreateLoader(int id, Bundle args) {
                    return NetworkCycleChartDataLoader.builder(getContext())
                            .setNetworkTemplate(mTemplate)
                            .build();
                }

                @Override
                public void onLoadFinished(@NonNull Loader<List<NetworkCycleChartData>> loader,
                        List<NetworkCycleChartData> data) {
                    mLoadingViewController.showContent(false /* animate */);
                    mCycleData = data;
                    mDataUsageListHeaderController.updateCycleData(mCycleData);
                    updateConfigButtonVisibility();
                    mDataUsageListAppsController.setCycleData(mCycleData);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<NetworkCycleChartData>> loader) {
                    mCycleData = null;
                }
            };

    private static boolean isGuestUser(Context context) {
        if (context == null) return false;
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) return false;
        return userManager.isGuestUser();
    }
}
