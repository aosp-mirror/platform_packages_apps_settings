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

import static com.android.settings.deviceinfo.legal.ModuleLicenseProvider.LICENSE_FILE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class ModuleLicenseProviderTest {
    public static final String PACKAGE_NAME = "com.android.test_package";
    @Test
    public void onCreate_returnsTrue() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        assertThat(provider.onCreate()).isTrue();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void query_throwsUnsupportedOperationException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.query(null, null, null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void insert_throwsUnsupportedOperationException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.insert(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void delete_throwsUnsupportedOperationException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.delete(null, null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void update_throwsUnsupportedOperationException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.update(null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getType_notContentScheme_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.getType(new Uri.Builder()
                .scheme("badscheme")
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getType_invalidAuthority_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.getType(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("notmyauthority")
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getType_emptyPath_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.getType(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getType_missingPackageName_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.getType(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(LICENSE_FILE_NAME)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getType_missingFileName_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.getType(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getType_incorrectFileName_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.getType(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath("badname.txt")
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getType_packageNotAModule_throwsIllegalArgumentException()
            throws PackageManager.NameNotFoundException {
        ModuleLicenseProvider provider = spy(new ModuleLicenseProvider());
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(provider.getContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getModuleInfo(PACKAGE_NAME, 0))
                .thenThrow(new PackageManager.NameNotFoundException());

        provider.getType(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build());
    }

    @Test
    public void getType_validUri_returnsHtmlMimeType()
            throws PackageManager.NameNotFoundException {
        ModuleLicenseProvider provider = spy(new ModuleLicenseProvider());
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(provider.getContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getModuleInfo(PACKAGE_NAME, 0))
                .thenReturn(new ModuleInfo());

        assertThat(provider.getType(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build())).isEqualTo(ModuleLicenseProvider.LICENSE_FILE_MIME_TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_notContentScheme_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.openFile(new Uri.Builder()
                .scheme("badscheme")
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build(), "r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_invalidAuthority_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.openFile(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("notmyauthority")
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build(), "r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_emptyPath_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.openFile(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .build(), "r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_missingPackageName_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.openFile(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(LICENSE_FILE_NAME)
                .build(), "r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_missingFileName_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.openFile(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .build(), "r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_incorrectFileName_throwsIllegalArgumentException() {
        ModuleLicenseProvider provider = new ModuleLicenseProvider();
        provider.openFile(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath("badname.txt")
                .build(), "r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_packageNotAModule_throwsIllegalArgumentException()
            throws PackageManager.NameNotFoundException {
        ModuleLicenseProvider provider = spy(new ModuleLicenseProvider());
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(provider.getContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getModuleInfo(PACKAGE_NAME, 0))
                .thenThrow(new PackageManager.NameNotFoundException());

        provider.openFile(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build(), "r");
    }

    @Test(expected = IllegalArgumentException.class)
    public void openFile_validUri_notReadMode_throwsIllegalArgumentException()
            throws PackageManager.NameNotFoundException {
        ModuleLicenseProvider provider = spy(new ModuleLicenseProvider());
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(provider.getContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getModuleInfo(PACKAGE_NAME, 0))
                .thenReturn(new ModuleInfo());

        provider.openFile(new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(ModuleLicenseProvider.AUTHORITY)
                .appendPath(PACKAGE_NAME)
                .appendPath(LICENSE_FILE_NAME)
                .build(), "badmode");
    }

    @Test
    public void isCachedHtmlFileOutdated_packageNotInSharedPrefs_returnTrue()
            throws PackageManager.NameNotFoundException {
        Context context = RuntimeEnvironment.application;
        context.getSharedPreferences(ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();

        assertThat(ModuleLicenseProvider.isCachedHtmlFileOutdated(context, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isCachedHtmlFileOutdated_versionCodeDiffersFromSharedPref_returnTrue()
            throws PackageManager.NameNotFoundException {
        Context context = spy(RuntimeEnvironment.application);
        SharedPreferences.Editor editor = context.getSharedPreferences(
                ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit();
        editor.clear().commit();
        editor.putLong(PACKAGE_NAME, 900L).commit();
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setLongVersionCode(1000L);
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo);

        assertThat(ModuleLicenseProvider.isCachedHtmlFileOutdated(context, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isCachedHtmlFileOutdated_fileDoesNotExist_returnTrue()
            throws PackageManager.NameNotFoundException {
        Context context = spy(RuntimeEnvironment.application);
        context.getSharedPreferences(ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
        SharedPreferences.Editor editor = context.getSharedPreferences(
                ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit();
        editor.clear().commit();
        editor.putLong(PACKAGE_NAME, 1000L).commit();
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setLongVersionCode(1000L);
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo);
        new File(context.getCacheDir() + "/" + PACKAGE_NAME, LICENSE_FILE_NAME).delete();

        assertThat(ModuleLicenseProvider.isCachedHtmlFileOutdated(context, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isCachedHtmlFileOutdated_fileIsEmpty_returnTrue()
            throws PackageManager.NameNotFoundException, IOException {
        Context context = spy(RuntimeEnvironment.application);
        context.getSharedPreferences(ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
        SharedPreferences.Editor editor = context.getSharedPreferences(
                ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit();
        editor.clear().commit();
        editor.putLong(PACKAGE_NAME, 1000L).commit();
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setLongVersionCode(1000L);
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo);
        new File(context.getCacheDir(), PACKAGE_NAME).mkdir();
        File file = new File(context.getCacheDir() + "/" + PACKAGE_NAME, LICENSE_FILE_NAME);
        file.delete();
        file.createNewFile();

        assertThat(ModuleLicenseProvider.isCachedHtmlFileOutdated(context, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isCachedHtmlFileOutdated_notOutdated_returnFalse()
            throws PackageManager.NameNotFoundException, IOException {
        Context context = spy(RuntimeEnvironment.application);
        context.getSharedPreferences(ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
        SharedPreferences.Editor editor = context.getSharedPreferences(
                ModuleLicenseProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit();
        editor.clear().commit();
        editor.putLong(PACKAGE_NAME, 1000L).commit();
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setLongVersionCode(1000L);
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo);
        new File(context.getCacheDir(), PACKAGE_NAME).mkdir();
        File file = new File(context.getCacheDir() + "/" + PACKAGE_NAME, LICENSE_FILE_NAME);
        file.delete();
        file.createNewFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("test");
        }

        assertThat(ModuleLicenseProvider.isCachedHtmlFileOutdated(context, PACKAGE_NAME)).isFalse();
    }

    @Test
    public void getUriForPackage_returnsProperlyFormattedUri() {
        assertThat(ModuleLicenseProvider.getUriForPackage(PACKAGE_NAME))
                .isEqualTo(Uri.parse("content://com.android.settings.module_licenses/com.android.test_package/NOTICE.html"));
    }
}
