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

import static com.android.settings.slices.SettingsSliceProvider.ACTION_TOGGLE_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.ACTION_WIFI_CHANGED;
import static com.android.settings.slices.SettingsSliceProvider.EXTRA_SLICE_KEY;

import android.app.slice.Slice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.TogglePreferenceController;

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
        String action = intent.getAction();
        String key = intent.getStringExtra(EXTRA_SLICE_KEY);

        switch (action) {
            case ACTION_TOGGLE_CHANGED:
                handleToggleAction(context, key);
                break;
            case ACTION_WIFI_CHANGED:
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                boolean newState = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE,
                        wm.isWifiEnabled());
                wm.setWifiEnabled(newState);
                // Wait a bit for wifi to update (TODO: is there a better way to do this?)
                Handler h = new Handler();
                h.postDelayed(() -> {
                    Uri uri = SettingsSliceProvider.getUri(SettingsSliceProvider.PATH_WIFI);
                    context.getContentResolver().notifyChange(uri, null);
                }, 1000);
                break;
        }
    }

    private void handleToggleAction(Context context, String key) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalStateException("No key passed to Intent for toggle controller");
        }

        BasePreferenceController controller = getBasePreferenceController(context, key);

        if (!(controller instanceof TogglePreferenceController)) {
            throw new IllegalStateException("Toggle action passed for a non-toggle key: " + key);
        }

        // TODO post context.getContentResolver().notifyChanged(uri, null) in the Toggle controller
        // so that it's automatically broadcast to any slice.
        TogglePreferenceController toggleController = (TogglePreferenceController) controller;
        boolean currentValue = toggleController.isChecked();
        toggleController.setChecked(!currentValue);
    }

    private BasePreferenceController getBasePreferenceController(Context context, String key) {
        final SlicesDatabaseAccessor accessor = new SlicesDatabaseAccessor(context);
        final SliceData sliceData = accessor.getSliceDataFromKey(key);
        return SliceBuilderUtils.getPreferenceController(context, sliceData);
    }
}
