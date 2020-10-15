/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/** PreferenceController for displaying all text in bold. */
public class ForceBoldTextPreferenceController extends TogglePreferenceController {

    public ForceBoldTextPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_BOLD_TEXT, Configuration.FORCE_BOLD_TEXT_NO)
                == Configuration.FORCE_BOLD_TEXT_YES;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FORCE_BOLD_TEXT,
                (isChecked ? Configuration.FORCE_BOLD_TEXT_YES : Configuration.FORCE_BOLD_TEXT_NO));
    }
}
