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

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;


/**
 * Represents the preference controller for (un)hiding Private Space entry point in All Apps and
 * shows On/Off summary depending upon the settings provider value.
 */
public final class HidePrivateSpaceSummaryController extends BasePreferenceController {
    public HidePrivateSpaceSummaryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return android.os.Flags.allowPrivateProfile() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    @Override
    public CharSequence getSummary() {
        return isHidden() ? mContext.getString(R.string.privatespace_hide_on_summary)
                : mContext.getString(R.string.privatespace_hide_off_summary);
    }

    private boolean isHidden() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                HIDE_PRIVATESPACE_ENTRY_POINT, 0) == 1;
    }
}
