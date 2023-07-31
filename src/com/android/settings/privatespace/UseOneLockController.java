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

import android.content.Context;

import com.android.settings.core.TogglePreferenceController;

/** Represents the preference controller for using the same lock as the screen lock */
public class UseOneLockController extends TogglePreferenceController {
    public UseOneLockController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        // TODO(b/293569406) Need to save this to a persistent store, maybe like SettingsProvider
        return false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        // TODO(b/293569406) Need to save this to a persistent store, maybe like SettingsProvider
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
