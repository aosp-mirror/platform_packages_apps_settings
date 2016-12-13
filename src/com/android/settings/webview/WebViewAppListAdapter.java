/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.webview;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.android.settings.applications.AppViewHolder;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom list adapter for Settings to choose WebView package.
 * Note: parts of this class are copied from AppPicker.java.
 */
class WebViewAppListAdapter extends ArrayAdapter<WebViewApplicationInfo> {
    private final LayoutInflater mInflater;

    public WebViewAppListAdapter(Context context,
            WebViewUpdateServiceWrapper webviewUpdateServiceWrapper) {
        super(context, 0);
        mInflater = LayoutInflater.from(context);

        final List<WebViewApplicationInfo> packageInfoList =
                new ArrayList<WebViewApplicationInfo>();
        List<ApplicationInfo> pkgs =
                webviewUpdateServiceWrapper.getValidWebViewApplicationInfos(getContext());
        for (ApplicationInfo ai : pkgs) {
            WebViewApplicationInfo info = new WebViewApplicationInfo(ai,
                    ai.loadLabel(context.getPackageManager()).toString(),
                    getDisabledReason(webviewUpdateServiceWrapper, context, ai.packageName));
            packageInfoList.add(info);
        }
        addAll(packageInfoList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        AppViewHolder holder = AppViewHolder.createOrRecycle(mInflater, convertView);
        convertView = holder.rootView;
        WebViewApplicationInfo info = getItem(position);
        holder.appName.setText(info.label);
        if (info.info != null) {
            holder.appIcon.setImageDrawable(info.info.loadIcon(getContext().getPackageManager()));
            // Allow disable-description to wrap - to be able to show several lines of text in case
            // a package is disabled/uninstalled for several users.
            holder.summary.setSingleLine(false);
            if (!isEnabled(position)) {
                holder.summary.setText(info.disabledReason);
            } else {
                holder.summary.setText("");
            }
        } else {
            holder.appIcon.setImageDrawable(null);
            holder.summary.setText("");
        }
        holder.disabled.setVisibility(View.GONE);
        // Only allow a package to be chosen if it is enabled and installed for all users.
        convertView.setEnabled(isEnabled(position));
        return convertView;
    }

    @Override
    public boolean isEnabled (int position) {
        WebViewApplicationInfo info = getItem(position);
        return info.disabledReason == null;
    }

    @Override
    public boolean areAllItemsEnabled() {
        int numItems = getCount();
        for (int n = 0; n < numItems; n++) {
            if (!isEnabled(n)) return false;
        }
        return true;
    }

    /**
     * Returns the reason why a package cannot be used as WebView implementation.
     * This is either because of it being disabled, uninstalled, or hidden for any user.
     */
    @VisibleForTesting
    static String getDisabledReason(WebViewUpdateServiceWrapper webviewUpdateServiceWrapper,
            Context context, String packageName) {
        StringBuilder disabledReason = new StringBuilder();
        List<UserPackageWrapper> userPackages =
                webviewUpdateServiceWrapper.getPackageInfosAllUsers(context, packageName);
        for (UserPackageWrapper userPackage : userPackages) {
            if (!userPackage.isInstalledPackage()) {
                // Package uninstalled/hidden
                disabledReason.append(context.getString(
                        R.string.webview_uninstalled_for_user, userPackage.getUserInfo().name));
            } else if (!userPackage.isEnabledPackage()) {
                // Package disabled
                disabledReason.append(context.getString(
                    R.string.webview_disabled_for_user, userPackage.getUserInfo().name));
            }
        }
        if (disabledReason.length() == 0) return null;
        return disabledReason.toString();
    }
}

