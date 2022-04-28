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

package com.android.settings.display;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.Lifecycle;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.devicestate.DeviceStateRotationLockSettingsManager;
import com.android.settingslib.devicestate.DeviceStateRotationLockSettingsManager.SettableDeviceState;
import com.android.settingslib.search.SearchIndexableRaw;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class with utility methods related to device state auto-rotation that can be used in
 * auto-rotation settings fragments and controllers.
 */
public class DeviceStateAutoRotationHelper {

    private static final String TAG = "DeviceStateAutoRotHelpr";

    static void initControllers(Lifecycle lifecycle,
            List<DeviceStateAutoRotateSettingController> controllers) {
        for (DeviceStateAutoRotateSettingController controller : controllers) {
            controller.init(lifecycle);
        }
    }

    static ImmutableList<AbstractPreferenceController> createPreferenceControllers(
            Context context) {
        List<SettableDeviceState> settableDeviceStates = DeviceStateRotationLockSettingsManager
                .getInstance(context).getSettableDeviceStates();
        int numDeviceStates = settableDeviceStates.size();
        if (numDeviceStates == 0) {
            return ImmutableList.of();
        }
        String[] deviceStateSettingDescriptions = context.getResources().getStringArray(
                R.array.config_settableAutoRotationDeviceStatesDescriptions);
        if (numDeviceStates != deviceStateSettingDescriptions.length) {
            Log.wtf(TAG,
                    "Mismatch between number of device states and device states descriptions.");
            return ImmutableList.of();
        }

        ImmutableList.Builder<AbstractPreferenceController> controllers =
                ImmutableList.builderWithExpectedSize(numDeviceStates);
        for (int i = 0; i < numDeviceStates; i++) {
            SettableDeviceState settableDeviceState = settableDeviceStates.get(i);
            if (!settableDeviceState.isSettable()) {
                continue;
            }
            // Preferences with a lower order will be showed first. Here we go below 0 to make sure
            // we are shown before statically declared preferences in XML.
            int order = -numDeviceStates + i;
            controllers.add(new DeviceStateAutoRotateSettingController(
                    context,
                    settableDeviceState.getDeviceState(),
                    deviceStateSettingDescriptions[i],
                    order
            ));
        }
        return controllers.build();
    }

    static List<SearchIndexableRaw> getRawDataToIndex(
            Context context, boolean enabled) {
        // Check what the "enabled" param is for
        List<AbstractPreferenceController> controllers = createPreferenceControllers(context);
        List<SearchIndexableRaw> rawData = new ArrayList<>();
        for (AbstractPreferenceController controller : controllers) {
            ((BasePreferenceController) controller).updateRawDataToIndex(rawData);
        }
        return rawData;
    }

    /** Returns whether the device state based auto-rotation settings are enabled. */
    public static boolean isDeviceStateRotationEnabled(Context context) {
        return RotationPolicy.isRotationLockToggleVisible(context)
                && DeviceStateRotationLockSettingsManager.isDeviceStateRotationLockEnabled(context);
    }

    /**
     * Returns whether the device state based auto-rotation settings are enabled for the
     * accessibility settings page.
     */
    public static boolean isDeviceStateRotationEnabledForA11y(Context context) {
        return RotationPolicy.isRotationSupported(context)
                && DeviceStateRotationLockSettingsManager.isDeviceStateRotationLockEnabled(context);
    }
}
