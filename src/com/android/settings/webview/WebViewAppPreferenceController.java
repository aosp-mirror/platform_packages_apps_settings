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
import android.content.pm.PackageInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.applications.defaultapps.DefaultAppInfo;
import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;

public class WebViewAppPreferenceController extends DefaultAppPreferenceController {

    private static final String WEBVIEW_APP_KEY = "select_webview_provider";

    private final WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;
    private Preference mPreference;

    public WebViewAppPreferenceController(Context context) {
        this(context, new WebViewUpdateServiceWrapper());
    }

    public WebViewAppPreferenceController(Context context,
            WebViewUpdateServiceWrapper webviewUpdateServiceWrapper) {
        super(context);
        mWebViewUpdateServiceWrapper = webviewUpdateServiceWrapper;
    }

    @Override
    public DefaultAppInfo getDefaultAppInfo() {
        PackageInfo currentPackage = mWebViewUpdateServiceWrapper.getCurrentWebViewPackage();
        return new DefaultAppInfo(mPackageManager,
                currentPackage == null ? null : currentPackage.applicationInfo);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = screen.findPreference(WEBVIEW_APP_KEY);
        }
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
