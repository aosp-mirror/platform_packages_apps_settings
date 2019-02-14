/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

/**
 * A preference with a plus button on the side representing an "add" action. The plus button will
 * only be visible when a non-null click listener is registered.
 */
public class AddPreference extends RestrictedPreference implements View.OnClickListener {

    private OnAddClickListener mListener;
    private View mWidgetFrame;
    private View mAddWidget;

    public AddPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @VisibleForTesting
    int getAddWidgetResId() {
        return R.id.add_preference_widget;
    }

    /** Sets a listener for clicks on the plus button. Passing null will cause the button to be
     * hidden. */
    public void setOnAddClickListener(OnAddClickListener listener) {
        mListener = listener;
       if (mWidgetFrame != null) {
           mWidgetFrame.setVisibility(shouldHideSecondTarget() ? View.GONE : View.VISIBLE);
       }
    }

    public void setAddWidgetEnabled(boolean enabled) {
        if (mAddWidget != null) {
            mAddWidget.setEnabled(enabled);
        }
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_add;
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return mListener == null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mWidgetFrame = holder.findViewById(android.R.id.widget_frame);
        mAddWidget = holder.findViewById(getAddWidgetResId());
        mAddWidget.setEnabled(true);
        mAddWidget.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == getAddWidgetResId() && mListener != null) {
            mListener.onAddClick(this);
        }
    }

    public interface OnAddClickListener {
        void onAddClick(AddPreference p);
    }
}
