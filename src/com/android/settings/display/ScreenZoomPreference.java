/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.display;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.settingslib.display.DisplayDensityUtils;

/**
 * Preference for changing the density of the display on which the preference
 * is visible.
 */
public class ScreenZoomPreference extends Preference {
    public ScreenZoomPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context,
                android.support.v7.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));

        if (TextUtils.isEmpty(getFragment())) {
            setFragment("com.android.settings.display.ScreenZoomSettings");
        }

        final DisplayDensityUtils density = new DisplayDensityUtils(context);
        final int defaultIndex = density.getCurrentIndex();
        if (defaultIndex < 0) {
            setVisible(false);
            setEnabled(false);
        } else if (TextUtils.isEmpty(getSummary())) {
            final String[] entries = density.getEntries();
            final int currentIndex = density.getCurrentIndex();
            setSummary(entries[currentIndex]);
        }
    }
}
