/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.dream.DreamBackend;

/**
 * Controller for the {@link androidx.preference.SwitchPreference} which controls if dream
 * overlays should be enabled.
 */
public class DreamComplicationPreferenceController extends TogglePreferenceController {
    private final DreamBackend mBackend;

    public DreamComplicationPreferenceController(Context context, String key) {
        super(context, key);
        mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mBackend.getSupportedComplications().isEmpty() ? CONDITIONALLY_UNAVAILABLE
                : AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mBackend.getComplicationsEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setComplicationsEnabled(isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
