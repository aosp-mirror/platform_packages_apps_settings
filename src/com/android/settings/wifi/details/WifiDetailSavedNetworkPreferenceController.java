/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.os.Handler;

import androidx.fragment.app.Fragment;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.wifi.AccessPoint;

public class WifiDetailSavedNetworkPreferenceController extends WifiDetailPreferenceController {

    WifiDetailSavedNetworkPreferenceController(AccessPoint accessPoint,
            ConnectivityManager connectivityManager, Context context,
            Fragment fragment, Handler handler,
            Lifecycle lifecycle,
            WifiManager wifiManager,
            MetricsFeatureProvider metricsFeatureProvider,
            IconInjector injector) {
        super(accessPoint, connectivityManager, context, fragment, handler, lifecycle, wifiManager,
                metricsFeatureProvider, injector);
    }

    public static WifiDetailSavedNetworkPreferenceController newInstance(
            AccessPoint accessPoint,
            ConnectivityManager connectivityManager,
            Context context,
            Fragment fragment,
            Handler handler,
            Lifecycle lifecycle,
            WifiManager wifiManager,
            MetricsFeatureProvider metricsFeatureProvider) {
        return new WifiDetailSavedNetworkPreferenceController(
                accessPoint, connectivityManager, context, fragment, handler, lifecycle,
                wifiManager, metricsFeatureProvider, new IconInjector(context));
    }

    @Override
    public void onPause() {
        // Do nothing
    }

    @Override
    public void onResume() {
        updateSavedNetworkInfo();
    }
}
