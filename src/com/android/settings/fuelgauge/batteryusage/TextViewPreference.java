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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.widget.GroupSectionDividerMixin;

/** A preference for a single text view. */
public class TextViewPreference extends Preference implements GroupSectionDividerMixin {
    private static final String TAG = "TextViewPreference";

    @VisibleForTesting CharSequence mText;

    public TextViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_text_view);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        final TextView textView = (TextView) view.findViewById(R.id.text);
        textView.setText(mText);
    }

    void setText(CharSequence text) {
        mText = text;
        notifyChanged();
    }
}
