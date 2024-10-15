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

package com.android.settings.gestures;

import static android.app.contextualsearch.ContextualSearchManager.FEATURE_CONTEXTUAL_SEARCH;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.settings.core.TogglePreferenceController;

/**
 * Configures behaviour of Contextual Search setting.
 */
public class NavigationSettingsContextualSearchController extends TogglePreferenceController {

    public NavigationSettingsContextualSearchController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        boolean onByDefault = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_searchAllEntrypointsEnabledDefault);
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, onByDefault ? 1 : 0)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEARCH_ALL_ENTRYPOINTS_ENABLED, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getPackageManager().hasSystemFeature(FEATURE_CONTEXTUAL_SEARCH)) {
            return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }
}
