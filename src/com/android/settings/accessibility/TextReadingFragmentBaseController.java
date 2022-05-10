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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.TextReadingPreferenceFragment.EXTRA_LAUNCHED_FROM;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint;
import com.android.settings.core.BasePreferenceController;

/**
 * The base controller for the fragment{@link TextReadingPreferenceFragment}.
 */
public class TextReadingFragmentBaseController extends BasePreferenceController {
    @EntryPoint
    private int mEntryPoint;

    private TextReadingFragmentBaseController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    TextReadingFragmentBaseController(Context context, String preferenceKey,
            @EntryPoint int entryPoint) {
        this(context, preferenceKey);
        mEntryPoint = entryPoint;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            final Bundle extras = preference.getExtras();
            extras.putInt(EXTRA_LAUNCHED_FROM, mEntryPoint);
        }

        return super.handlePreferenceTreeClick(preference);
    }
}
