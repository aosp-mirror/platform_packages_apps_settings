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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.GroupSectionDividerMixin;

/** A preference for battery header text. */
public class BatteryHeaderTextPreference extends Preference implements GroupSectionDividerMixin {
    private static final String TAG = "BatteryHeaderTextPreference";

    @Nullable private CharSequence mText;
    @Nullable private CharSequence mContentDescription;

    public BatteryHeaderTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_battery_header_text);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        final TextView textView = (TextView) view.findViewById(R.id.text);
        textView.setText(mText);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        if (!TextUtils.isEmpty(mContentDescription)) {
            textView.setContentDescription(mContentDescription);
        }
    }

    void setText(@Nullable CharSequence text) {
        mText = text;
        notifyChanged();
    }

    void setContentDescription(@Nullable CharSequence contentDescription) {
        mContentDescription = contentDescription;
        notifyChanged();
    }
}
