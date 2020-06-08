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

import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;

import android.app.settings.SettingsEnums;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArraySet;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.applications.AppUtils;

public class AppLaunchSettings extends AppInfoWithHeader implements OnClickListener,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "AppLaunchSettings";
    private static final String KEY_APP_LINK_STATE = "app_link_state";
    private static final String KEY_SUPPORTED_DOMAIN_URLS = "app_launch_supported_domain_urls";
    private static final String KEY_CLEAR_DEFAULTS = "app_launch_clear_defaults";
    private static final String FRAGMENT_OPEN_SUPPORTED_LINKS =
            "com.android.settings.applications.OpenSupportedLinks";

    private PackageManager mPm;

    private boolean mIsBrowser;
    private boolean mHasDomainUrls;
    private Preference mAppLinkState;
    private AppDomainsPreference mAppDomainUrls;
    private ClearDefaultsPreference mClearDefaultsPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.installed_app_launch_settings);
        mAppDomainUrls = (AppDomainsPreference) findPreference(KEY_SUPPORTED_DOMAIN_URLS);
        mClearDefaultsPreference = (ClearDefaultsPreference) findPreference(KEY_CLEAR_DEFAULTS);
        mAppLinkState = findPreference(KEY_APP_LINK_STATE);
        mAppLinkState.setOnPreferenceClickListener(preference -> {
            final Bundle args = new Bundle();
            args.putString(ARG_PACKAGE_NAME, mPackageName);
            args.putInt(ARG_PACKAGE_UID, mUserId);

            new SubSettingLauncher(this.getContext())
                    .setDestination(FRAGMENT_OPEN_SUPPORTED_LINKS)
                    .setArguments(args)
                    .setSourceMetricsCategory(SettingsEnums.APPLICATIONS_APP_LAUNCH)
                    .setTitleRes(-1)
                    .launch();
            return true;
        });

        mPm = getActivity().getPackageManager();

        mIsBrowser = AppUtils.isBrowserApp(this.getContext(), mPackageName, UserHandle.myUserId());
        mHasDomainUrls =
                (mAppEntry.info.privateFlags & ApplicationInfo.PRIVATE_FLAG_HAS_DOMAIN_URLS) != 0;

        if (!mIsBrowser) {
            CharSequence[] entries = getEntries(mPackageName);
            mAppDomainUrls.setTitles(entries);
            mAppDomainUrls.setValues(new int[entries.length]);
            mAppLinkState.setEnabled(mHasDomainUrls);
        } else {
            // Browsers don't show the app-link prefs
            mAppLinkState.setShouldDisableView(true);
            mAppLinkState.setEnabled(false);
            mAppDomainUrls.setShouldDisableView(true);
            mAppDomainUrls.setEnabled(false);
        }
    }

    private int linkStateToResourceId(int state) {
        switch (state) {
            case INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS:
                return R.string.app_link_open_always; // Always
            case INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER:
                return R.string.app_link_open_never; // Never
            default:
                return R.string.app_link_open_ask; // Ask
        }
    }

    private CharSequence[] getEntries(String packageName) {
        ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
        return result.toArray(new CharSequence[result.size()]);
    }

    private void setAppLinkStateSummary() {
        final int state = mPm.getIntentVerificationStatusAsUser(mPackageName,
                UserHandle.myUserId());
        mAppLinkState.setSummary(linkStateToResourceId(state));
    }

    @Override
    protected boolean refreshUi() {
        if (mHasDomainUrls) {
            //Update the summary after return from the OpenSupportedLinks
            setAppLinkStateSummary();
        }
        mClearDefaultsPreference.setPackageName(mPackageName);
        mClearDefaultsPreference.setAppEntry(mAppEntry);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        // No dialogs for preferred launch settings.
        return null;
    }

    @Override
    public void onClick(View v) {
        // Nothing to do
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // actual updates are handled by the app link dropdown callback
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_APP_LAUNCH;
    }
}
