/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class AppHeader {

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon,
            CharSequence label, final Intent settingsIntent) {
        createAppHeader(fragment, icon, label, settingsIntent, 0);
    }

    public static void createAppHeader(Activity activity, Drawable icon, CharSequence label,
            final Intent settingsIntent, ViewGroup pinnedHeader) {
        final View bar = activity.getLayoutInflater().inflate(R.layout.app_header,
                pinnedHeader, false);
        setupHeaderView(activity, icon, label, settingsIntent, 0, bar);
        pinnedHeader.addView(bar);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon,
            CharSequence label, Intent settingsIntent, int tintColorRes) {
        View bar = fragment.setPinnedHeaderView(R.layout.app_header);
        setupHeaderView(fragment.getActivity(), icon, label, settingsIntent, tintColorRes, bar);
    }

    private static View setupHeaderView(final Activity activity, Drawable icon, CharSequence label,
            final Intent settingsIntent, int tintColorRes, View bar) {
        final ImageView appIcon = (ImageView) bar.findViewById(R.id.app_icon);
        appIcon.setImageDrawable(icon);
        if (tintColorRes != 0) {
            appIcon.setImageTintList(ColorStateList.valueOf(activity.getColor(tintColorRes)));
        }

        final TextView appName = (TextView) bar.findViewById(R.id.app_name);
        appName.setText(label);

        final View appSettings = bar.findViewById(R.id.app_settings);
        if (settingsIntent == null) {
            appSettings.setVisibility(View.GONE);
        } else {
            appSettings.setClickable(true);
            appSettings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.startActivity(settingsIntent);
                }
            });
        }

        return bar;
    }

}
