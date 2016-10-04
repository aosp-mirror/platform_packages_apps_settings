/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.core;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

/**
 * A controller that manages event for preference.
 */
public abstract class PreferenceController {

    protected Context mContext;

    public PreferenceController(Context context) {
        mContext = context;
    }

    /**
     * Displays preference in this controller.
     */
    public abstract void displayPreference(PreferenceScreen screen);

    /**
     * Handles preference tree click
     *
     * @param preference the preference being clicked
     * @return true if click is handled
     */
    public abstract boolean handlePreferenceTreeClick(Preference preference);


    /**
     * Removes preference from screen.
     */
    protected final void removePreference(PreferenceScreen screen, String key) {
        Preference pref = screen.findPreference(key);
        if (pref != null) {
            screen.removePreference(pref);
        }
    }

}
