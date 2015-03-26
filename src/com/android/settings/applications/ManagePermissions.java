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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.PermissionsInfo.PermissionGroup;

import java.util.List;

public class ManagePermissions extends SettingsPreferenceFragment
        implements PermissionsInfo.Callback, OnPreferenceClickListener {

    private static final String TAG = "ManagePermissions";

    private boolean mLoadComplete;
    private PermissionsInfo mPermissionsInfo;

    @Override
    public void onResume() {
        super.onResume();

        mPermissionsInfo = new PermissionsInfo(getActivity(), this);
    }

    private void refreshUi() {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            screen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(screen);
        } else {
            screen.removeAll();
        }
        final int count = screen.getPreferenceCount();
        if (count == 0) {
            List<PermissionGroup> groups = mPermissionsInfo.getGroups();
            for (PermissionGroup group : groups) {
                if (group.possibleApps.size() == 0) continue;
                PermissionPreference pref = new PermissionPreference(getActivity(), group);
                pref.refreshUi();
                screen.addPreference(pref);
            }
        } else {
            for (int i = 0; i < count; i++) {
                ((PermissionPreference) screen.getPreference(i)).refreshUi();
            }
        }
    }

    @Override
    public void onPermissionLoadComplete() {
        refreshUi();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.MANAGE_PERMISSIONS;
    }

    private class PermissionPreference extends Preference implements OnPreferenceClickListener {
        private final PermissionGroup mGroup;

        public PermissionPreference(Context context, PermissionGroup group) {
            super(context);
            setOnPreferenceClickListener(this);
            mGroup = group;
        }

        public void refreshUi() {
            setTitle(mGroup.label);
            setIcon(mGroup.icon);
            setSummary(getContext().getString(R.string.app_permissions_group_summary,
                    mGroup.grantedApps.size(), mGroup.possibleApps.size()));
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent i = new Intent(Intent.ACTION_MANAGE_PERMISSION_APPS)
                    .putExtra(Intent.EXTRA_PERMISSION_NAME, mGroup.name);
            try {
                getActivity().startActivity(i);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "No app to handle " + i.getAction());
            }
            return true;
        }
    }

}
