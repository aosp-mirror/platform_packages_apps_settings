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

package com.android.settings.notification.zen;

import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.core.text.BidiFormatter;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.app.AppChannelsBypassingDndSettings;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.apppreference.AppPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Adds a preference to the PreferenceScreen for each notification channel that can bypass DND.
 */
public class ZenModeAllBypassingAppsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {
    public static final String KEY_NO_APPS = getKey("none");
    private static final String KEY = "zen_mode_bypassing_apps_list";

    private final NotificationBackend mNotificationBackend;

    @VisibleForTesting ApplicationsState mApplicationsState;
    @VisibleForTesting PreferenceCategory mPreferenceCategory;
    @VisibleForTesting Context mPrefContext;

    private ApplicationsState.Session mAppSession;
    private Fragment mHostFragment;

    public ZenModeAllBypassingAppsPreferenceController(Context context, Application app,
            Fragment host, NotificationBackend notificationBackend) {
        this(context, app == null ? null : ApplicationsState.getInstance(app), host,
                notificationBackend);
    }

    private ZenModeAllBypassingAppsPreferenceController(Context context, ApplicationsState appState,
            Fragment host, NotificationBackend notificationBackend) {
        super(context);
        mNotificationBackend = notificationBackend;
        mApplicationsState = appState;
        mHostFragment = host;

        if (mApplicationsState != null && host != null) {
            mAppSession = mApplicationsState.newSession(mAppSessionCallbacks, host.getLifecycle());
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(KEY);
        mPrefContext = screen.getContext();
        updateAppList();
        super.displayPreference(screen);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /**
     * Call this method to trigger the app list to refresh.
     */
    public void updateAppList() {
        if (mAppSession == null) {
            return;
        }

        ApplicationsState.AppFilter filter = ApplicationsState.FILTER_ALL_ENABLED;
        List<ApplicationsState.AppEntry> apps = mAppSession.rebuild(filter,
                ApplicationsState.ALPHA_COMPARATOR);
        updateAppList(apps);
    }

    @VisibleForTesting
    void updateAppList(List<ApplicationsState.AppEntry> apps) {
        if (mPreferenceCategory == null || apps == null) {
            return;
        }

        List<Preference> appsBypassingDnd = new ArrayList<>();
        for (ApplicationsState.AppEntry app : apps) {
            String pkg = app.info.packageName;
            mApplicationsState.ensureIcon(app);
            final int appChannels = mNotificationBackend.getChannelCount(pkg, app.info.uid);
            final int appChannelsBypassingDnd = mNotificationBackend
                    .getNotificationChannelsBypassingDnd(pkg, app.info.uid).getList().size();
            if (appChannelsBypassingDnd > 0) {
                final String key = getKey(pkg);
                // re-use previously created preference when possible
                Preference pref = mPreferenceCategory.findPreference(key);
                if (pref == null) {
                    pref = new AppPreference(mPrefContext);
                    pref.setKey(key);
                    pref.setOnPreferenceClickListener(preference -> {
                        Bundle args = new Bundle();
                        args.putString(AppInfoBase.ARG_PACKAGE_NAME, app.info.packageName);
                        args.putInt(AppInfoBase.ARG_PACKAGE_UID, app.info.uid);
                        new SubSettingLauncher(mContext)
                                .setDestination(AppChannelsBypassingDndSettings.class.getName())
                                .setArguments(args)
                                .setUserHandle(UserHandle.getUserHandleForUid(app.info.uid))
                                .setResultListener(mHostFragment, 0)
                                .setSourceMetricsCategory(
                                        SettingsEnums.NOTIFICATION_ZEN_MODE_OVERRIDING_APP)
                                .launch();
                        return true;
                    });
                }
                pref.setTitle(BidiFormatter.getInstance().unicodeWrap(app.label));
                pref.setIcon(app.icon);
                if (appChannels > appChannelsBypassingDnd) {
                    pref.setSummary(R.string.zen_mode_bypassing_apps_summary_some);
                } else {
                    pref.setSummary(R.string.zen_mode_bypassing_apps_summary_all);
                }

                appsBypassingDnd.add(pref);
            }
        }

        if (appsBypassingDnd.size() == 0) {
            Preference pref = mPreferenceCategory.findPreference(KEY_NO_APPS);
            if (pref == null) {
                pref = new Preference(mPrefContext);
                pref.setKey(KEY_NO_APPS);
                pref.setTitle(R.string.zen_mode_bypassing_apps_none);
            }
            appsBypassingDnd.add(pref);
        }

        if (hasAppListChanged(appsBypassingDnd, mPreferenceCategory)) {
            mPreferenceCategory.removeAll();
            for (Preference prefToAdd : appsBypassingDnd) {
                mPreferenceCategory.addPreference(prefToAdd);
            }
        }
    }

    static boolean hasAppListChanged(List<Preference> newAppPreferences,
            PreferenceCategory preferenceCategory) {
        if (newAppPreferences.size() != preferenceCategory.getPreferenceCount()) {
            return true;
        }

        for (int i = 0; i < newAppPreferences.size(); i++) {
            Preference newAppPref = newAppPreferences.get(i);
            Preference pref = preferenceCategory.getPreference(i);
            if (!Objects.equals(newAppPref.getKey(), pref.getKey())) {
                return true;
            }
        }
        return false;

    }

    /**
     * Create a unique key to idenfity an AppPreference
     */
    static String getKey(String pkg) {
        return pkg;
    }

    private final ApplicationsState.Callbacks mAppSessionCallbacks =
            new ApplicationsState.Callbacks() {

                @Override
                public void onRunningStateChanged(boolean running) {
                    updateAppList();
                }

                @Override
                public void onPackageListChanged() {
                    updateAppList();
                }

                @Override
                public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                    updateAppList(apps);
                }

                @Override
                public void onPackageIconChanged() {
                    updateAppList();
                }

                @Override
                public void onPackageSizeChanged(String packageName) {
                    updateAppList();
                }

                @Override
                public void onAllSizesComputed() { }

                @Override
                public void onLauncherInfoChanged() {
                    updateAppList();
                }

                @Override
                public void onLoadEntriesCompleted() {
                    updateAppList();
                }
            };
}
