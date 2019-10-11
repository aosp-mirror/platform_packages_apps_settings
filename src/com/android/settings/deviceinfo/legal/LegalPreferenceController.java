/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.deviceinfo.legal;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import java.util.List;


public abstract class LegalPreferenceController extends BasePreferenceController {
    private final PackageManager mPackageManager;
    private Preference mPreference;

    public LegalPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        if (findMatchingSpecificActivity() != null) {
            return AVAILABLE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);

        if (getAvailabilityStatus() == AVAILABLE) {
            replacePreferenceIntent();
        }
    }

    protected abstract Intent getIntent();

    private ResolveInfo findMatchingSpecificActivity() {
        final Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        // Find the activity that is in the system image
        final List<ResolveInfo> list = mPackageManager.queryIntentActivities(intent, 0);
        if (list == null) {
            return null;
        }

        for (ResolveInfo resolveInfo : list) {
            if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                    != 0) {
                return resolveInfo;
            }
        }

        // Did not find a matching activity
        return null;
    }

    private void replacePreferenceIntent() {
        final ResolveInfo resolveInfo = findMatchingSpecificActivity();
        if (resolveInfo == null) {
            return;
        }

        // Replace the intent with this specific activity
        mPreference.setIntent(new Intent().setClassName(
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name));

        mPreference.setTitle(resolveInfo.loadLabel(mPackageManager));
    }
}
