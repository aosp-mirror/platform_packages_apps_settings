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

import android.app.AlertDialog;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.Utils;

import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;

import java.util.List;

public class AppLaunchSettings extends AppInfoWithHeader implements OnClickListener,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "AppLaunchSettings";

    private static final String KEY_OPEN_DOMAIN_URLS = "app_launch_open_domain_urls";
    private static final String KEY_SUPPORTED_DOMAIN_URLS = "app_launch_supported_domain_urls";
    private static final String KEY_CLEAR_DEFAULTS = "app_launch_clear_defaults";

    private PackageManager mPm;

    private boolean mHasDomainUrls;
    private SwitchPreference mOpenDomainUrls;
    private AppDomainsPreference mAppDomainUrls;
    private ClearDefaultsPreference mClearDefaultsPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.installed_app_launch_settings);

        mPm = getActivity().getPackageManager();

        mOpenDomainUrls = (SwitchPreference) findPreference(KEY_OPEN_DOMAIN_URLS);
        mOpenDomainUrls.setOnPreferenceChangeListener(this);

        mHasDomainUrls =
                (mAppEntry.info.privateFlags & ApplicationInfo.PRIVATE_FLAG_HAS_DOMAIN_URLS) != 0;
        List<IntentFilterVerificationInfo> iviList = mPm.getIntentFilterVerifications(mPackageName);

        List<IntentFilter> filters = mPm.getAllIntentFilters(mPackageName);

        mAppDomainUrls = (AppDomainsPreference) findPreference(KEY_SUPPORTED_DOMAIN_URLS);
        CharSequence[] entries = getEntries(mPackageName, iviList, filters);
        mAppDomainUrls.setTitles(entries);
        mAppDomainUrls.setValues(new int[entries.length]);

        mClearDefaultsPreference = (ClearDefaultsPreference) findPreference(KEY_CLEAR_DEFAULTS);

        updateDomainUrlPrefState();
    }

    private void updateDomainUrlPrefState() {
        mOpenDomainUrls.setEnabled(mHasDomainUrls);

        boolean checked = false;
        if (mHasDomainUrls) {
            final int status = mPm.getIntentVerificationStatus(mPackageName, UserHandle.myUserId());
            if (status == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS) {
                checked = true;
            }
        }
        mOpenDomainUrls.setChecked(checked);
    }

    private CharSequence[] getEntries(String packageName, List<IntentFilterVerificationInfo> iviList,
            List<IntentFilter> filters) {
        ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
        return result.toArray(new CharSequence[result.size()]);
    }

    @Override
    protected boolean refreshUi() {
        mClearDefaultsPreference.setPackageName(mPackageName);
        mClearDefaultsPreference.setAppEntry(mAppEntry);
        updateDomainUrlPrefState();
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
        boolean ret = false;
        final String key = preference.getKey();
        if (KEY_OPEN_DOMAIN_URLS.equals(key)) {
            final SwitchPreference pref = (SwitchPreference) preference;
            final Boolean switchedOn = (Boolean) newValue;
            int newState = switchedOn ?
                    INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS :
                    INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;
            final int userId = UserHandle.myUserId();
            boolean success = mPm.updateIntentVerificationStatus(mPackageName, newState, userId);
            if (success) {
                // read back the state to ensure canonicality
                newState = mPm.getIntentVerificationStatus(mPackageName, userId);
                ret = (newState == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS);
                pref.setChecked(ret);
            } else {
                Log.e(TAG, "Couldn't update intent verification status!");
            }
        }
        return ret;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_APP_LAUNCH;
    }
}
