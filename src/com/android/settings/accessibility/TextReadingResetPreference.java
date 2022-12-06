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

import androidx.annotation.IntDef;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The preference which is used for resetting the status of all preferences in the display size
 * and text page.
 */
public class TextReadingResetPreference extends Preference {
    private View.OnClickListener mOnResetClickListener;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ButtonStyle.DEFAULT,
            ButtonStyle.SUW,
    })
    @interface ButtonStyle {
        int DEFAULT = 0;
        int SUW = 1;
    }

    public TextReadingResetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setSetupWizardStyle(ButtonStyle.DEFAULT);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final View view = holder.findViewById(R.id.reset_button);
        view.setOnClickListener(mOnResetClickListener);
    }

    void setSetupWizardStyle(@ButtonStyle int style) {
        final int layoutResId = (style == ButtonStyle.SUW)
                ? R.layout.accessibility_text_reading_reset_button_suw
                : R.layout.accessibility_text_reading_reset_button;
        setLayoutResource(layoutResId);
    }

    void setOnResetClickListener(View.OnClickListener resetClickListener) {
        mOnResetClickListener = resetClickListener;
    }
}
