/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.intentpicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.TwoTargetPreference;

/** This customized VerifiedLinksPreference was belonged to Open by default page */
public class VerifiedLinksPreference extends TwoTargetPreference {
    private Context mContext;
    private View.OnClickListener mOnWidgetClickListener;
    private boolean mShowCheckBox;

    public VerifiedLinksPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public VerifiedLinksPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, /* defStyleRes= */0);
    }

    public VerifiedLinksPreference(Context context, AttributeSet attrs) {
        this(context, attrs, /* defStyleAttr= */ 0);
    }

    public VerifiedLinksPreference(Context context) {
        this(context, /* attrs= */ null);
    }

    private void init(Context context) {
        mContext = context;
        mOnWidgetClickListener = null;
        mShowCheckBox = true;
        setLayoutResource(R.layout.preference_checkable_two_target);
        setWidgetLayoutResource(R.layout.verified_links_widget);
    }

    /**
     * Register a callback to be invoked when this widget is clicked.
     *
     * @param listener The callback that will run.
     */
    public void setWidgetFrameClickListener(View.OnClickListener listener) {
        mOnWidgetClickListener = listener;
    }

    /** Determine the visibility of the {@link CheckBox}. */
    public void setCheckBoxVisible(boolean isVisible) {
        mShowCheckBox = isVisible;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final View settingsWidget = view.findViewById(android.R.id.widget_frame);
        final View divider = view.findViewById(R.id.two_target_divider);
        divider.setVisibility(View.VISIBLE);
        settingsWidget.setVisibility(View.VISIBLE);
        if (mOnWidgetClickListener != null) {
            settingsWidget.setOnClickListener(mOnWidgetClickListener);
        }
        final View checkboxContainer = view.findViewById(R.id.checkbox_container);
        final View parentView = (View) checkboxContainer.getParent();
        parentView.setEnabled(false);
        parentView.setClickable(false);
        CheckBox checkBox = (CheckBox) view.findViewById(com.android.internal.R.id.checkbox);
        if (checkBox != null) {
            checkBox.setChecked(true);
            checkboxContainer.setVisibility(mShowCheckBox ? View.VISIBLE : View.GONE);
        }
    }
}
