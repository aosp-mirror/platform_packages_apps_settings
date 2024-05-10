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
package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.RestrictedSwitchPreference;

/**
 * This widget with enabled filterTouchesWhenObscured attribute use to replace
 * the {@link RestrictedSwitchPreference} in the Special access app pages for
 * security.
 */
public class FilterTouchesRestrictedSwitchPreference extends RestrictedSwitchPreference {
    public FilterTouchesRestrictedSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FilterTouchesRestrictedSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FilterTouchesRestrictedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FilterTouchesRestrictedSwitchPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View switchView = holder.findViewById(androidx.preference.R.id.switchWidget);
        if (switchView != null) {
            final View rootView = switchView.getRootView();
            rootView.setFilterTouchesWhenObscured(true);
        }
    }
}
