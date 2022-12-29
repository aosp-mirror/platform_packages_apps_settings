/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.connecteddevice.stylus;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/** Controls the USI stylus details and provides updates to individual controllers. */
public class StylusUsiDetailsFragment extends DashboardFragment {
    private static final String TAG = StylusUsiDetailsFragment.class.getSimpleName();
    private static final String KEY_DEVICE_INPUT_ID = "device_input_id";

    @VisibleForTesting
    @Nullable
    InputDevice mInputDevice;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        int inputDeviceId = getArguments().getInt(KEY_DEVICE_INPUT_ID);
        InputManager im = context.getSystemService(InputManager.class);
        mInputDevice = im.getInputDevice(inputDeviceId);

        super.onAttach(context);
        if (mInputDevice == null) {
            finish();
        }
    }


    @Override
    public int getMetricsCategory() {
        // TODO(b/261988317): for new SettingsEnum for this page
        return SettingsEnums.BLUETOOTH_DEVICE_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.stylus_usi_details_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();
        if (mInputDevice != null) {
            Lifecycle lifecycle = getSettingsLifecycle();
            controllers.add(new StylusUsiHeaderController(context, mInputDevice));
            controllers.add(new StylusDevicesController(context, mInputDevice, null, lifecycle));
        }
        return controllers;
    }
}
