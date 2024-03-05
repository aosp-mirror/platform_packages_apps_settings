/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class PointerFillStylePreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final String KEY_POINTER_FILL_STYLE = "pointer_fill_style";

    public PointerFillStylePreferenceController(@NonNull Context context) {
        super(context, KEY_POINTER_FILL_STYLE);
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return android.view.flags.Flags.enableVectorCursorA11ySettings() ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference pointerFillStylePreference = screen.findPreference(KEY_POINTER_FILL_STYLE);
        if (pointerFillStylePreference == null) {
            return;
        }
        pointerFillStylePreference.setPreferenceDataStore(new PreferenceDataStore() {
            @Override
            public void putInt(@NonNull String key, int value) {
                Settings.System.putIntForUser(mContext.getContentResolver(), key, value,
                        UserHandle.USER_CURRENT);
            }

            @Override
            public int getInt(@NonNull String key, int defValue) {
                return Settings.System.getIntForUser(mContext.getContentResolver(), key, defValue,
                        UserHandle.USER_CURRENT);
            }
        });
    }
}
