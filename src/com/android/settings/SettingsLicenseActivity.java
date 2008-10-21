/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import org.apache.commons.codec.binary.Base64;

/**
 * The "dialog" that shows from "License" in the Settings app.
 */
public class SettingsLicenseActivity extends AlertActivity {

    private static final String TAG = "SettingsLicenseActivity";
    private static final boolean LOGV = false || Config.LOGV;
    
    private static final String DEFAULT_LICENSE_PATH = "/system/etc/NOTICE.html";
    private static final String PROPERTY_LICENSE_PATH = "ro.config.license_path";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String fileName = SystemProperties.get(PROPERTY_LICENSE_PATH, DEFAULT_LICENSE_PATH);
        if (TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "The system property for the license file is empty.");
            showErrorAndFinish();
            return;
        }

        FileReader fileReader = null;
        StringBuilder data = null;
        try {
            data = new StringBuilder(2048); 
            char tmp[] = new char[2048];
            int numRead;
            fileReader = new FileReader(fileName);
            while ((numRead = fileReader.read(tmp)) >= 0) {
                data.append(tmp, 0, numRead);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "License HTML file not found at " + fileName, e);
            showErrorAndFinish();
            return;
        } catch (IOException e) {
            Log.e(TAG, "Error reading license HTML file at " + fileName, e);
            showErrorAndFinish();
            return;
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
            }
        }
    
        if (TextUtils.isEmpty(data)) {
            Log.e(TAG, "License HTML is empty (from " + fileName + ")");
            showErrorAndFinish();
            return;
        }
        
        WebView webView = new WebView(this);

        if (LOGV) Log.v(TAG, "Started encode at " + System.currentTimeMillis());
        // Need to encode to base64 for WebView to load the contents properly
        String dataStr;
        try {
            byte[] base64Bytes = Base64.encodeBase64(data.toString().getBytes("ISO8859_1"));
            dataStr = new String(base64Bytes);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not convert to base64", e);
            showErrorAndFinish();
            return;
        }
        if (LOGV) Log.v(TAG, "Ended encode at " + System.currentTimeMillis());
        if (LOGV) {
            Log.v(TAG, "Started test decode at " + System.currentTimeMillis());
            Base64.decodeBase64(dataStr.getBytes());
            Log.v(TAG, "Ended decode at " + System.currentTimeMillis());
        }

        
        // Begin the loading.  This will be done in a separate thread in WebView.
        webView.loadData(dataStr, "text/html", "base64");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Change from 'Loading...' to the real title
                mAlert.setTitle(getString(R.string.settings_license_activity_title));
            }
        });
        
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.settings_license_activity_loading);
        p.mView = webView;
        p.mForceInverseBackground = true;
        setupAlert();
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_license_activity_unavailable, Toast.LENGTH_LONG)
                .show();
        finish();
    }
    
}
