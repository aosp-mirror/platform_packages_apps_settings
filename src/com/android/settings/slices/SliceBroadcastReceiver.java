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

import static com.android.settings.slices.SettingsSliceProvider.ACTION_SLIDER_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.ACTION_TOGGLE_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.ACTION_WIFI_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.EXTRA_SLICE_KEY;
import static com.android.settings.slices.SettingsSliceProvider.EXTRA_SLICE_PLATFORM_DEFINED;

import android.app.slice.Slice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.SettingsSlicesContract;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.core.TogglePreferenceController;

import androidx.slice.core.SliceHints;

/**
 * Responds to actions performed on slices and notifies slices of updates in state changes.
 */
public class SliceBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = "SettSliceBroadcastRec";

    /**
     * TODO (b/) move wifi action into generalized case.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String key = intent.getStringExtra(EXTRA_SLICE_KEY);
        final boolean isPlatformDefined = intent.getBooleanExtra(EXTRA_SLICE_PLATFORM_DEFINED,
                false /* default */);

        switch (action) {
            case ACTION_TOGGLE_CHANGED:
                handleToggleAction(context, key, isPlatformDefined);
                break;
            case ACTION_SLIDER_CHANGED:
                int newPosition = intent.getIntExtra(SliceHints.EXTRA_RANGE_VALUE, -1);
                handleSliderAction(context, key, newPosition);
                break;
            case ACTION_WIFI_CHANGED:
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                boolean newState = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE,
                        wm.isWifiEnabled());
                wm.setWifiEnabled(newState);
                // Wait a bit for wifi to update (TODO: is there a better way to do this?)
                Handler h = new Handler();
                h.postDelayed(() -> {
                    Uri uri = SliceBuilderUtils.getUri(SettingsSliceProvider.PATH_WIFI,
                            false /* isPlatformSlice */);
                    context.getContentResolver().notifyChange(uri, null);
                }, 1000);
                break;
        }
    }

    private void handleToggleAction(Context context, String key, boolean isPlatformSlice) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalStateException("No key passed to Intent for toggle controller");
        }

        final BasePreferenceController controller = getPreferenceController(context, key);

        if (!(controller instanceof TogglePreferenceController)) {
            throw new IllegalStateException("Toggle action passed for a non-toggle key: " + key);
        }

        if (!controller.isAvailable()) {
            Log.d(TAG, "Can't update " + key + " since the setting is unavailable");
            updateUri(context, key, isPlatformSlice);
        }

        // TODO post context.getContentResolver().notifyChanged(uri, null) in the Toggle controller
        // so that it's automatically broadcast to any slice.
        final TogglePreferenceController toggleController = (TogglePreferenceController) controller;
        final boolean currentValue = toggleController.isChecked();
        toggleController.setChecked(!currentValue);
        updateUri(context, key, isPlatformSlice);
    }

    private void handleSliderAction(Context context, String key, int newPosition) {
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

        final SliderPreferenceController sliderController = (SliderPreferenceController) controller;
        final int maxSteps = sliderController.getMaxSteps();
        if (newPosition < 0 || newPosition > maxSteps) {
            throw new IllegalArgumentException(
                    "Invalid position passed to Slider controller. Expected between 0 and "
                            + maxSteps + " but found " + newPosition);
        }

        sliderController.setSliderPosition(newPosition);
    }

    private BasePreferenceController getPreferenceController(Context context, String key) {
        final SlicesDatabaseAccessor accessor = new SlicesDatabaseAccessor(context);
        final SliceData sliceData = accessor.getSliceDataFromKey(key);
        return SliceBuilderUtils.getPreferenceController(context, sliceData);
    }

    private void updateUri(Context context, String key, boolean isPlatformDefined) {
        final String path = SettingsSlicesContract.PATH_SETTING_ACTION + "/" + key;
        final Uri uri = SliceBuilderUtils.getUri(path, isPlatformDefined);
        context.getContentResolver().notifyChange(uri, null /* observer */);
    }
}
