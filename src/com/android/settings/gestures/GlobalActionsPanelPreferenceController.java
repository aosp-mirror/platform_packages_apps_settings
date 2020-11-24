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
 * limitations under the License
 */

package com.android.settings.gestures;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.provider.Settings;
import android.text.TextUtils;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;

import com.android.internal.annotations.VisibleForTesting;

public class GlobalActionsPanelPreferenceController extends GesturePreferenceController {
    private static final String PREF_KEY_VIDEO = "global_actions_panel_video";

    @VisibleForTesting
    protected static final String ENABLED_SETTING = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    @VisibleForTesting
    protected static final String AVAILABLE_SETTING =
            Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;

    @VisibleForTesting
    protected static final String TOGGLE_KEY = "gesture_global_actions_panel_switch";

    public GlobalActionsPanelPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return hasNFC(mContext) == true ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    // Check to see if device supports NFC
    public static boolean hasNFC(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), ENABLED_SETTING,
                isChecked ? 1 : 0);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), TOGGLE_KEY);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean isChecked() {
        int enabled = Settings.Secure.getInt(mContext.getContentResolver(), ENABLED_SETTING, 0);
        return enabled == 1;
    }
}
