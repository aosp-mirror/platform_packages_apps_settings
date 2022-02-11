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
import android.hardware.display.ColorDisplayManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** PreferenceController for persisting feature activation state after a restart. */
public class ReduceBrightColorsPersistencePreferenceController extends TogglePreferenceController {
    private final ColorDisplayManager mColorDisplayManager;

    public ReduceBrightColorsPersistencePreferenceController(
            Context context, String preferenceKey) {
        super(context, preferenceKey);
        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!ColorDisplayManager.isReduceBrightColorsAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!mColorDisplayManager.isReduceBrightColorsActivated()) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS, (isChecked ? 1 : 0));
    }

    @Override
    public final void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(mColorDisplayManager.isReduceBrightColorsActivated());
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
