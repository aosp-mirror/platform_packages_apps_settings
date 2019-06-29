/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.deviceinfo.legal;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class ModuleLicenseProvider extends ContentProvider {
    private static final String TAG = "ModuleLicenseProvider";

    public static final String AUTHORITY = "com.android.settings.module_licenses";
    static final String GZIPPED_LICENSE_FILE_NAME = "NOTICE.html.gz";
    static final String LICENSE_FILE_NAME = "NOTICE.html";
    static final String LICENSE_FILE_MIME_TYPE = "text/html";
    static final String PREFS_NAME = "ModuleLicenseProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        checkUri(getContext(), uri);
        return LICENSE_FILE_MIME_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        final Context context = getContext();
        checkUri(context, uri);
        Preconditions.checkArgument("r".equals(mode), "Read is the only supported mode");

        try {
            String packageName = uri.getPathSegments().get(0);
            File cachedFile = getCachedHtmlFile(context, packageName);
            if (isCachedHtmlFileOutdated(context, packageName)) {
                try (InputStream in = new GZIPInputStream(
                        getPackageAssetManager(context.getPackageManager(), packageName)
                                .open(GZIPPED_LICENSE_FILE_NAME))) {
                    File directory = getCachedFileDirectory(context, packageName);
                    if (!directory.exists()) {
                        directory.mkdir();
                    }
                    Files.copy(in, cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                // Now that the file is saved, write the package's version code to shared prefs
                SharedPreferences.Editor editor = getPrefs(context).edit();
                editor.putLong(
                        packageName,
                        getPackageInfo(context, packageName).getLongVersionCode())
                                .commit();
            }
            return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "checkUri should have already caught this error", e);
        } catch (IOException e) {
            Log.e(TAG, "Could not open file descriptor", e);
        }
        return null;
    }

    /**
     * Returns true if the cached file for the given package is outdated. A cached file is
     * outdated if one of the following are true:
     * 1. the shared prefs does not contain a version code for this package
     * 2. The version code does not match the package's version code
     * 3. There is no file or the file is empty.
     */
    @VisibleForTesting
    static boolean isCachedHtmlFileOutdated(Context context, String packageName)
            throws PackageManager.NameNotFoundException {
        SharedPreferences prefs = getPrefs(context);
        File file = getCachedHtmlFile(context, packageName);
        return !prefs.contains(packageName)
                || prefs.getLong(packageName, 0L)
                        != getPackageInfo(context, packageName).getLongVersionCode()
                || !file.exists() || file.length() == 0;
    }

    static AssetManager getPackageAssetManager(PackageManager packageManager, String packageName)
            throws PackageManager.NameNotFoundException {
        return packageManager.getResourcesForApplication(
                packageManager.getPackageInfo(packageName, PackageManager.MATCH_APEX)
                        .applicationInfo)
                                .getAssets();
    }

    static Uri getUriForPackage(String packageName) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(packageName)
                .appendPath(LICENSE_FILE_NAME)
                .build();
    }

    private static void checkUri(Context context, Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        // A URI is valid iff it:
        // 1. is a content URI
        // 2. uses the correct authority
        // 3. has exactly 2 segments and the last one is NOTICE.html
        // 4. (checked below) first path segment is the package name of a module
        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())
                || !AUTHORITY.equals(uri.getAuthority())
                || pathSegments == null
                || pathSegments.size() != 2
                || !LICENSE_FILE_NAME.equals(pathSegments.get(1))) {
            throw new IllegalArgumentException(uri + "is not a valid URI");
        }
        // Grab the first path segment, which is the package name of the module and make sure that
        // there's actually a module for that package. getModuleInfo will throw if it does not
        // exist.
        try {
            context.getPackageManager().getModuleInfo(pathSegments.get(0), 0 /* flags */);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException(uri + "is not a valid URI", e);
        }
    }

    private static File getCachedFileDirectory(Context context, String packageName) {
        return new File(context.getCacheDir(), packageName);
    }

    private static File getCachedHtmlFile(Context context, String packageName) {
        return new File(context.getCacheDir() + "/" + packageName, LICENSE_FILE_NAME);
    }

    private static  PackageInfo getPackageInfo(Context context, String packageName)
            throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getPackageInfo(packageName, PackageManager.MATCH_APEX);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
