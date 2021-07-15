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

package com.android.settings.bluetooth;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * A custom preference for companion device apps. Added a button for association removal
 */
public class CompanionAppWidgetPreference extends Preference {
    private Drawable mWidgetIcon;
    private View.OnClickListener mWidgetListener;
    private int mImageButtonPadding;

    public CompanionAppWidgetPreference(Drawable widgetIcon, View.OnClickListener widgetListener,
            Context context) {
        super(context);
        mWidgetIcon = widgetIcon;
        mWidgetListener = widgetListener;
        mImageButtonPadding = context.getResources().getDimensionPixelSize(
                R.dimen.bluetooth_companion_app_widget);
        setWidgetLayoutResource(R.layout.companion_apps_remove_button_widget);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageButton imageButton = (ImageButton) holder.findViewById(R.id.remove_button);
        imageButton.setPadding(
                mImageButtonPadding, mImageButtonPadding, mImageButtonPadding, mImageButtonPadding);
        imageButton.setColorFilter(getContext().getColor(android.R.color.darker_gray));
        imageButton.setImageDrawable(mWidgetIcon);
        imageButton.setOnClickListener(mWidgetListener);
    }

}
