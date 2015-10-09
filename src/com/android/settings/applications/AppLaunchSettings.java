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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import com.android.settings.Utils;

import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;

import java.util.List;

public class AppLaunchSettings extends AppInfoWithHeader implements OnClickListener,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "AppLaunchSettings";

    private static final String KEY_APP_LINK_STATE = "app_link_state";
    private static final String KEY_SUPPORTED_DOMAIN_URLS = "app_launch_supported_domain_urls";
    private static final String KEY_CLEAR_DEFAULTS = "app_launch_clear_defaults";

    private static final Intent sBrowserIntent;
    static {
        sBrowserIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("http:"));
    }

    private PackageManager mPm;

    private boolean mIsBrowser;
    private boolean mHasDomainUrls;
    private DropDownPreference mAppLinkState;
    private AppDomainsPreference mAppDomainUrls;
    private ClearDefaultsPreference mClearDefaultsPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.installed_app_launch_settings);
        mAppDomainUrls = (AppDomainsPreference) findPreference(KEY_SUPPORTED_DOMAIN_URLS);
        mClearDefaultsPreference = (ClearDefaultsPreference) findPreference(KEY_CLEAR_DEFAULTS);
        mAppLinkState = (DropDownPreference) findPreference(KEY_APP_LINK_STATE);

        mPm = getActivity().getPackageManager();

        mIsBrowser = isBrowserApp(mPackageName);
        mHasDomainUrls =
                (mAppEntry.info.privateFlags & ApplicationInfo.PRIVATE_FLAG_HAS_DOMAIN_URLS) != 0;

        if (!mIsBrowser) {
            List<IntentFilterVerificationInfo> iviList = mPm.getIntentFilterVerifications(mPackageName);
            List<IntentFilter> filters = mPm.getAllIntentFilters(mPackageName);
            CharSequence[] entries = getEntries(mPackageName, iviList, filters);
            mAppDomainUrls.setTitles(entries);
            mAppDomainUrls.setValues(new int[entries.length]);
        }
        buildStateDropDown();
    }

    // An app is a "browser" if it has an activity resolution that wound up
    // marked with the 'handleAllWebDataURI' flag.
    private boolean isBrowserApp(String packageName) {
        sBrowserIntent.setPackage(packageName);
        List<ResolveInfo> list = mPm.queryIntentActivitiesAsUser(sBrowserIntent,
                PackageManager.MATCH_ALL, UserHandle.myUserId());
        final int count = list.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo != null && info.handleAllWebDataURI) {
                return true;
            }
        }
        return false;
    }

    private void buildStateDropDown() {
        if (mIsBrowser) {
            // Browsers don't show the app-link prefs
            mAppLinkState.setShouldDisableView(true);
            mAppLinkState.setEnabled(false);
            mAppDomainUrls.setShouldDisableView(true);
            mAppDomainUrls.setEnabled(false);
        } else {
            // Designed order of states in the dropdown:
            //
            // * always
            // * ask
            // * never
            mAppLinkState.addItem(R.string.app_link_open_always,
                    INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS);
            mAppLinkState.addItem(R.string.app_link_open_ask,
                    INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK);
            mAppLinkState.addItem(R.string.app_link_open_never,
                    INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER);

            mAppLinkState.setEnabled(mHasDomainUrls);
            if (mHasDomainUrls) {
                // Present 'undefined' as 'ask' because the OS treats them identically for
                // purposes of the UI (and does the right thing around pending domain
                // verifications that might arrive after the user chooses 'ask' in this UI).
                final int state = mPm.getIntentVerificationStatus(mPackageName, UserHandle.myUserId());
                mAppLinkState.setSelectedValue(
                        (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED)
                            ? INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK
                            : state);

                // Set the callback only after setting the initial selected item
                mAppLinkState.setCallback(new Callback() {
                    @Override
                    public boolean onItemSelected(int pos, Object value) {
                        return updateAppLinkState((Integer) value);
                    }
                });
            }
        }
    }

    private boolean updateAppLinkState(final int newState) {
        if (mIsBrowser) {
            // We shouldn't get into this state, but if we do make sure
            // not to cause any permanent mayhem.
            return false;
        }

        final int userId = UserHandle.myUserId();
        final int priorState = mPm.getIntentVerificationStatus(mPackageName, userId);

        if (priorState == newState) {
            return false;
        }

        boolean success = mPm.updateIntentVerificationStatus(mPackageName, newState, userId);
        if (success) {
            // Read back the state to see if the change worked
            final int updatedState = mPm.getIntentVerificationStatus(mPackageName, userId);
            success = (newState == updatedState);
        } else {
            Log.e(TAG, "Couldn't update intent verification status!");
        }
        return success;
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
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_APP_LAUNCH;
    }
}
