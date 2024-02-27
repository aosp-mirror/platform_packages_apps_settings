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

package com.android.settings.display;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.server.display.feature.flags.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for the settings toggle which allows screen brightness to go even dimmer than usual.
 *
 */
public class EvenDimmerPreferenceController extends TogglePreferenceController {

    private static final String TAG = "EvenDimmerPreferenceController";

    private final Resources mResources;

    public EvenDimmerPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mResources = context.getResources();
    }

    @Override
    public int getAvailabilityStatus() {
        // enable based on flag and config.xml
        final boolean enabledInConfig = mResources.getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        return (Flags.evenDimmer() && enabledInConfig) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return getEvenDimmerActivated();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final float enabled = getAvailabilityStatus() == AVAILABLE && isChecked ? 1 : 0;
        Log.i(TAG, "setChecked to : " + enabled);

        return Settings.Secure.putFloat(
                mContext.getContentResolver(), Settings.Secure.EVEN_DIMMER_ACTIVATED, enabled);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private boolean getEvenDimmerActivated() {
        return Settings.Secure.getFloat(mContext.getContentResolver(),
                Settings.Secure.EVEN_DIMMER_ACTIVATED, 0) == 1;
    }
}
