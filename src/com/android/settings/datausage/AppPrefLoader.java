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

package com.android.settings.datausage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.ArraySet;

import androidx.preference.Preference;

import com.android.settingslib.utils.AsyncLoaderCompat;

public class AppPrefLoader extends AsyncLoaderCompat<ArraySet<Preference>> {
    private ArraySet<String> mPackages;
    private PackageManager mPackageManager;
    private Context mPrefContext;

    public AppPrefLoader(Context prefContext, ArraySet<String> pkgs, PackageManager pm) {
        super(prefContext);
        mPackages = pkgs;
        mPackageManager = pm;
        mPrefContext = prefContext;
    }

    @Override
    public ArraySet<Preference> loadInBackground() {
        ArraySet<Preference> results = new ArraySet<>();
        for (int i = 1, size = mPackages.size(); i < size; i++) {
            try {
                ApplicationInfo info = mPackageManager.getApplicationInfo(mPackages.valueAt(i), 0);
                Preference preference = new Preference(mPrefContext);
                preference.setIcon(info.loadIcon(mPackageManager));
                preference.setTitle(info.loadLabel(mPackageManager));
                preference.setSelectable(false);
                results.add(preference);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return results;
    }

    @Override
    protected void onDiscardResult(ArraySet<Preference> result) {
    }
}
