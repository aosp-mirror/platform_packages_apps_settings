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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.R;
import com.android.settings.SettingsDumpService;
import com.android.settings.core.OnActivityResultListener;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class NetworkDashboardFragment extends DashboardFragment implements
        OnActivityResultListener {

    private static final String TAG = "NetworkDashboardFrag";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_NETWORK_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_provider_internet;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        use(AirplaneModePreferenceController.class).setFragment(this);
        use(NetworkProviderCallsSmsController.class).init(this);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_network_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(),
                this /* LifecycleOwner */);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, LifecycleOwner lifecycleOwner) {
        final InternetPreferenceController internetPreferenceController =
                new InternetPreferenceController(context, lifecycle, lifecycleOwner);

        final VpnPreferenceController vpnPreferenceController =
                new VpnPreferenceController(context);
        final PrivateDnsPreferenceController privateDnsPreferenceController =
                new PrivateDnsPreferenceController(context);

        if (lifecycle != null) {
            lifecycle.addObserver(vpnPreferenceController);
            lifecycle.addObserver(privateDnsPreferenceController);
        }

        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        controllers.add(new MobileNetworkSummaryController(context, lifecycle, lifecycleOwner));
        controllers.add(vpnPreferenceController);
        if (internetPreferenceController != null) {
            controllers.add(internetPreferenceController);
        }
        controllers.add(privateDnsPreferenceController);

        // Start SettingsDumpService after the MobileNetworkRepository is created.
        Intent intent = new Intent(context, SettingsDumpService.class);
        intent.putExtra(SettingsDumpService.EXTRA_KEY_SHOW_NETWORK_DUMP, true);
        context.startService(intent);
        return controllers;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case AirplaneModePreferenceController.REQUEST_CODE_EXIT_ECM:
                use(AirplaneModePreferenceController.class)
                        .onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.network_provider_internet) {
                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* LifecycleOwner */);
                }
            };
}
