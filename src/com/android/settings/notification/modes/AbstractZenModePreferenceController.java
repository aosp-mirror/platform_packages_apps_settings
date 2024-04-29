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

import android.app.AutomaticZenRule;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Base class for any preference controllers pertaining to any single Zen mode.
 */
abstract class AbstractZenModePreferenceController extends AbstractPreferenceController {

    @Nullable
    protected ZenModesBackend mBackend;

    @Nullable  // only until updateZenMode() is called
    private ZenMode mZenMode;

    @NonNull
    final String mKey;

    // ZenModesBackend should only be passed in if the preference controller may set the user's
    // policy for this zen mode. Otherwise, if the preference controller is essentially read-only
    // and leads to a further Settings screen, backend should be null.
    AbstractZenModePreferenceController(@NonNull Context context, @NonNull String key,
            @Nullable ZenModesBackend backend) {
        super(context);
        mBackend = backend;
        mKey = key;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return mKey;
    }

    // Called by the parent Fragment onStart, which means it will happen before resume.
    public void updateZenMode(@NonNull Preference preference, @NonNull ZenMode zenMode) {
        mZenMode = zenMode;
        updateState(preference);
    }

    @Nullable
    public ZenMode getMode() {
        return mZenMode;
    }

    @Nullable
    public AutomaticZenRule getAZR() {
        if (mZenMode == null || mZenMode.getRule() == null) {
            return null;
        }
        return mZenMode.getRule();
    }

    /** Implementations of this class should override
     *  {@link AbstractPreferenceController#updateState(Preference)} to specify what should
     *  happen when the preference is updated */
}
