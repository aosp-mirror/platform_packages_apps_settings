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
import android.app.Fragment;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.InstalledAppDetails;

public class AppHeader {

    public static final String EXTRA_HIDE_INFO_BUTTON = "hideInfoButton";
    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon,
            CharSequence label, String pkgName, int uid) {
        createAppHeader(fragment, icon, label, pkgName, uid, 0, null);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon,
            CharSequence label, String pkgName, int uid, Intent externalSettings) {
        createAppHeader(fragment, icon, label, pkgName, uid, 0, externalSettings);
    }

    public static void createAppHeader(Activity activity, Drawable icon, CharSequence label,
            String pkgName, int uid, ViewGroup pinnedHeader) {
        final View bar = activity.getLayoutInflater().inflate(R.layout.app_header,
                pinnedHeader, false);
        setupHeaderView(activity, icon, label, pkgName, uid, false, 0, bar, null);
        pinnedHeader.addView(bar);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon,
            CharSequence label, String pkgName, int uid, int tintColorRes) {
        createAppHeader(fragment, icon, label, pkgName, uid, tintColorRes, null);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon,
            CharSequence label, String pkgName, int uid, int tintColorRes,
            Intent externalSettings) {
        View bar = fragment.setPinnedHeaderView(R.layout.app_header);
        setupHeaderView(fragment.getActivity(), icon, label, pkgName, uid, includeAppInfo(fragment),
                tintColorRes, bar, externalSettings);
    }

    public static View setupHeaderView(final Activity activity, Drawable icon, CharSequence label,
            final String pkgName, final int uid, final boolean includeAppInfo, int tintColorRes,
            View bar, final Intent externalSettings) {
        final ImageView appIcon = (ImageView) bar.findViewById(R.id.app_icon);
        appIcon.setImageDrawable(icon);
        if (tintColorRes != 0) {
            appIcon.setImageTintList(ColorStateList.valueOf(activity.getColor(tintColorRes)));
        }

        final TextView appName = (TextView) bar.findViewById(R.id.app_name);
        appName.setText(label);

        if (pkgName != null && !pkgName.equals(Utils.OS_PKG)) {
            bar.setClickable(true);
            bar.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (includeAppInfo) {
                        AppInfoBase.startAppInfoFragment(InstalledAppDetails.class,
                                R.string.application_info_label, pkgName, uid, activity,
                                INSTALLED_APP_DETAILS);
                    } else {
                        activity.finish();
                    }
                }
            });
            if (externalSettings != null) {
                final View appSettings = bar.findViewById(R.id.app_settings);
                appSettings.setVisibility(View.VISIBLE);
                appSettings.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.startActivity(externalSettings);
                    }
                });
            }
        }
        return bar;
    }

    public static boolean includeAppInfo(final Fragment fragment) {
        Bundle args = fragment.getArguments();
        boolean showInfo = true;
        if (args != null && args.getBoolean(EXTRA_HIDE_INFO_BUTTON, false)) {
            showInfo = false;
        }
        Intent intent = fragment.getActivity().getIntent();
        if (intent != null && intent.getBooleanExtra(EXTRA_HIDE_INFO_BUTTON, false)) {
            showInfo = false;
        }
        return showInfo;
    }
}
