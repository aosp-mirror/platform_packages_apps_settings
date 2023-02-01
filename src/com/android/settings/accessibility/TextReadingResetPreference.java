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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * The preference which is used for resetting the status of all preferences in the display size
 * and text page.
 */
public class TextReadingResetPreference extends Preference {
    private View.OnClickListener mOnResetClickListener;

    public TextReadingResetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayoutResource(R.layout.accessibility_text_reading_reset_button);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final View view = holder.findViewById(R.id.reset_button);
        view.setOnClickListener(mOnResetClickListener);
    }

    void setOnResetClickListener(View.OnClickListener resetClickListener) {
        mOnResetClickListener = resetClickListener;
    }
}
