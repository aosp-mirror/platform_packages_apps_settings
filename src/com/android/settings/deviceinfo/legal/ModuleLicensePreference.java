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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ModuleInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;

/**
 * Preference in a list that represents a mainline module that has a licenses file.
 */
public class ModuleLicensePreference extends Preference {
    private static final String TAG = "ModuleLicensePreference";
    private final ModuleInfo mModule;

    public ModuleLicensePreference(Context context, ModuleInfo module) {
        super(context);
        mModule = module;
        setKey(module.getPackageName());
        setTitle(module.getName());
    }

    @Override
    protected void onClick() {
        // Kick off external viewer due to WebView security restrictions (Settings cannot use
        // WebView because it is UID 1000).
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(
                        ModuleLicenseProvider.getUriForPackage(mModule.getPackageName()),
                        ModuleLicenseProvider.LICENSE_FILE_MIME_TYPE)
                .putExtra(Intent.EXTRA_TITLE, mModule.getName())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage("com.android.htmlviewer");
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to find viewer", e);
            showError();
        }
    }

    private void showError() {
        Toast.makeText(
                getContext(), R.string.settings_license_activity_unavailable, Toast.LENGTH_LONG)
                .show();
    }
}
