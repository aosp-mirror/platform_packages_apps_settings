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

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.DevelopmentSettings;
import com.android.settings.core.PreferenceController;

public class WebViewAppPreferenceController extends PreferenceController {

    private static final String WEBVIEW_APP_KEY = "select_webview_provider";

    private Context mContext;
    private Preference mPreference;
    private final WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    public WebViewAppPreferenceController(Context context) {
        this(context, new WebViewUpdateServiceWrapper());
    }

    public WebViewAppPreferenceController(Context context,
            WebViewUpdateServiceWrapper webviewUpdateServiceWrapper) {
        super(context);
        mContext = context;
        mWebViewUpdateServiceWrapper = webviewUpdateServiceWrapper;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            return true;
        }
        return false;
    }

    public Intent getActivityIntent() {
        return new Intent(mContext, WebViewAppPicker.class);
    }

    @Override
    public void updateState(Preference preference) {
        mPreference.setSummary(getCurrentWebViewPackageLabel(mContext));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = screen.findPreference(WEBVIEW_APP_KEY);
        }
    }

    /**
     * Handle the return-value from the WebViewAppPicker Activity.
     */
    public void onActivityResult(int resultCode, Intent data) {
        // Update the preference summary no matter whether we succeeded to change the webview
        // implementation correctly - we might have changed implementation to one the user did not
        // choose.
        updateState(null);
    }

    private String getCurrentWebViewPackageLabel(Context context) {
        PackageInfo webViewPackage = mWebViewUpdateServiceWrapper.getCurrentWebViewPackage();
        if (webViewPackage == null) return "";
        return webViewPackage.applicationInfo.loadLabel(context.getPackageManager()).toString();
    }


    @Override
    public String getPreferenceKey() {
        return WEBVIEW_APP_KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public void enablePreference(boolean enabled) {
        if (isAvailable()) {
            mPreference.setEnabled(enabled);
        }
    }
}
