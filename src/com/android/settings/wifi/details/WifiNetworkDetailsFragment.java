/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.wifi.details;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.vpn2.ConnectivityManagerWrapperImpl;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detail page for the currently connected wifi network.
 *
 * <p>The AccessPoint should be saved to the intent Extras when launching this class via
 * {@link AccessPoint#saveWifiState(Bundle)} in order to properly render this page.
 */
public class WifiNetworkDetailsFragment extends DashboardFragment {

    private static final String TAG = "WifiNetworkDetailsFrg";

    private AccessPoint mAccessPoint;
    private WifiDetailPreferenceController mWifiDetailPreferenceController;
    private WifiDetailActionBarObserver mWifiDetailActionBarObserver;

    @Override
    public void onAttach(Context context) {
        mWifiDetailActionBarObserver = new WifiDetailActionBarObserver(context, this);
        getLifecycle().addObserver(mWifiDetailActionBarObserver);

        mAccessPoint = new AccessPoint(context, getArguments());
        super.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.WIFI_NETWORK_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_network_details_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        mWifiDetailPreferenceController = new WifiDetailPreferenceController(
                mAccessPoint,
                new ConnectivityManagerWrapperImpl(cm),
                context,
                this,
                new Handler(Looper.getMainLooper()),  // UI thread.
                getLifecycle(),
                context.getSystemService(WifiManager.class),
                mMetricsFeatureProvider);

        return new ArrayList<>(Collections.singletonList(mWifiDetailPreferenceController));
    }
}
