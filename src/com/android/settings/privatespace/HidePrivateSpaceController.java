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

package com.android.settings.privatespace;

import static android.provider.Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 *  A class that is used to show details page for the setting to hide private space entry point
 *  in All Apps.
 */
public class HidePrivateSpaceController extends TogglePreferenceController {
    private static final int DISABLED_VALUE = 0;
    private static final int ENABLED_VALUE = 1;

    public HidePrivateSpaceController(Context context, String key) {
        super(context, key);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                HIDE_PRIVATESPACE_ENTRY_POINT, DISABLED_VALUE) != DISABLED_VALUE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(), HIDE_PRIVATESPACE_ENTRY_POINT,
                isChecked ? ENABLED_VALUE : DISABLED_VALUE);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
