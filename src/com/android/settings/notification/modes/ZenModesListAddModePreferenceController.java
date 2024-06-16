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

package com.android.settings.notification.modes;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Random;

class ZenModesListAddModePreferenceController extends AbstractPreferenceController {

    private final ZenModesBackend mBackend;
    private final ZenServiceListing mServiceListing;

    ZenModesListAddModePreferenceController(Context context, ZenModesBackend backend,
            ZenServiceListing serviceListing) {
        super(context);
        mBackend = backend;
        mServiceListing = serviceListing;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "add_mode";
    }

    @Override
    public void updateState(Preference preference) {
        preference.setOnPreferenceClickListener(pref -> {
            // TODO: b/326442408 - Launch the proper mode creation flow (using mServiceListing).
            ZenMode mode = mBackend.addCustomMode("New mode #" + new Random().nextInt(1000));
            if (mode != null) {
                ZenSubSettingLauncher.forMode(mContext, mode.getId()).launch();
            }
            return true;
        });
    }
}
