/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.zenaccess;

import android.app.Flags;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;

import java.util.Set;

public class ZenAccessDetails extends AppInfoWithHeader implements
        ZenAccessSettingObserverMixin.Listener {

    private static final String SWITCH_PREF_KEY = "zen_access_switch";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_ACCESS_DETAIL;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_access_permission_details);
        requireActivity().setTitle(Flags.modesApi() && Flags.modesUi()
                ? R.string.manage_zen_modes_access_title
                : R.string.manage_zen_access_title);
        getSettingsLifecycle().addObserver(
                new ZenAccessSettingObserverMixin(getContext(), this /* listener */));
    }

    @Override
    protected boolean refreshUi() {
        final Context context = getContext();
        // don't show for managed profiles
        if (UserManager.get(context).isManagedProfile(context.getUserId())
            && !ZenAccessController.hasAccess(context, mPackageName)) {
            finish();
        }
        // If this app didn't declare this permission in their manifest, don't bother showing UI.
        final Set<String> needAccessApps =
                ZenAccessController.getPackagesRequestingNotificationPolicyAccess();
        if (needAccessApps.contains(mPackageName)) {
            updatePreference(context, findPreference(SWITCH_PREF_KEY));
        } else {
            finish();
        }
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    private void updatePreference(Context context, TwoStatePreference preference) {
        final CharSequence label = mPackageInfo.applicationInfo.loadLabel(mPm);
        final Set<String> autoApproved = ZenAccessController.getAutoApprovedPackages(context);
        if (autoApproved.contains(mPackageName)) {
            //Auto approved, user cannot do anything. Hard code summary and disable preference.
            preference.setEnabled(false);
            preference.setSummary(getString(R.string.zen_access_disabled_package_warning));
            return;
        }
        preference.setTitle(Flags.modesApi() && Flags.modesUi()
                ? R.string.zen_modes_access_detail_switch
                : R.string.zen_access_detail_switch);
        preference.setChecked(ZenAccessController.hasAccess(context, mPackageName));
        preference.setOnPreferenceChangeListener((p, newValue) -> {
            final boolean access = (Boolean) newValue;
            if (access) {
                new ScaryWarningDialogFragment()
                        .setPkgInfo(mPackageName, label, ZenAccessDetails.this)
                        .show(getFragmentManager(), "dialog");
            } else {
                new FriendlyWarningDialogFragment()
                        .setPkgInfo(mPackageName, label, ZenAccessDetails.this)
                        .show(getFragmentManager(), "dialog");
            }
            return false;
        });
    }

    @Override
    public void onZenAccessPolicyChanged() {
        refreshUi();
    }
}
