/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.virtual;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Dedicated screen displaying the information for a single virtual device to the user and allowing
 * them to manage that device.
 */
public class VirtualDeviceDetailsFragment extends DashboardFragment {

    private static final String TAG = VirtualDeviceDetailsFragment.class.getSimpleName();

    static final String DEVICE_ARG = "virtual_device_arg";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        VirtualDeviceWrapper device =
                getArguments().getParcelable(DEVICE_ARG, VirtualDeviceWrapper.class);

        use(VirtualDeviceDetailsHeaderController.class).init(device);
        use(VirtualDeviceDetailsButtonsController.class).init(this, device);
        use(VirtualDeviceDetailsCompanionAppController.class)
                .init(this, device.getAssociationInfo().getPackageName());
        use(VirtualDeviceDetailsFooterController.class).init(device.getDeviceName(context));
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.VIRTUAL_DEVICE_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.virtual_device_details_fragment;
    }
}
