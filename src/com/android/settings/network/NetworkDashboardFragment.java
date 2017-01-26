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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.WifiMasterSwitchPreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.android.settings.network.MobilePlanPreferenceController
        .MANAGE_MOBILE_PLAN_DIALOG_ID;

public class NetworkDashboardFragment extends DashboardFragment implements
        MobilePlanPreferenceController.MobilePlanPreferenceHost {

    private static final String TAG = "NetworkDashboardFrag";

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
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final AirplaneModePreferenceController airplaneModePreferenceController =
                new AirplaneModePreferenceController(context, this /* fragment */);
        final MobilePlanPreferenceController mobilePlanPreferenceController =
                new MobilePlanPreferenceController(context, this);
        final WifiMasterSwitchPreferenceController wifiPreferenceController =
            new WifiMasterSwitchPreferenceController(context, mMetricsFeatureProvider);
        final Lifecycle lifecycle = getLifecycle();
        lifecycle.addObserver(airplaneModePreferenceController);
        lifecycle.addObserver(mobilePlanPreferenceController);
        lifecycle.addObserver(wifiPreferenceController);

        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(airplaneModePreferenceController);
        controllers.add(new MobileNetworkPreferenceController(context));
        controllers.add(new TetherPreferenceController(context));
        controllers.add(new VpnPreferenceController(context));
        controllers.add(new WifiCallingPreferenceController(context));
        controllers.add(new NetworkResetPreferenceController(context));
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

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_and_internet;
                    return Arrays.asList(sir);
                }
            };
}
