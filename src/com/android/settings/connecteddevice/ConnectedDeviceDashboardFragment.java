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
package com.android.settings.connecteddevice;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.nfc.NfcPreferenceController;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.List;

public class ConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "ConnectedDeviceFrag";

    @Override
    public int getMetricsCategory() {
        return CONNECTED_DEVICE_CATEGORY_FRAGMENT;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_DEVICE;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final NfcPreferenceController nfcPreferenceController =
                new NfcPreferenceController(context);
        getLifecycle().addObserver(nfcPreferenceController);
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(nfcPreferenceController);
        return controllers;
    }

}