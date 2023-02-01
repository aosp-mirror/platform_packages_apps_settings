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
import android.view.accessibility.AccessibilityManager;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

/**
 * Preference category that supports requesting accessibility focus.
 */
public class AccessibilityFocusablePreferenceCategory extends PreferenceCategory {
    private PreferenceViewHolder mView;

    public AccessibilityFocusablePreferenceCategory(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AccessibilityFocusablePreferenceCategory(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AccessibilityFocusablePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibilityFocusablePreferenceCategory(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mView = view;
    }

    /**
     * Call this to try to give accessibility focus to the category title.
     */
    public void requestAccessibilityFocus() {
        if (mView == null || mView.itemView == null) {
            return;
        }
        if (!AccessibilityManager.getInstance(getContext()).isEnabled()) {
            return;
        }
        mView.itemView.requestAccessibilityFocus();
    }
}
