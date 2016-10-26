/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.ApplicationsState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AppHeaderController {

    @IntDef({ActionType.ACTION_APP_INFO,
            ActionType.ACTION_APP_PREFERENCE,
            ActionType.ACTION_STORE_DEEP_LINK,
            ActionType.ACTION_NOTIF_PREFERENCE})
    @Retention(RetentionPolicy.SOURCE)
    @interface ActionType {
        int ACTION_APP_INFO = 0;
        int ACTION_STORE_DEEP_LINK = 1;
        int ACTION_APP_PREFERENCE = 2;
        int ACTION_NOTIF_PREFERENCE = 3;
    }

    private static final String TAG = "AppDetailFeature";

    private final Context mContext;

    public AppHeaderController(Context context) {
        mContext = context;
    }

    public void bindAppHeader(View appSnippet, PackageInfo packageInfo,
            ApplicationsState.AppEntry appEntry) {
        final String versionName = packageInfo == null ? null : packageInfo.versionName;
        final Resources res = appSnippet.getResources();

        // Set Icon
        final ImageView iconView = (ImageView) appSnippet.findViewById(android.R.id.icon);
        if (appEntry.icon != null) {
            iconView.setImageDrawable(appEntry.icon.getConstantState().newDrawable(res));
        }

        // Set application name.
        final TextView labelView = (TextView) appSnippet.findViewById(android.R.id.title);
        labelView.setText(appEntry.label);

        // Version number of application
        final TextView appVersion = (TextView) appSnippet.findViewById(android.R.id.summary);

        if (!TextUtils.isEmpty(versionName)) {
            appVersion.setSelected(true);
            appVersion.setVisibility(View.VISIBLE);
            appVersion.setText(res.getString(R.string.version_text, String.valueOf(versionName)));
        } else {
            appVersion.setVisibility(View.INVISIBLE);
        }
    }

    public void bindAppHeaderButtons(Fragment fragment, View appLinkButtons, String packageName,
            @ActionType int leftAction, @ActionType int rightAction) {
        ImageButton leftButton = (ImageButton) appLinkButtons.findViewById(R.id.left_button);
        ImageButton rightButton = (ImageButton) appLinkButtons.findViewById(R.id.right_button);

        bindAppDetailButton(fragment, packageName, leftButton, leftAction);
        bindAppDetailButton(fragment, packageName, rightButton, rightAction);
    }

    private void bindAppDetailButton(Fragment fragment, String packageName,
            ImageButton button, @ActionType int action) {
        if (button == null) {
            return;
        }
        switch (action) {
            case ActionType.ACTION_APP_INFO: {
                if (packageName == null || packageName.equals(Utils.OS_PKG)) {
                    button.setVisibility(View.GONE);
                } else {
                    // TODO
                    button.setImageResource(com.android.settings.R.drawable.ic_info);
                    button.setVisibility(View.VISIBLE);
                }
                return;
            }
            case ActionType.ACTION_STORE_DEEP_LINK: {
                final Intent intent = new Intent(Intent.ACTION_SHOW_APP_INFO)
                        .setPackage(getInstallerPackageName(mContext, packageName));
                final Intent result = resolveIntent(intent);
                if (result == null) {
                    button.setVisibility(View.GONE);
                } else {
                    result.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
                    button.setImageResource(R.drawable.ic_sim_sd);
                    button.setOnClickListener(v -> fragment.startActivity(intent));
                    button.setVisibility(View.VISIBLE);
                }
                return;
            }
            case ActionType.ACTION_NOTIF_PREFERENCE: {
                // TODO
                return;
            }
            case ActionType.ACTION_APP_PREFERENCE: {
                final Intent intent = resolveIntent(
                        new Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(packageName));
                if (intent == null) {
                    button.setVisibility(View.GONE);
                    return;
                }
                button.setOnClickListener(v -> fragment.startActivity(intent));
                button.setVisibility(View.VISIBLE);
                return;
            }
        }
    }

    private String getInstallerPackageName(Context context, String packageName) {
        try {
            return context.getPackageManager().getInstallerPackageName(packageName);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Exception while retrieving the package installer of " + packageName, e);
            return null;
        }
    }

    private Intent resolveIntent(Intent i) {
        ResolveInfo result = mContext.getPackageManager().resolveActivity(i, 0);
        if (result != null) {
            return new Intent(i.getAction())
                    .setClassName(result.activityInfo.packageName, result.activityInfo.name);
        }
        return null;
    }
}
