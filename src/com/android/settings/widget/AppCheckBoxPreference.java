/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * {@link CheckBoxPreference} that used only to display app
 */
public class AppCheckBoxPreference extends CheckBoxPreference {
    public AppCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app);
    }

    public AppCheckBoxPreference(Context context) {
        super(context);
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final LinearLayout layout = (LinearLayout) holder.findViewById(R.id.summary_container);
        if (layout != null) {
            // If summary doesn't exist, make it gone
            layout.setVisibility(TextUtils.isEmpty(getSummary()) ? View.GONE : View.VISIBLE);
        }
    }
}
