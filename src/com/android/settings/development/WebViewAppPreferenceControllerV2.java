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

package com.android.settings.development;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppInfo;
import com.android.settings.webview.WebViewUpdateServiceWrapper;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public class WebViewAppPreferenceControllerV2 extends DeveloperOptionsPreferenceController {

    private static final String TAG = "WebViewAppPrefCtrl";
    private static final String WEBVIEW_APP_KEY = "select_webview_provider";

    private final PackageManagerWrapper mPackageManager;
    private final WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    private Preference mPreference;

    public WebViewAppPreferenceControllerV2(Context context) {
        super(context);

        mPackageManager = new PackageManagerWrapper(context.getPackageManager());
        mWebViewUpdateServiceWrapper = new WebViewUpdateServiceWrapper();
    }

    @Override
    public String getPreferenceKey() {
        return WEBVIEW_APP_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        final CharSequence defaultAppLabel = getDefaultAppLabel();
        if (!TextUtils.isEmpty(defaultAppLabel)) {
            mPreference.setSummary(defaultAppLabel);
            mPreference.setIcon(getDefaultAppIcon());
        } else {
            Log.d(TAG, "No default app");
            mPreference.setSummary(R.string.app_list_preference_none);
            mPreference.setIcon(null);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mPreference.setEnabled(false);
    }

    @VisibleForTesting
    DefaultAppInfo getDefaultAppInfo() {
        final PackageInfo currentPackage = mWebViewUpdateServiceWrapper.getCurrentWebViewPackage();
        return new DefaultAppInfo(mPackageManager,
                currentPackage == null ? null : currentPackage.applicationInfo);
    }

    private Drawable getDefaultAppIcon() {
        final DefaultAppInfo app = getDefaultAppInfo();
        return app.loadIcon();
    }

    private CharSequence getDefaultAppLabel() {
        final DefaultAppInfo app = getDefaultAppInfo();
        return app.loadLabel();
    }
}
