/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.widget.FooterPreference;

/**
 * Display the Open Supported Links page. Allow users choose what kind supported links they need.
 */
public class OpenSupportedLinks extends DashboardFragment {
    private static final String TAG = "OpenSupportedLinks";
    private static final String FOOTER_KEY = "supported_links_footer";

    @VisibleForTesting
    PackageInfo mPackageInfo;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Bundle args = getArguments();
        mPackageInfo = (args != null) ? args.getParcelable(AppLaunchSettings.KEY_PACKAGE_INFO)
                : null;
        if (mPackageInfo == null) {
            Log.w(TAG, "Missing PackageInfo; maybe reinstalling?");
            return;
        }
        use(AppHeaderPreferenceController.class).setParentFragment(this).setPackageInfo(
                mPackageInfo).setLifeCycle(getSettingsLifecycle());
        use(AppOpenSupportedLinksPreferenceController.class).setInit(mPackageInfo.packageName);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        final FooterPreference footer = findPreference(FOOTER_KEY);
        if (footer == null) {
            Log.w(TAG, "Can't find the footer preference.");
            return;
        }
        addLinksToFooter(footer);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.open_supported_links;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.OPEN_SUPPORTED_LINKS;
    }

    @VisibleForTesting
    void addLinksToFooter(FooterPreference footer) {
        final ArraySet<String> result = Utils.getHandledDomains(getPackageManager(),
                mPackageInfo.packageName);
        if (result.isEmpty()) {
            Log.w(TAG, "Can't find any app links.");
            return;
        }
        CharSequence title = footer.getTitle() + System.lineSeparator();
        for (String link : result) {
            title = title + System.lineSeparator() + link;
        }
        footer.setTitle(title);
    }
}
