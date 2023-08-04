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
 * limitations under the License
 */

package com.android.settings.slices;

import static com.android.settings.bluetooth.BluetoothSliceBuilder.ACTION_BLUETOOTH_SLICE_CHANGED;
import static com.android.settings.network.telephony.Enhanced4gLteSliceHelper.ACTION_ENHANCED_4G_LTE_CHANGED;
import static com.android.settings.notification.zen.ZenModeSliceBuilder.ACTION_ZEN_MODE_SLICE_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.ACTION_SLIDER_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.ACTION_TOGGLE_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.EXTRA_SLICE_KEY;
import static com.android.settings.wifi.calling.WifiCallingSliceHelper.ACTION_WIFI_CALLING_CHANGED;
import static com.android.settings.wifi.calling.WifiCallingSliceHelper.ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED;
import static com.android.settings.wifi.calling.WifiCallingSliceHelper.ACTION_WIFI_CALLING_PREFERENCE_WIFI_ONLY;
import static com.android.settings.wifi.calling.WifiCallingSliceHelper.ACTION_WIFI_CALLING_PREFERENCE_WIFI_PREFERRED;

import android.app.settings.SettingsEnums;
import android.app.slice.Slice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.notification.zen.ZenModeSliceBuilder;
import com.android.settings.overlay.FeatureFactory;

/**
 * Responds to actions performed on slices and notifies slices of updates in state changes.
 */
public class SliceBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = "SettSliceBroadcastRec";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String key = intent.getStringExtra(EXTRA_SLICE_KEY);

        if (CustomSliceRegistry.isValidAction(action)) {
            final CustomSliceable sliceable =
                    CustomSliceable.createInstance(context,
                            CustomSliceRegistry.getSliceClassByUri(Uri.parse(action)));
            sliceable.onNotifyChange(intent);
            return;
        }
        final Uri sliceUri = intent.getData();

        switch (action) {
            case ACTION_TOGGLE_CHANGED:
                final boolean isChecked = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false);
                handleToggleAction(context, sliceUri, key, isChecked);
                break;
            case ACTION_SLIDER_CHANGED:
                final int newPosition = intent.getIntExtra(Slice.EXTRA_RANGE_VALUE, -1);
                handleSliderAction(context, sliceUri, key, newPosition);
                break;
            case ACTION_BLUETOOTH_SLICE_CHANGED:
                BluetoothSliceBuilder.handleUriChange(context, intent);
                break;
            case ACTION_WIFI_CALLING_CHANGED:
                FeatureFactory.getFeatureFactory()
                        .getSlicesFeatureProvider()
                        .getNewWifiCallingSliceHelper(context)
                        .handleWifiCallingChanged(intent);
                break;
            case ACTION_ZEN_MODE_SLICE_CHANGED:
                ZenModeSliceBuilder.handleUriChange(context, intent);
                break;
            case ACTION_ENHANCED_4G_LTE_CHANGED:
                FeatureFactory.getFeatureFactory()
                        .getSlicesFeatureProvider()
                        .getNewEnhanced4gLteSliceHelper(context)
                        .handleEnhanced4gLteChanged(intent);
                break;
            case ACTION_WIFI_CALLING_PREFERENCE_WIFI_ONLY:
            case ACTION_WIFI_CALLING_PREFERENCE_WIFI_PREFERRED:
            case ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED:
                FeatureFactory.getFeatureFactory()
                        .getSlicesFeatureProvider()
                        .getNewWifiCallingSliceHelper(context)
                        .handleWifiCallingPreferenceChanged(intent);
                break;
        }
    }

    private void handleToggleAction(Context context, Uri sliceUri, String key, boolean isChecked) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalStateException("No key passed to Intent for toggle controller");
        }

        final BasePreferenceController controller = getPreferenceController(context, key);

        if (!(controller instanceof TogglePreferenceController)) {
            throw new IllegalStateException("Toggle action passed for a non-toggle key: " + key);
        }

        if (!controller.isAvailable()) {
            Log.w(TAG, "Can't update " + key + " since the setting is unavailable");
            if (!controller.hasAsyncUpdate()) {
                context.getContentResolver().notifyChange(sliceUri, null /* observer */);
            }
            return;
        }

        // TODO post context.getContentResolver().notifyChanged(uri, null) in the Toggle controller
        // so that it's automatically broadcast to any slice.
        final TogglePreferenceController toggleController = (TogglePreferenceController) controller;
        toggleController.setChecked(isChecked);
        logSliceValueChange(context, key, isChecked ? 1 : 0);
        if (!controller.hasAsyncUpdate()) {
            context.getContentResolver().notifyChange(sliceUri, null /* observer */);
        }
    }

    private void handleSliderAction(Context context, Uri sliceUri, String key, int newPosition) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException(
                    "No key passed to Intent for slider controller. Use extra: " + EXTRA_SLICE_KEY);
        }

        if (newPosition == -1) {
            throw new IllegalArgumentException("Invalid position passed to Slider controller");
        }

        final BasePreferenceController controller = getPreferenceController(context, key);

        if (!(controller instanceof SliderPreferenceController)) {
            throw new IllegalArgumentException("Slider action passed for a non-slider key: " + key);
        }

        if (!controller.isAvailable()) {
            Log.w(TAG, "Can't update " + key + " since the setting is unavailable");
            context.getContentResolver().notifyChange(sliceUri, null /* observer */);
            return;
        }

        final SliderPreferenceController sliderController = (SliderPreferenceController) controller;
        final int minValue = sliderController.getMin();
        final int maxValue = sliderController.getMax();
        if (newPosition < minValue || newPosition > maxValue) {
            throw new IllegalArgumentException(
                    "Invalid position passed to Slider controller. Expected between " + minValue
                            + " and " + maxValue + " but found " + newPosition);
        }

        sliderController.setSliderPosition(newPosition);
        logSliceValueChange(context, key, newPosition);
        context.getContentResolver().notifyChange(sliceUri, null /* observer */);
    }

    /**
     * Log Slice value update events into MetricsFeatureProvider. The logging schema generally
     * follows the pattern in SharedPreferenceLogger.
     */
    private void logSliceValueChange(Context context, String sliceKey, int newValue) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                .action(SettingsEnums.PAGE_UNKNOWN,
                        SettingsEnums.ACTION_SETTINGS_SLICE_CHANGED,
                        SettingsEnums.PAGE_UNKNOWN,
                        sliceKey, newValue);
    }

    private BasePreferenceController getPreferenceController(Context context, String key) {
        final SlicesDatabaseAccessor accessor = new SlicesDatabaseAccessor(context);
        final SliceData sliceData = accessor.getSliceDataFromKey(key);
        return SliceBuilderUtils.getPreferenceController(context, sliceData);
    }
}
