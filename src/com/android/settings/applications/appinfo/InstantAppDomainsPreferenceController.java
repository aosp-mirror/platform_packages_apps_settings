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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;

import com.android.settings.Utils;
import com.android.settings.applications.AppDomainsPreference;
import com.android.settingslib.applications.AppUtils;

import java.util.Set;

public class InstantAppDomainsPreferenceController extends AppInfoPreferenceControllerBase {

    private PackageManager mPackageManager;

    public InstantAppDomainsPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return mParent.getPackageInfo() != null
                && AppUtils.isInstant(mParent.getPackageInfo().applicationInfo)
                ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        final AppDomainsPreference instantAppDomainsPreference = (AppDomainsPreference) preference;
        final Set<String> handledDomainSet =
                Utils.getHandledDomains(mPackageManager, mParent.getPackageInfo().packageName);
        final String[] handledDomains =
                handledDomainSet.toArray(new String[handledDomainSet.size()]);
        instantAppDomainsPreference.setTitles(handledDomains);
        // Dummy values, unused in the implementation
        instantAppDomainsPreference.setValues(new int[handledDomains.length]);
    }

}
