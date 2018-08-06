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

package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import android.util.ArraySet;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import com.android.settingslib.applications.DefaultAppInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fragment for choosing default browser.
 */
public class DefaultBrowserPicker extends DefaultAppPickerFragment {

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_browser_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_BROWSER_PICKER;
    }

    @Override
    protected String getDefaultKey() {
        return mPm.getDefaultBrowserPackageNameAsUser(mUserId);
    }

    @Override
    protected boolean setDefaultKey(String packageName) {
        return mPm.setDefaultBrowserPackageNameAsUser(packageName, mUserId);
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final Context context = getContext();
        // Resolve that intent and check that the handleAllWebDataURI boolean is set
        final List<ResolveInfo> list = mPm.queryIntentActivitiesAsUser(
                DefaultBrowserPreferenceController.BROWSE_PROBE, PackageManager.MATCH_ALL, mUserId);

        final int count = list.size();
        final Set<String> addedPackages = new ArraySet<>();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo == null || !info.handleAllWebDataURI) {
                continue;
            }
            final String packageName = info.activityInfo.packageName;
            if (addedPackages.contains(packageName)) {
                continue;
            }
            try {
                candidates.add(new DefaultAppInfo(context, mPm,
                        mPm.getApplicationInfoAsUser(packageName, 0, mUserId)));
                addedPackages.add(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // Skip unknown packages.
            }
        }

        return candidates;
    }
}
