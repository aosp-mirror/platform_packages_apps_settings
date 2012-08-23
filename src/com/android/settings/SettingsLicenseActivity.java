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
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * The "dialog" that shows from "License" in the Settings app.
 */
public class SettingsLicenseActivity extends Activity {

    private static final String TAG = "SettingsLicenseActivity";
    private static final boolean LOGV = false || false;

    private static final String DEFAULT_LICENSE_PATH = "/system/etc/NOTICE.html.gz";
    private static final String PROPERTY_LICENSE_PATH = "ro.config.license_path";

    private Handler mHandler;
    private WebView mWebView;
    private ProgressDialog mSpinnerDlg;
    private AlertDialog mTextDlg;

    private class LicenseFileLoader implements Runnable {

        private static final String INNER_TAG = "SettingsLicenseActivity.LicenseFileLoader";
        public static final int STATUS_OK = 0;
        public static final int STATUS_NOT_FOUND = 1;
        public static final int STATUS_READ_ERROR = 2;
        public static final int STATUS_EMPTY_FILE = 3;

        private String mFileName;
        private Handler mHandler;

        public LicenseFileLoader(String fileName, Handler handler) {
            mFileName = fileName;
            mHandler = handler;
        }

        public void run() {

            int status = STATUS_OK;

            InputStreamReader inputReader = null;
            StringBuilder data = new StringBuilder(2048);
            try {
                char[] tmp = new char[2048];
                int numRead;
                if (mFileName.endsWith(".gz")) {
                    inputReader = new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(mFileName)));
                } else {
                    inputReader = new FileReader(mFileName);
                }

                while ((numRead = inputReader.read(tmp)) >= 0) {
                    data.append(tmp, 0, numRead);
                }
            } catch (FileNotFoundException e) {
                Log.e(INNER_TAG, "License HTML file not found at " + mFileName, e);
                status = STATUS_NOT_FOUND;
            } catch (IOException e) {
                Log.e(INNER_TAG, "Error reading license HTML file at " + mFileName, e);
                status = STATUS_READ_ERROR;
            } finally {
                try {
                    if (inputReader != null) {
                        inputReader.close();
                    }
                } catch (IOException e) {
                }
            }

            if ((status == STATUS_OK) && TextUtils.isEmpty(data)) {
                Log.e(INNER_TAG, "License HTML is empty (from " + mFileName + ")");
                status = STATUS_EMPTY_FILE;
            }

            // Tell the UI thread that we are finished.
            Message msg = mHandler.obtainMessage(status, null);
            if (status == STATUS_OK) {
                msg.obj = data.toString();
            }
            mHandler.sendMessage(msg);
        }
    }

    public SettingsLicenseActivity() {
        super();
        mHandler = null;
        mWebView = null;
        mSpinnerDlg = null;
        mTextDlg = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String fileName = SystemProperties.get(PROPERTY_LICENSE_PATH, DEFAULT_LICENSE_PATH);
        if (TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "The system property for the license file is empty.");
            showErrorAndFinish();
            return;
        }

        // The activity does not have any view itself,
        // so set it invisible to avoid displaying the title text in the background.
        setVisible(false);

        mWebView = new WebView(this);

        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == LicenseFileLoader.STATUS_OK) {
                    String text = (String) msg.obj;
                    showPageOfText(text);
                } else {
                    showErrorAndFinish();
                }
            }
        };

        CharSequence title = getText(R.string.settings_license_activity_title);
        CharSequence msg = getText(R.string.settings_license_activity_loading);

        ProgressDialog pd = ProgressDialog.show(this, title, msg, true, false);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mSpinnerDlg = pd;

        // Start separate thread to do the actual loading.
        Thread thread = new Thread(new LicenseFileLoader(fileName, mHandler));
        thread.start();
    }

    @Override
    protected void onDestroy() {
        if (mTextDlg != null && mTextDlg.isShowing()) {
            mTextDlg.dismiss();
        }
        if (mSpinnerDlg != null && mSpinnerDlg.isShowing()) {
            mSpinnerDlg.dismiss();
        }
        super.onDestroy();
    }

    private void showPageOfText(String text) {
        // Create an AlertDialog to display the WebView in.
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsLicenseActivity.this);
        builder.setCancelable(true)
               .setView(mWebView)
               .setTitle(R.string.settings_license_activity_title);

        mTextDlg = builder.create();
        mTextDlg.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(DialogInterface dlgi) {
                SettingsLicenseActivity.this.finish();
            }
        });

        // Begin the loading.  This will be done in a separate thread in WebView.
        mWebView.loadDataWithBaseURL(null, text, "text/html", "utf-8", null);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mSpinnerDlg.dismiss();
                if (SettingsLicenseActivity.this.isResumed()) {
                    mTextDlg.show();
                }
            }
        });

        mWebView = null;
    }

    private void showErrorAndFinish() {
        mSpinnerDlg.dismiss();
        mSpinnerDlg = null;
        Toast.makeText(this, R.string.settings_license_activity_unavailable, Toast.LENGTH_LONG)
                .show();
        finish();
    }
}
