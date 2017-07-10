/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import static com.android.settings.network.MobilePlanPreferenceController
        .MANAGE_MOBILE_PLAN_DIALOG_ID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.network.MobilePlanPreferenceController.MobilePlanPreferenceHost;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.WifiMasterSwitchPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkDashboardFragment extends DashboardFragment implements
        MobilePlanPreferenceHost {

    private static final String TAG = "NetworkDashboardFrag";

    private NetworkResetActionMenuController mNetworkResetController;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_NETWORK_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_and_internet;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mNetworkResetController = new NetworkResetActionMenuController(context);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_network_dashboard;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mNetworkResetController.buildMenuItem(menu);
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), mMetricsFeatureProvider, this
                /* fragment */,
                this /* mobilePlanHost */);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, MetricsFeatureProvider metricsFeatureProvider, Fragment fragment,
            MobilePlanPreferenceHost mobilePlanHost) {
        final AirplaneModePreferenceController airplaneModePreferenceController =
                new AirplaneModePreferenceController(context, fragment);
        final MobilePlanPreferenceController mobilePlanPreferenceController =
                new MobilePlanPreferenceController(context, mobilePlanHost);
        final WifiMasterSwitchPreferenceController wifiPreferenceController =
                new WifiMasterSwitchPreferenceController(context, metricsFeatureProvider);
        final MobileNetworkPreferenceController mobileNetworkPreferenceController =
                new MobileNetworkPreferenceController(context);
        final VpnPreferenceController vpnPreferenceController =
                new VpnPreferenceController(context);

        if (lifecycle != null) {
            lifecycle.addObserver(airplaneModePreferenceController);
            lifecycle.addObserver(mobilePlanPreferenceController);
            lifecycle.addObserver(wifiPreferenceController);
            lifecycle.addObserver(mobileNetworkPreferenceController);
            lifecycle.addObserver(vpnPreferenceController);
        }

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(airplaneModePreferenceController);
        controllers.add(mobileNetworkPreferenceController);
        controllers.add(new TetherPreferenceController(context, lifecycle));
        controllers.add(vpnPreferenceController);
        controllers.add(new ProxyPreferenceController(context));
        controllers.add(mobilePlanPreferenceController);
        controllers.add(wifiPreferenceController);
        return controllers;
    }

    @Override
    public void showMobilePlanMessageDialog() {
        showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Log.d(TAG, "onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                final MobilePlanPreferenceController controller =
                        getPreferenceController(MobilePlanPreferenceController.class);
                return new AlertDialog.Builder(getActivity())
                        .setMessage(controller.getMobilePlanDialogMessage())
                        .setCancelable(false)
                        .setPositiveButton(com.android.internal.R.string.ok,
                                (dialog, id) -> controller.setMobilePlanDialogMessage(null))
                        .create();
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (MANAGE_MOBILE_PLAN_DIALOG_ID == dialogId) {
            return MetricsProto.MetricsEvent.DIALOG_MANAGE_MOBILE_PLAN;
        }
        return 0;
    }

    @VisibleForTesting
    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final MobileNetworkPreferenceController mMobileNetworkPreferenceController;
        private final TetherPreferenceController mTetherPreferenceController;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this(context, summaryLoader,
                    new MobileNetworkPreferenceController(context),
                    new TetherPreferenceController(context, null /* lifecycle */));
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        SummaryProvider(Context context, SummaryLoader summaryLoader,
                MobileNetworkPreferenceController mobileNetworkPreferenceController,
                TetherPreferenceController tetherPreferenceController) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mMobileNetworkPreferenceController = mobileNetworkPreferenceController;
            mTetherPreferenceController = tetherPreferenceController;
        }


        @Override
        public void setListening(boolean listening) {
            if (listening) {
                String summary = mContext.getString(R.string.wifi_settings_title);
                if (mMobileNetworkPreferenceController.isAvailable()) {
                    final String mobileSettingSummary = mContext.getString(
                            R.string.network_dashboard_summary_mobile);
                    summary = mContext.getString(R.string.join_many_items_middle, summary,
                            mobileSettingSummary);
                }
                final String dataUsageSettingSummary = mContext.getString(
                        R.string.network_dashboard_summary_data_usage);
                summary = mContext.getString(R.string.join_many_items_middle, summary,
                        dataUsageSettingSummary);
                if (mTetherPreferenceController.isAvailable()) {
                    final String hotspotSettingSummary = mContext.getString(
                            R.string.network_dashboard_summary_hotspot);
                    summary = mContext.getString(R.string.join_many_items_middle, summary,
                            hotspotSettingSummary);
                }
                mSummaryLoader.setSummary(this, summary);
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


    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_and_internet;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* metricsFeatureProvider */, null /* fragment */,
                            null /* mobilePlanHost */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Remove master switch as a result
                    keys.add(WifiMasterSwitchPreferenceController.KEY_TOGGLE_WIFI);
                    return keys;
                }
            };
}
