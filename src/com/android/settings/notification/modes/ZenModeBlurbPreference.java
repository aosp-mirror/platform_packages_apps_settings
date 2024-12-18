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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.TopIntroPreference;

public class ZenModeBlurbPreference extends TopIntroPreference {

    public ZenModeBlurbPreference(Context context) {
        super(context);
    }

    public ZenModeBlurbPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (holder.findViewById(android.R.id.title) instanceof TextView textView) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getContext().getResources().getDimensionPixelSize(
                            R.dimen.zen_mode_blurb_text_size));

            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            ViewGroup.LayoutParams layoutParams = textView.getLayoutParams();
            if (layoutParams.width != MATCH_PARENT) {
                layoutParams.width = MATCH_PARENT;
                textView.setLayoutParams(layoutParams);
            }
        }
    }
}
