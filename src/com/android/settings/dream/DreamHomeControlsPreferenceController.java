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

package com.android.settings.dream;

import android.content.Context;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.dream.DreamBackend;

/**
 * Controller for the {@link androidx.preference.SwitchPreference} which controls if dream
 * overlays should be enabled.
 */
public class DreamHomeControlsPreferenceController extends TogglePreferenceController {
    private final DreamBackend mBackend;

    public DreamHomeControlsPreferenceController(Context context, String key) {
        this(context, key, DreamBackend.getInstance(context));
    }

    @VisibleForTesting
    public DreamHomeControlsPreferenceController(Context context, String key,
            DreamBackend dreamBackend) {
        super(context, key);
        mBackend = dreamBackend;
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean supported =
                mBackend.getSupportedComplications()
                        .contains(DreamBackend.COMPLICATION_TYPE_HOME_CONTROLS);
        return supported ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mBackend.getEnabledComplications().contains(
                DreamBackend.COMPLICATION_TYPE_HOME_CONTROLS);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setHomeControlsEnabled(isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
