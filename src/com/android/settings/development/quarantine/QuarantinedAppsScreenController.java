/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.quarantine;

import android.app.Application;
import android.content.Context;
import android.content.pm.Flags;
import android.content.pm.PackageManager;
import android.content.pm.SuspendDialogInfo;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class QuarantinedAppsScreenController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, OnDestroy,
        ApplicationsState.Callbacks, Preference.OnPreferenceChangeListener,
        AppStateBaseBridge.Callback {
    private final ApplicationsState mApplicationsState;
    private final QuarantinedAppStateBridge mQuarantinedAppStateBridge;
    private ApplicationsState.Session mSession;
    private PreferenceScreen mScreen;
    private AppFilter mFilter;
    private boolean mExtraLoaded;

    public QuarantinedAppsScreenController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) context.getApplicationContext());
        mQuarantinedAppStateBridge = new QuarantinedAppStateBridge(context,
                mApplicationsState, this);
    }

    @Override
    public void onStart() {
        mQuarantinedAppStateBridge.resume(true /* forceLoadAllApps */);
    }

    @Override
    public void onStop() {
        mQuarantinedAppStateBridge.pause();
    }

    @Override
    public void onDestroy() {
        mQuarantinedAppStateBridge.release();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }

    public void setFilter(AppFilter filter) {
        mFilter = filter;
    }

    public void setSession(Lifecycle lifecycle) {
        mSession = mApplicationsState.newSession(this, lifecycle);
    }

    @Override
    public void onExtraInfoUpdated() {
        mExtraLoaded = true;
        rebuild();
    }

    public void rebuild() {
        if (!mExtraLoaded || mSession == null) {
            return;
        }

        final ArrayList<AppEntry> apps = mSession.rebuild(mFilter,
                ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            onRebuildComplete(apps);
        }
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        if (apps == null) {
            return;
        }

        // Preload top visible icons of app list.
        AppUtils.preloadTopIcons(mContext, apps,
                mContext.getResources().getInteger(R.integer.config_num_visible_app_icons));

        // Create apps key set for removing useless preferences
        final Set<String> appsKeySet = new TreeSet<>();
        // Add or update preferences
        final int count = apps.size();
        for (int i = 0; i < count; i++) {
            final AppEntry entry = apps.get(i);
            if (!shouldAddPreference(entry)) {
                continue;
            }
            final String prefkey = QuarantinedAppPreference.generateKey(entry);
            appsKeySet.add(prefkey);
            QuarantinedAppPreference preference = mScreen.findPreference(prefkey);
            if (preference == null) {
                preference = new QuarantinedAppPreference(mScreen.getContext(), entry);
                preference.setOnPreferenceChangeListener(this);
                mScreen.addPreference(preference);
            } else {
                preference.updateState();
            }
            preference.setOrder(i);
        }

        // Remove useless preferences
        removeUselessPrefs(appsKeySet);
    }

    private void removeUselessPrefs(final Set<String> appsKeySet) {
        final int prefCount = mScreen.getPreferenceCount();
        String prefKey;
        if (prefCount > 0) {
            for (int i = prefCount - 1; i >= 0; i--) {
                final Preference pref = mScreen.getPreference(i);
                prefKey = pref.getKey();
                if (!appsKeySet.isEmpty() && appsKeySet.contains(prefKey)) {
                    continue;
                }
                mScreen.removePreference(pref);
            }
        }
    }

    @VisibleForTesting
    static boolean shouldAddPreference(AppEntry app) {
        return app != null && UserHandle.isApp(app.info.uid);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof QuarantinedAppPreference) {
            final QuarantinedAppPreference quarantinedPreference =
                    (QuarantinedAppPreference) preference;
            final boolean quarantined = newValue == Boolean.TRUE;
            setPackageQuarantined(quarantinedPreference.getEntry().info.packageName,
                    quarantinedPreference.getEntry().info.uid, quarantined);
            quarantinedPreference.getEntry().extraInfo = quarantined;
            return true;
        }
        return false;
    }

    private void setPackageQuarantined(String pkg, int uid, boolean quarantined) {
        final PackageManager pm = mContext.createContextAsUser(
                UserHandle.getUserHandleForUid(uid), 0).getPackageManager();
        final SuspendDialogInfo dialogInfo;
        if (quarantined) {
            dialogInfo = new SuspendDialogInfo.Builder()
                    .setNeutralButtonText(R.string.unquarantine_app_button)
                    .setNeutralButtonAction(SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
                    .build();
        } else {
            dialogInfo = null;
        }
        pm.setPackagesSuspended(new String[] {pkg}, quarantined, null /* appExtras */,
                null /* launcherExtras */, dialogInfo,
                PackageManager.FLAG_SUSPEND_QUARANTINED);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.quarantinedEnabled() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
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
    }
}
