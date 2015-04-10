/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.annotation.Nullable;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.android.settings.InstrumentedFragment;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.R;

public class ManageDefaultApps extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = ManageDefaultApps.class.getSimpleName();

    private static final String KEY_DEFAULT_BROWSER = "default_browser";

    private DefaultBrowserPreference mDefaultBrowserPreference;
    private PackageManager mPm;
    private int myUserId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.default_apps);

        mPm = getPackageManager();
        myUserId = UserHandle.myUserId();

        mDefaultBrowserPreference = (DefaultBrowserPreference) findPreference(KEY_DEFAULT_BROWSER);
        mDefaultBrowserPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final CharSequence packageName = mDefaultBrowserPreference.getValue();
                        if (TextUtils.isEmpty(packageName)) {
                            return false;
                        }
                        return mPm.setDefaultBrowserPackageName(packageName.toString(), myUserId);
                    }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String packageName = getPackageManager().getDefaultBrowserPackageName(
                UserHandle.myUserId());
        if (!TextUtils.isEmpty(packageName)) {
            // Check if the package is still there
            Intent intent = new Intent();
            intent.setPackage(packageName);
            ResolveInfo info = mPm.resolveActivityAsUser(intent, 0, myUserId);
            if (info != null) {
                mDefaultBrowserPreference.setValue(packageName);
            } else {
                // Otherwise select the first one
                mDefaultBrowserPreference.setValueIndex(0);
            }
        } else {
            Log.d(TAG, "Cannot set empty default Browser value!");
        }
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.VIEW_CATEGORY_DEFAULT_APPS;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
}
