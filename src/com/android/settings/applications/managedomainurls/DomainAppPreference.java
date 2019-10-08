/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.managedomainurls;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.widget.apppreference.AppPreference;

public class DomainAppPreference extends AppPreference {

    private final AppEntry mEntry;
    private final PackageManager mPm;
    private final IconDrawableFactory mIconDrawableFactory;

    public DomainAppPreference(final Context context, IconDrawableFactory iconFactory,
            AppEntry entry) {
        super(context);
        mIconDrawableFactory = iconFactory;
        mPm = context.getPackageManager();
        mEntry = entry;
        mEntry.ensureLabel(getContext());

        setState();
    }

    public void reuse() {
        setState();
        notifyChanged();
    }

    public AppEntry getEntry() {
        return mEntry;
    }

    private void setState() {
        setTitle(mEntry.label);
        setIcon(mIconDrawableFactory.getBadgedIcon(mEntry.info));
        setSummary(getDomainsSummary(mEntry.info.packageName));
    }

    private CharSequence getDomainsSummary(String packageName) {
        // If the user has explicitly said "no" for this package, that's the
        // string we should show.
        int domainStatus =
                mPm.getIntentVerificationStatusAsUser(packageName, UserHandle.myUserId());
        if (domainStatus == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER) {
            return getContext().getText(R.string.domain_urls_summary_none);
        }
        // Otherwise, ask package manager for the domains for this package,
        // and show the first one (or none if there aren't any).
        final ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
        if (result.isEmpty()) {
            return getContext().getText(R.string.domain_urls_summary_none);
        } else if (result.size() == 1) {
            return getContext().getString(R.string.domain_urls_summary_one, result.valueAt(0));
        } else {
            return getContext().getString(R.string.domain_urls_summary_some, result.valueAt(0));
        }
    }
}
