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

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.applications.PermissionsInfo;
import com.android.settingslib.applications.PermissionsInfo.PermissionGroup;

import java.util.List;

public class ManagePermissions extends SettingsPreferenceFragment
        implements PermissionsInfo.Callback {

    private static final String TAG = "ManagePermissions";

    private static final String OS_PKG = "android";

    private PermissionsInfo mPermissionsInfo;

    private PreferenceScreen mExtraScreen;

    @Override
    public void onResume() {
        super.onResume();

        mPermissionsInfo = new PermissionsInfo(getActivity(), this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        showLoadingWhenEmpty();
    }

    private void refreshUi() {
        Activity activity = getActivity();
        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            screen = getPreferenceManager().createPreferenceScreen(activity);
            setPreferenceScreen(screen);
        } else {
            screen.removeAll();
            if (mExtraScreen != null) {
                mExtraScreen.removeAll();
            }
        }
        final int count = screen.getPreferenceCount();
        if (count == 0) {
            final Preference extraScreenPreference = new Preference(activity);
            extraScreenPreference.setIcon(R.drawable.ic_toc);
            extraScreenPreference.setTitle(R.string.additional_permissions);
            List<PermissionGroup> groups = mPermissionsInfo.getGroups();
            for (PermissionGroup group : groups) {
                if (group.possibleApps.size() == 0) continue;
                PermissionPreference pref = new PermissionPreference(activity, group);
                pref.refreshUi();
                if (group.packageName.equals(OS_PKG)) {
                    screen.addPreference(pref);
                } else {
                    if (mExtraScreen == null) {
                        mExtraScreen = getPreferenceManager().createPreferenceScreen(activity);
                    }
                    mExtraScreen.addPreference(pref);
                }
            }
            if (mExtraScreen != null) {
                extraScreenPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AdditionalPermissionsFragment frag = new AdditionalPermissionsFragment();
                        frag.setTargetFragment(ManagePermissions.this, 0);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.main_content, frag);
                        ft.addToBackStack("AdditionalPerms");
                        ft.commit();
                        return true;
                    }
                });
                extraScreenPreference.setSummary(getString(R.string.additional_permissions_more,
                        mExtraScreen.getPreferenceCount()));
                screen.addPreference(extraScreenPreference);
            }
        } else {
            updatePrefs(screen);
            if (mExtraScreen != null) {
                updatePrefs(mExtraScreen);
            }
        }
    }

    private void updatePrefs(PreferenceScreen screen) {
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference pref = screen.getPreference(i);
            if (pref instanceof PermissionPreference) {
                ((PermissionPreference) pref).refreshUi();
            }
        }
    }

    @Override
    public void onPermissionLoadComplete() {
        refreshUi();
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
//            setSummary(getContext().getString(R.string.app_permissions_group_summary,
//                    mGroup.grantedApps.size(), mGroup.possibleApps.size()));
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

    public static class AdditionalPermissionsFragment extends SettingsPreferenceFragment {
        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            setPreferenceScreen(((ManagePermissions) getTargetFragment()).mExtraScreen);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            Resources resources = getResources();
            Theme theme = getActivity().getTheme();
            AppHeader.createAppHeader(this, resources.getDrawable(R.drawable.ic_toc, theme),
                    getString(R.string.additional_permissions), null, android.R.color.white);
        }

        @Override
        protected int getMetricsCategory() {
            return MetricsLogger.MANAGE_PERMISSIONS;
        }
    }

}
