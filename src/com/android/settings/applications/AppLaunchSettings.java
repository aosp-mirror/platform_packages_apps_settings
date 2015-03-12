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

package com.android.settings.applications;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppLaunchSettings extends AppInfoWithHeader implements OnClickListener {

    private Button mActivitiesButton;
    private AppWidgetManager mAppWidgetManager;

    private View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppWidgetManager = AppWidgetManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.app_preferred_settings, container, false);

        final ViewGroup allDetails = (ViewGroup) view.findViewById(R.id.all_details);
        Utils.forceCustomPadding(allDetails, true /* additive padding */);

        mActivitiesButton = (Button) view.findViewById(R.id.clear_activities_button);
        mRootView = view;

        return view;
    }

    @Override
    protected boolean refreshUi() {
        retrieveAppEntry();
        boolean hasBindAppWidgetPermission =
                mAppWidgetManager.hasBindAppWidgetPermission(mAppEntry.info.packageName);

        TextView autoLaunchTitleView = (TextView) mRootView.findViewById(R.id.auto_launch_title);
        TextView autoLaunchView = (TextView) mRootView.findViewById(R.id.auto_launch);
        boolean autoLaunchEnabled = hasPreferredActivities(mPm, mPackageName)
                || hasUsbDefaults(mUsbManager, mPackageName);
        if (!autoLaunchEnabled && !hasBindAppWidgetPermission) {
            resetLaunchDefaultsUi(autoLaunchTitleView, autoLaunchView);
        } else {
            boolean useBullets = hasBindAppWidgetPermission && autoLaunchEnabled;

            if (hasBindAppWidgetPermission) {
                autoLaunchTitleView.setText(R.string.auto_launch_label_generic);
            } else {
                autoLaunchTitleView.setText(R.string.auto_launch_label);
            }

            CharSequence text = null;
            int bulletIndent = getResources()
                    .getDimensionPixelSize(R.dimen.installed_app_details_bullet_offset);
            if (autoLaunchEnabled) {
                CharSequence autoLaunchEnableText = getText(R.string.auto_launch_enable_text);
                SpannableString s = new SpannableString(autoLaunchEnableText);
                if (useBullets) {
                    s.setSpan(new BulletSpan(bulletIndent), 0, autoLaunchEnableText.length(), 0);
                }
                text = (text == null) ?
                        TextUtils.concat(s, "\n") : TextUtils.concat(text, "\n", s, "\n");
            }
            if (hasBindAppWidgetPermission) {
                CharSequence alwaysAllowBindAppWidgetsText =
                        getText(R.string.always_allow_bind_appwidgets_text);
                SpannableString s = new SpannableString(alwaysAllowBindAppWidgetsText);
                if (useBullets) {
                    s.setSpan(new BulletSpan(bulletIndent),
                            0, alwaysAllowBindAppWidgetsText.length(), 0);
                }
                text = (text == null) ?
                        TextUtils.concat(s, "\n") : TextUtils.concat(text, "\n", s, "\n");
            }
            autoLaunchView.setText(text);
            mActivitiesButton.setEnabled(true);
            mActivitiesButton.setOnClickListener(this);
        }
        return true;
    }

    private void resetLaunchDefaultsUi(TextView title, TextView autoLaunchView) {
        title.setText(R.string.auto_launch_label);
        autoLaunchView.setText(R.string.auto_launch_disable_text);
        // Disable clear activities button
        mActivitiesButton.setEnabled(false);
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        // No dialogs for preferred launch settings.
        return null;
    }

    @Override
    public void onClick(View v) {
        if (v == mActivitiesButton) {
            if (mUsbManager != null) {
                mPm.clearPackagePreferredActivities(mPackageName);
                try {
                    mUsbManager.clearDefaults(mPackageName, UserHandle.myUserId());
                } catch (RemoteException e) {
                    Log.e(TAG, "mUsbManager.clearDefaults", e);
                }
                mAppWidgetManager.setBindAppWidgetPermission(mPackageName, false);
                TextView autoLaunchTitleView =
                        (TextView) mRootView.findViewById(R.id.auto_launch_title);
                TextView autoLaunchView = (TextView) mRootView.findViewById(R.id.auto_launch);
                resetLaunchDefaultsUi(autoLaunchTitleView, autoLaunchView);
            }
        }
    }

    private static boolean hasUsbDefaults(IUsbManager usbManager, String packageName) {
        try {
            if (usbManager != null) {
                return usbManager.hasDefaults(packageName, UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "mUsbManager.hasDefaults", e);
        }
        return false;
    }

    private static boolean hasPreferredActivities(PackageManager pm, String packageName) {
        // Get list of preferred activities
        List<ComponentName> prefActList = Collections.emptyList();
        // Intent list cannot be null. so pass empty list
        List<IntentFilter> intentList = Collections.emptyList();
        pm.getPreferredActivities(intentList, prefActList, packageName);
        if (localLOGV) {
            Log.i(TAG, "Have " + prefActList.size() + " number of activities in preferred list");
        }
        return prefActList.size() > 0;
    }

    public static CharSequence getSummary(AppEntry appEntry, IUsbManager usbManager,
            PackageManager pm, Context context) {
        String packageName = appEntry.info.packageName;
        boolean hasPreferred = hasPreferredActivities(pm, packageName)
                || hasUsbDefaults(usbManager, packageName);
        return context.getString(hasPreferred
                ? R.string.launch_defaults_some
                : R.string.launch_defaults_none);
    }

}
