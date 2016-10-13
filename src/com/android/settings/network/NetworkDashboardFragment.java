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
import android.util.Log;

import com.android.internal.logging.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.List;

import static com.android.settings.network.MobilePlanPreferenceController
        .MANAGE_MOBILE_PLAN_DIALOG_ID;

public class NetworkDashboardFragment extends DashboardFragment implements
        MobilePlanPreferenceController.MobilePlanPreferenceHost {

    private static final String TAG = "NetworkDashboardFrag";

    @Override
    public int getMetricsCategory() {
        return NETWORK_CATEGORY_FRAGMENT;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_NETWORK;
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
        getLifecycle().addObserver(airplaneModePreferenceController);
        getLifecycle().addObserver(mobilePlanPreferenceController);

        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(airplaneModePreferenceController);
        controllers.add(new MobileNetworkPreferenceController(context));
        controllers.add(new TetherPreferenceController(context));
        controllers.add(new VpnPreferenceController(context));
        controllers.add(new WifiCallingPreferenceController(context));
        controllers.add(new NetworkResetPreferenceController(context));
        controllers.add(new ProxyPreferenceController(context));
        controllers.add(mobilePlanPreferenceController);
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
}
