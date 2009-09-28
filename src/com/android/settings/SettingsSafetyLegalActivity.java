/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * The "dialog" that shows from "Safety information" in the Settings app.
 */
public class SettingsSafetyLegalActivity extends AlertActivity {
    private static final String PROPERTY_LSAFETYLEGAL_URL = "ro.url.safetylegal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String userSafetylegalUrl = SystemProperties.get(PROPERTY_LSAFETYLEGAL_URL);

        final Configuration configuration = getResources().getConfiguration();
        final String language = configuration.locale.getLanguage();
        final String country = configuration.locale.getCountry();

        String loc = String.format("locale=%s-%s", language, country);

        userSafetylegalUrl = String.format("%s&%s", userSafetylegalUrl, loc);

        if (!isDataNetworkConnected()) {
            showErrorAndFinish(userSafetylegalUrl);
            return;
        }

        WebView webView = new WebView(this);

        // Begin accessing
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(userSafetylegalUrl);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Change from 'Loading...' to the real title
                mAlert.setTitle(getString(R.string.settings_safetylegal_activity_title));
            }
        });

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.settings_safetylegal_activity_loading);
        p.mView = webView;
        p.mForceInverseBackground = true;
        setupAlert();
    }

    private void showErrorAndFinish(String url) {
        new AlertDialog.Builder(this)
                .setMessage(getResources()
                        .getString(R.string.settings_safetylegal_activity_unreachable, url))
                .setTitle(R.string.settings_safetylegal_activity_title)
                .setPositiveButton(android.R.string.ok, mOkListener)
                .setCancelable(true)
                .show();
    }

    private boolean isDataNetworkConnected() {
        TelephonyManager mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        if (mTelephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    private final OnClickListener mOkListener = new OnClickListener() {
         public void onClick(DialogInterface dialog, int whichButton) {
             finish();
         }
    };
}
