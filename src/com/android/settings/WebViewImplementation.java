/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings;

import android.annotation.Nullable;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.util.Log;
import android.webkit.IWebViewUpdateService;
import android.webkit.WebViewProviderInfo;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;

public class WebViewImplementation extends InstrumentedActivity implements
        OnCancelListener, OnDismissListener {

    private static final String TAG = "WebViewImplementation";

    private IWebViewUpdateService mWebViewUpdateService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserManager.get(this).isAdminUser()) {
            finish();
            return;
        }
        mWebViewUpdateService  =
                IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));
        try {
            WebViewProviderInfo[] providers = mWebViewUpdateService.getValidWebViewPackages();
            if (providers == null) {
                Log.e(TAG, "No WebView providers available");
                finish();
                return;
            }

            String currentValue = mWebViewUpdateService.getCurrentWebViewPackageName();
            if (currentValue == null) {
                currentValue = "";
            }

            int currentIndex = -1;
            ArrayList<String> options = new ArrayList<>();
            final ArrayList<String> values = new ArrayList<>();
            for (WebViewProviderInfo provider : providers) {
                if (Utils.isPackageEnabled(this, provider.packageName)) {
                    options.add(provider.description);
                    values.add(provider.packageName);
                    if (currentValue.contentEquals(provider.packageName)) {
                        currentIndex = values.size() - 1;
                    }
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.select_webview_provider_dialog_title)
                    .setSingleChoiceItems(options.toArray(new String[0]), currentIndex,
                            new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                mWebViewUpdateService.changeProviderAndSetting(values.get(which));
                            } catch (RemoteException e) {
                                Log.w(TAG, "Problem reaching webviewupdate service", e);
                            }
                            finish();
                        }
                    }).setNegativeButton(android.R.string.cancel, null)
                    .setOnCancelListener(this)
                    .setOnDismissListener(this)
                    .show();
        } catch (RemoteException e) {
            Log.w(TAG, "Problem reaching webviewupdate service", e);
            finish();
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WEBVIEW_IMPLEMENTATION;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
