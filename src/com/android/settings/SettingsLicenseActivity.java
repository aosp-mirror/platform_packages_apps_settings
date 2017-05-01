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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.users.RestrictedProfileSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The "dialog" that shows from "License" in the Settings app.
 */
public class SettingsLicenseActivity extends Activity implements
            LoaderManager.LoaderCallbacks<File> {
    private static final String TAG = "SettingsLicenseActivity";

    private static final String DEFAULT_LICENSE_PATH = "/system/etc/NOTICE.html.gz";
    private static final String PROPERTY_LICENSE_PATH = "ro.config.license_path";

    private static final int LOADER_ID_LICENSE_HTML_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String licenseHtmlPath =
                SystemProperties.get(PROPERTY_LICENSE_PATH, DEFAULT_LICENSE_PATH);
        if (isFilePathValid(licenseHtmlPath)) {
            showSelectedFile(licenseHtmlPath);
        } else {
            showHtmlFromDefaultXmlFiles();
        }
    }

    @Override
    public Loader<File> onCreateLoader(int id, Bundle args) {
        return new LicenseHtmlLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<File> loader, File generatedHtmlFile) {
        showGeneratedHtmlFile(generatedHtmlFile);
    }

    @Override
    public void onLoaderReset(Loader<File> loader) {
    }

    private void showHtmlFromDefaultXmlFiles() {
        getLoaderManager().initLoader(LOADER_ID_LICENSE_HTML_LOADER, Bundle.EMPTY, this);
    }

    @VisibleForTesting
    Uri getUriFromGeneratedHtmlFile(File generatedHtmlFile) {
        return FileProvider.getUriForFile(this, RestrictedProfileSettings.FILE_PROVIDER_AUTHORITY,
                generatedHtmlFile);
    }

    private void showGeneratedHtmlFile(File generatedHtmlFile) {
        if (generatedHtmlFile != null) {
            showHtmlFromUri(getUriFromGeneratedHtmlFile(generatedHtmlFile));
        } else {
            Log.e(TAG, "Failed to generate.");
            showErrorAndFinish();
        }
    }

    private void showSelectedFile(final String path) {
        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "The system property for the license file is empty");
            showErrorAndFinish();
            return;
        }

        final File file = new File(path);
        if (!isFileValid(file)) {
            Log.e(TAG, "License file " + path + " does not exist");
            showErrorAndFinish();
            return;
        }
        showHtmlFromUri(Uri.fromFile(file));
     }

     private void showHtmlFromUri(Uri uri) {
        // Kick off external viewer due to WebView security restrictions; we
        // carefully point it at HTMLViewer, since it offers to decompress
        // before viewing.
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/html");
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.settings_license_activity_title));
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.htmlviewer");

        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to find viewer", e);
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_license_activity_unavailable, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    private boolean isFilePathValid(final String path) {
        return !TextUtils.isEmpty(path) && isFileValid(new File(path));
    }

    @VisibleForTesting
    boolean isFileValid(final File file) {
        return file.exists() && file.length() != 0;
    }
}
