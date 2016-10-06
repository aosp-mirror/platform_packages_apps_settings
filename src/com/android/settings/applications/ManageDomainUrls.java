/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.ArraySet;
import android.view.View;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;

/**
 * Activity to manage how Android handles URL resolution. Includes both per-app
 * handling as well as system handling for Web Actions.
 */
public class ManageDomainUrls extends SettingsPreferenceFragment
        implements ApplicationsState.Callbacks, OnPreferenceChangeListener,
        OnPreferenceClickListener {

    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;

    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private PreferenceGroup mDomainAppList;
    private SwitchPreference mWebAction;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        mApplicationsState = ApplicationsState.getInstance(
                (Application) getContext().getApplicationContext());
        mSession = mApplicationsState.newSession(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSession.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.release();
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        if (getContext() == null) {
            return;
        }

        final boolean disableWebActions = Global.getInt(getContext().getContentResolver(),
                Global.ENABLE_EPHEMERAL_FEATURE, 1) == 0;
        if (disableWebActions) {
            mDomainAppList = getPreferenceScreen();
        } else {
            final PreferenceGroup preferenceScreen = getPreferenceScreen();
            if (preferenceScreen.getPreferenceCount() == 0) {
                // add preferences
                final PreferenceCategory webActionCategory =
                        new PreferenceCategory(getPrefContext());
                webActionCategory.setTitle(R.string.web_action_section_title);
                preferenceScreen.addPreference(webActionCategory);

                // toggle to enable / disable Web Actions [aka Instant Apps]
                mWebAction = new SwitchPreference(getPrefContext());
                mWebAction.setTitle(R.string.web_action_enable_title);
                mWebAction.setSummary(R.string.web_action_enable_summary);
                mWebAction.setChecked(Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.WEB_ACTION_ENABLED, 1) != 0);
                mWebAction.setOnPreferenceChangeListener(this);
                webActionCategory.addPreference(mWebAction);

                // list to manage link handling per app
                mDomainAppList = new PreferenceCategory(getPrefContext());
                mDomainAppList.setTitle(R.string.domain_url_section_title);
                preferenceScreen.addPreference(mDomainAppList);
            }
        }
        rebuildAppList(mDomainAppList, apps);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWebAction) {
            final int enabled = (boolean) newValue ? 1 : 0;
            Settings.Secure.putInt(
                    getContentResolver(), Settings.Secure.WEB_ACTION_ENABLED, enabled);
            return true;
        }
        return false;
    }

    private void rebuild() {
        final ArrayList<AppEntry> apps = mSession.rebuild(
                ApplicationsState.FILTER_WITH_DOMAIN_URLS, ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            onRebuildComplete(apps);
        }
    }

    private void rebuildAppList(PreferenceGroup group, ArrayList<AppEntry> apps) {
        cacheRemoveAllPrefs(group);
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry entry = apps.get(i);
            String key = entry.info.packageName + "|" + entry.info.uid;
            DomainAppPreference preference = (DomainAppPreference) getCachedPreference(key);
            if (preference == null) {
                preference = new DomainAppPreference(getPrefContext(), entry);
                preference.setKey(key);
                preference.setOnPreferenceClickListener(this);
                group.addPreference(preference);
            } else {
                preference.reuse();
            }
            preference.setOrder(i);
        }
        removeCachedPrefs(group);
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
        rebuild();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.MANAGE_DOMAIN_URLS;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getClass() == DomainAppPreference.class) {
            ApplicationsState.AppEntry entry = ((DomainAppPreference) preference).mEntry;
            AppInfoBase.startAppInfoFragment(AppLaunchSettings.class, R.string.auto_launch_label,
                    entry.info.packageName, entry.info.uid, this,
                    INSTALLED_APP_DETAILS);
            return true;
        }
        return false;
    }

    private class DomainAppPreference extends Preference {
        private final AppEntry mEntry;
        private final PackageManager mPm;

        public DomainAppPreference(final Context context, AppEntry entry) {
            super(context);
            mPm = context.getPackageManager();
            mEntry = entry;
            mEntry.ensureLabel(getContext());
            setState();
            if (mEntry.icon != null) {
                setIcon(mEntry.icon);
            }
        }

        private void setState() {
            setTitle(mEntry.label);
            setSummary(getDomainsSummary(mEntry.info.packageName));
        }

        public void reuse() {
            setState();
            notifyChanged();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            if (mEntry.icon == null) {
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Ensure we have an icon before binding.
                        mApplicationsState.ensureIcon(mEntry);
                        // This might trigger us to bind again, but it gives an easy way to only
                        // load the icon once its needed, so its probably worth it.
                        setIcon(mEntry.icon);
                    }
                });
            }
            super.onBindViewHolder(holder);
        }

        private CharSequence getDomainsSummary(String packageName) {
            // If the user has explicitly said "no" for this package, that's the
            // string we should show.
            int domainStatus =
                    mPm.getIntentVerificationStatusAsUser(packageName, UserHandle.myUserId());
            if (domainStatus == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER) {
                return getContext().getString(R.string.domain_urls_summary_none);
            }
            // Otherwise, ask package manager for the domains for this package,
            // and show the first one (or none if there aren't any).
            ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
            if (result.size() == 0) {
                return getContext().getString(R.string.domain_urls_summary_none);
            } else if (result.size() == 1) {
                return getContext().getString(R.string.domain_urls_summary_one, result.valueAt(0));
            } else {
                return getContext().getString(R.string.domain_urls_summary_some, result.valueAt(0));
            }
        }
    }
}
