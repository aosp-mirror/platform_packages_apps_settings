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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.webkit.IWebViewUpdateService;
import android.webkit.UserPackage;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewUpdateManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WebViewUpdateServiceWrapper {
    private static final String TAG = "WVUSWrapper";

    public WebViewUpdateServiceWrapper() {
    }

    /**
     * Fetch the package currently used as WebView implementation.
     */
    public PackageInfo getCurrentWebViewPackage() {
        try {
            if (android.webkit.Flags.updateServiceIpcWrapper()) {
                return WebViewUpdateManager.getInstance().getCurrentWebViewPackage();
            } else {
                return WebViewFactory.getUpdateService().getCurrentWebViewPackage();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * Fetches ApplicationInfo objects for all currently valid WebView packages.
     * A WebView package is considered valid if it can be used as a WebView implementation. The
     * validity of a package is not dependent on whether the package is installed/enabled.
     */
    public List<ApplicationInfo> getValidWebViewApplicationInfos(Context context) {
        WebViewProviderInfo[] providers = null;
        try {
            if (android.webkit.Flags.updateServiceIpcWrapper()) {
                providers = context.getSystemService(WebViewUpdateManager.class)
                        .getValidWebViewPackages();
            } else {
                providers = WebViewFactory.getUpdateService().getValidWebViewPackages();
            }
        } catch (Exception e) {
        }
        List<ApplicationInfo> pkgs = new ArrayList<>();
        for (WebViewProviderInfo provider : providers) {
            try {
                pkgs.add(context.getPackageManager().getApplicationInfo(
                        provider.packageName, PACKAGE_FLAGS));
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return pkgs;
    }

    /**
     * Change WebView provider to {@param packageName}.
     *
     * @return whether the change succeeded.
     */
    public boolean setWebViewProvider(String packageName) {
        try {
            if (android.webkit.Flags.updateServiceIpcWrapper()) {
                return packageName.equals(
                        WebViewUpdateManager.getInstance().changeProviderAndSetting(packageName));
            } else {
                return packageName.equals(
                        WebViewFactory.getUpdateService().changeProviderAndSetting(packageName));
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception when trying to change provider to " + packageName, e);
        }
        return false;
    }

    /**
     * Fetch PackageInfos for the package named {@param packageName} for all users on the device.
     */
    public List<UserPackage> getPackageInfosAllUsers(Context context, String packageName) {
        return UserPackage.getPackageInfosAllUsers(context, packageName, PACKAGE_FLAGS);
    }

    /**
     * Show a toast to explain the chosen package can no longer be chosen.
     */
    public void showInvalidChoiceToast(Context context) {
        // The user chose a package that became invalid since the list was last updated,
        // show a Toast to explain the situation.
        Toast toast = Toast.makeText(context,
                com.android.settingslib.R.string.select_webview_provider_toast_text,
                Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Fetch the package name of the default WebView provider.
     */
    @Nullable
    public String getDefaultWebViewPackageName() {
        WebViewProviderInfo provider = null;
        try {
            if (android.webkit.Flags.updateServiceIpcWrapper()) {
                WebViewUpdateManager manager = WebViewUpdateManager.getInstance();
                if (manager != null) {
                    provider = manager.getDefaultWebViewPackage();
                }
            } else {
                IWebViewUpdateService service = WebViewFactory.getUpdateService();
                if (service != null) {
                    provider = service.getDefaultWebViewPackage();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception when trying to fetch default WebView package Name", e);
        }
        return provider != null ? provider.packageName : null;
    }

    static final int PACKAGE_FLAGS = PackageManager.MATCH_ANY_USER;
}
