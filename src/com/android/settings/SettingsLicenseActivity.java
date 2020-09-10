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

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settingslib.license.LicenseHtmlLoaderCompat;

import java.io.File;

/**
 * The "dialog" that shows from "License" in the Settings app.
 */
public class SettingsLicenseActivity extends FragmentActivity implements
            LoaderManager.LoaderCallbacks<File> {
    private static final String TAG = "SettingsLicenseActivity";

    private static final String LICENSE_PATH = "/system/etc/NOTICE.html.gz";

    private static final int LOADER_ID_LICENSE_HTML_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File file = new File(LICENSE_PATH);
        if (isFileValid(file)) {
            showHtmlFromUri(Uri.fromFile(file));
        } else {
            showHtmlFromDefaultXmlFiles();
        }
    }

    @Override
    public Loader<File> onCreateLoader(int id, Bundle args) {
        return new LicenseHtmlLoaderCompat(this);
    }

    @Override
    public void onLoadFinished(Loader<File> loader, File generatedHtmlFile) {
        showGeneratedHtmlFile(generatedHtmlFile);
    }

    @Override
    public void onLoaderReset(Loader<File> loader) {
    }

    private void showHtmlFromDefaultXmlFiles() {
        getSupportLoaderManager().initLoader(LOADER_ID_LICENSE_HTML_LOADER, Bundle.EMPTY, this);
    }

    @VisibleForTesting
    Uri getUriFromGeneratedHtmlFile(File generatedHtmlFile) {
        return FileProvider.getUriForFile(this, Utils.FILE_PROVIDER_AUTHORITY,
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

    @VisibleForTesting
    boolean isFileValid(final File file) {
        return file.exists() && file.length() != 0;
    }
}
