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
 * limitations under the License
 */

package com.android.settings.widget;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.RadioButtonPreference;

public class RadioButtonPreferenceWithExtraWidget extends RadioButtonPreference {
    public static final int EXTRA_WIDGET_VISIBILITY_GONE = 0;
    public static final int EXTRA_WIDGET_VISIBILITY_INFO = 1;
    public static final int EXTRA_WIDGET_VISIBILITY_SETTING = 2;

    private View mExtraWidgetDivider;
    private ImageView mExtraWidget;

    private int mExtraWidgetVisibility = EXTRA_WIDGET_VISIBILITY_GONE;
    private View.OnClickListener mExtraWidgetOnClickListener;

    public RadioButtonPreferenceWithExtraWidget(Context context) {
        super(context, null);
        setLayoutResource(R.layout.preference_radio_with_extra_widget);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        mExtraWidget = (ImageView) view.findViewById(R.id.radio_extra_widget);
        mExtraWidgetDivider = view.findViewById(R.id.radio_extra_widget_divider);
        setExtraWidgetVisibility(mExtraWidgetVisibility);

        if (mExtraWidgetOnClickListener != null) {
            setExtraWidgetOnClickListener(mExtraWidgetOnClickListener);
        }
    }

    public void setExtraWidgetVisibility(int visibility) {
        mExtraWidgetVisibility = visibility;
        if (mExtraWidget == null || mExtraWidgetDivider == null) {
            return;
        }

        if (visibility == EXTRA_WIDGET_VISIBILITY_GONE) {
            mExtraWidget.setClickable(false);
            mExtraWidget.setVisibility(View.GONE);
            mExtraWidgetDivider.setVisibility(View.GONE);
        } else {
            mExtraWidget.setClickable(true);
            mExtraWidget.setVisibility(View.VISIBLE);
            mExtraWidgetDivider.setVisibility(View.VISIBLE);
            if (mExtraWidgetVisibility == EXTRA_WIDGET_VISIBILITY_INFO) {
                mExtraWidget.setImageResource(R.drawable.ic_settings_about);
                mExtraWidget.setContentDescription(
                        getContext().getResources().getText(R.string.information_label));
            } else if (mExtraWidgetVisibility == EXTRA_WIDGET_VISIBILITY_SETTING) {
                mExtraWidget.setImageResource(R.drawable.ic_settings_accent);
                mExtraWidget.setContentDescription(
                        getContext().getResources().getText(R.string.settings_label));
            }
        }
    }

    public void setExtraWidgetOnClickListener(View.OnClickListener listener) {
        mExtraWidgetOnClickListener = listener;
        if (mExtraWidget != null) {
            mExtraWidget.setEnabled(true);
            mExtraWidget.setOnClickListener(listener);
        }
    }
}