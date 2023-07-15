/*
 * Copyright (C) 2023 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * A preference controller for the "Stay unlocked on fold" setting.
 *
 * This preference controller allows users to control whether or not the device
 * stays awake when it is folded. When this setting is enabled, the device will
 * stay awake even if the device is folded.
 *
 * @link android.provider.Settings.System#STAY_AWAKE_ON_FOLD
 */
public class StayAwakeOnFoldPreferenceController extends TogglePreferenceController {

    private final Resources mResources;

    public StayAwakeOnFoldPreferenceController(Context context, String key) {
        this(context, key, context.getResources());
    }

    public StayAwakeOnFoldPreferenceController(Context context, String key, Resources resources) {
        super(context, key);
        mResources = resources;
    }

    @Override
    public int getAvailabilityStatus() {
        return mResources.getBoolean(R.bool.config_stay_awake_on_fold) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.STAY_AWAKE_ON_FOLD,
                0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final int stayUnlockedOnFold = isChecked ? 1 : 0;

        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STAY_AWAKE_ON_FOLD, stayUnlockedOnFold);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

}
