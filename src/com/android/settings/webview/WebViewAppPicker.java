/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.webview;

import static android.provider.Settings.ACTION_WEBVIEW_SETTINGS;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.webkit.UserPackage;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.ArrayList;
import java.util.List;

public class WebViewAppPicker extends DefaultAppPickerFragment {
    private WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    private WebViewUpdateServiceWrapper getWebViewUpdateServiceWrapper() {
        if (mWebViewUpdateServiceWrapper == null) {
            setWebViewUpdateServiceWrapper(createDefaultWebViewUpdateServiceWrapper());
        }
        return mWebViewUpdateServiceWrapper;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!mUserManager.isAdminUser()) {
            getActivity().finish();
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.webview_app_settings;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> packageInfoList = new ArrayList<DefaultAppInfo>();
        final Context context = getContext();
        final WebViewUpdateServiceWrapper webViewUpdateService = getWebViewUpdateServiceWrapper();
        final List<ApplicationInfo> pkgs =
                webViewUpdateService.getValidWebViewApplicationInfos(context);
        for (ApplicationInfo ai : pkgs) {
            packageInfoList.add(createDefaultAppInfo(context, mPm, ai,
                    getDisabledReason(webViewUpdateService, context, ai.packageName)));
        }
        return packageInfoList;
    }

    @Override
    protected String getDefaultKey() {
        PackageInfo currentPackage = getWebViewUpdateServiceWrapper().getCurrentWebViewPackage();
        return currentPackage == null ? null : currentPackage.packageName;
    }

    protected boolean setDefaultKey(String key) {
        boolean success = getWebViewUpdateServiceWrapper().setWebViewProvider(key);
        return success;
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        if (success) {
            Activity activity = getActivity();
            Intent intent = activity == null ? null : activity.getIntent();
            if (intent != null && ACTION_WEBVIEW_SETTINGS.equals(intent.getAction())) {
                // If this was started through ACTION_WEBVIEW_SETTINGS then return once we have
                // chosen a new package.
                getActivity().finish();
            }
        } else {
            getWebViewUpdateServiceWrapper().showInvalidChoiceToast(getActivity());
            updateCandidates();
        }
    }

    private WebViewUpdateServiceWrapper createDefaultWebViewUpdateServiceWrapper() {
        return new WebViewUpdateServiceWrapper();
    }

    @VisibleForTesting
    void setWebViewUpdateServiceWrapper(WebViewUpdateServiceWrapper wvusWrapper) {
        mWebViewUpdateServiceWrapper = wvusWrapper;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WEBVIEW_IMPLEMENTATION;
    }

    private static class WebViewAppInfo extends DefaultAppInfo {
        public WebViewAppInfo(Context context, PackageManager pm, int userId,
                PackageItemInfo packageItemInfo, String summary, boolean enabled) {
            super(context, pm, userId, packageItemInfo, summary, enabled);
        }

        @Override
        public CharSequence loadLabel() {
            String versionName = "";
            try {
                versionName = mPm.getPackageInfo(packageItemInfo.packageName, 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
            }
            return String.format("%s %s", super.loadLabel(), versionName);
        }
    }


    @VisibleForTesting
    DefaultAppInfo createDefaultAppInfo(Context context, PackageManager pm,
            PackageItemInfo packageItemInfo, String disabledReason) {
        return new WebViewAppInfo(context, pm, mUserId, packageItemInfo, disabledReason,
                TextUtils.isEmpty(disabledReason) /* enabled */);
    }

    /**
     * Returns the reason why a package cannot be used as WebView implementation.
     * This is either because of it being disabled, uninstalled, or hidden for any user.
     */
    @VisibleForTesting
    String getDisabledReason(WebViewUpdateServiceWrapper webviewUpdateServiceWrapper,
            Context context, String packageName) {
        List<UserPackage> userPackages =
                webviewUpdateServiceWrapper.getPackageInfosAllUsers(context, packageName);
        for (UserPackage userPackage : userPackages) {
            if (!userPackage.isInstalledPackage()) {
                // Package uninstalled/hidden
                return context.getString(
                        R.string.webview_uninstalled_for_user, userPackage.getUserInfo().name);
            } else if (!userPackage.isEnabledPackage()) {
                // Package disabled
                return context.getString(
                        R.string.webview_disabled_for_user, userPackage.getUserInfo().name);
            }
        }
        return null;
    }
}
