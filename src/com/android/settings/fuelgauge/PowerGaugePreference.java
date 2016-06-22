/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.PreferenceViewHolder;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.TintablePreference;
import com.android.settings.Utils;

/**
 * Custom preference for displaying power consumption as a bar and an icon on
 * the left for the subsystem/app type.
 */
public class PowerGaugePreference extends TintablePreference {
    private final int mIconSize;

    private BatteryEntry mInfo;
    private CharSequence mContentDescription;
    private CharSequence mProgress;

    public PowerGaugePreference(Context context, Drawable icon, CharSequence contentDescription,
            BatteryEntry info) {
        super(context, null);
        setIcon(icon != null ? icon : new ColorDrawable(0));
        setWidgetLayoutResource(R.layout.preference_widget_summary);
        mInfo = info;
        mContentDescription = contentDescription;
        mIconSize = context.getResources().getDimensionPixelSize(R.dimen.app_icon_size);
    }

    public void setContentDescription(String name) {
        mContentDescription = name;
        notifyChanged();
    }

    public void setPercent(double percentOfMax, double percentOfTotal) {
        mProgress = Utils.formatPercentage((int) (percentOfTotal + 0.5));
        notifyChanged();
    }

    BatteryEntry getInfo() {
        return mInfo;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
        icon.setLayoutParams(new LinearLayout.LayoutParams(mIconSize, mIconSize));

        ((TextView) view.findViewById(R.id.widget_summary)).setText(mProgress);
        if (mContentDescription != null) {
            final TextView titleView = (TextView) view.findViewById(android.R.id.title);
            titleView.setContentDescription(mContentDescription);
        }
    }
}
