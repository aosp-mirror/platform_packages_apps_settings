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

package com.android.settings.notification;

import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.notification.NotifyingApp;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This controller displays a list of recently used apps and a "See all" button. If there is
 * no recently used app, "See all" will be displayed as "Notifications".
 */
public class RecentNotifyingAppsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "RecentNotisCtrl";
    private static final String KEY_PREF_CATEGORY = "recent_notifications_category";
    @VisibleForTesting
    static final String KEY_DIVIDER = "all_notifications_divider";
    @VisibleForTesting
    static final String KEY_SEE_ALL = "all_notifications";
    private static final int SHOW_RECENT_APP_COUNT = 5;
    private static final Set<String> SKIP_SYSTEM_PACKAGES = new ArraySet<>();

    private final Fragment mHost;
    private final PackageManager mPm;
    private final NotificationBackend mNotificationBackend;
    private final int mUserId;
    private final IconDrawableFactory mIconDrawableFactory;

    private List<NotifyingApp> mApps;
    private final ApplicationsState mApplicationsState;

    private PreferenceCategory mCategory;
    private Preference mSeeAllPref;
    private Preference mDivider;

    public RecentNotifyingAppsPreferenceController(Context context, NotificationBackend backend,
            Application app, Fragment host) {
        this(context, backend, app == null ? null : ApplicationsState.getInstance(app), host);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    RecentNotifyingAppsPreferenceController(Context context, NotificationBackend backend,
            ApplicationsState appState, Fragment host) {
        super(context);
        mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        mUserId = UserHandle.myUserId();
        mPm = context.getPackageManager();
        mHost = host;
        mApplicationsState = appState;
        mNotificationBackend = backend;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PREF_CATEGORY;
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        PreferenceControllerMixin.super.updateNonIndexableKeys(keys);
        // Don't index category name into search. It's not actionable.
        keys.add(KEY_PREF_CATEGORY);
        keys.add(KEY_DIVIDER);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mCategory = screen.findPreference(getPreferenceKey());
        mSeeAllPref = screen.findPreference(KEY_SEE_ALL);
        mDivider = screen.findPreference(KEY_DIVIDER);
        super.displayPreference(screen);
        refreshUi(mCategory.getContext());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshUi(mCategory.getContext());
        mSeeAllPref.setTitle(mContext.getString(R.string.recent_notifications_see_all_title));
    }

    @VisibleForTesting
    void refreshUi(Context prefContext) {
        reloadData();
        final List<NotifyingApp> recentApps = getDisplayableRecentAppList();
        if (recentApps != null && !recentApps.isEmpty()) {
            displayRecentApps(prefContext, recentApps);
        } else {
            displayOnlyAllAppsLink();
        }
    }

    @VisibleForTesting
    void reloadData() {
        mApps = mNotificationBackend.getRecentApps();
    }

    private void displayOnlyAllAppsLink() {
        mCategory.setTitle(null);
        mDivider.setVisible(false);
        mSeeAllPref.setTitle(R.string.notifications_title);
        mSeeAllPref.setIcon(null);
        int prefCount = mCategory.getPreferenceCount();
        for (int i = prefCount - 1; i >= 0; i--) {
            final Preference pref = mCategory.getPreference(i);
            if (!TextUtils.equals(pref.getKey(), KEY_SEE_ALL)) {
                mCategory.removePreference(pref);
            }
        }
    }

    private void displayRecentApps(Context prefContext, List<NotifyingApp> recentApps) {
        mCategory.setTitle(R.string.recent_notifications);
        mDivider.setVisible(true);
        mSeeAllPref.setSummary(null);
        mSeeAllPref.setIcon(R.drawable.ic_chevron_right_24dp);

        // Rebind prefs/avoid adding new prefs if possible. Adding/removing prefs causes jank.
        // Build a cached preference pool
        final Map<String, NotificationAppPreference> appPreferences = new ArrayMap<>();
        int prefCount = mCategory.getPreferenceCount();
        for (int i = 0; i < prefCount; i++) {
            final Preference pref = mCategory.getPreference(i);
            final String key = pref.getKey();
            if (!TextUtils.equals(key, KEY_SEE_ALL)) {
                appPreferences.put(key, (NotificationAppPreference) pref);
            }
        }
        final int recentAppsCount = recentApps.size();
        for (int i = 0; i < recentAppsCount; i++) {
            final NotifyingApp app = recentApps.get(i);
            // Bind recent apps to existing prefs if possible, or create a new pref.
            final String pkgName = app.getPackage();
            final ApplicationsState.AppEntry appEntry =
                    mApplicationsState.getEntry(app.getPackage(), mUserId);
            if (appEntry == null) {
                continue;
            }

            boolean rebindPref = true;
            NotificationAppPreference pref = appPreferences.remove(pkgName);
            if (pref == null) {
                pref = new NotificationAppPreference(prefContext);
                rebindPref = false;
            }
            pref.setKey(pkgName);
            pref.setTitle(appEntry.label);
            pref.setIcon(mIconDrawableFactory.getBadgedIcon(appEntry.info));
            pref.setIconSize(TwoTargetPreference.ICON_SIZE_SMALL);
            pref.setSummary(StringUtil.formatRelativeTime(mContext,
                    System.currentTimeMillis() - app.getLastNotified(), true));
            pref.setOrder(i);
            Bundle args = new Bundle();
            args.putString(AppInfoBase.ARG_PACKAGE_NAME, pkgName);
            args.putInt(AppInfoBase.ARG_PACKAGE_UID, appEntry.info.uid);

            pref.setIntent(new SubSettingLauncher(mHost.getActivity())
                    .setDestination(AppNotificationSettings.class.getName())
                    .setTitleRes(R.string.notifications_title)
                    .setArguments(args)
                    .setSourceMetricsCategory(
                            SettingsEnums.MANAGE_APPLICATIONS_NOTIFICATIONS)
                    .toIntent());
            pref.setSwitchEnabled(mNotificationBackend.isBlockable(mContext, appEntry.info));
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean blocked = !(Boolean) newValue;
                mNotificationBackend.setNotificationsEnabledForPackage(
                        pkgName, appEntry.info.uid, !blocked);
                return true;
            });
            pref.setChecked(
                    !mNotificationBackend.getNotificationsBanned(pkgName, appEntry.info.uid));

            if (!rebindPref) {
                mCategory.addPreference(pref);
            }
        }
        // Remove unused prefs from pref cache pool
        for (Preference unusedPrefs : appPreferences.values()) {
            mCategory.removePreference(unusedPrefs);
        }
    }

    private List<NotifyingApp> getDisplayableRecentAppList() {
        Collections.sort(mApps);
        List<NotifyingApp> displayableApps = new ArrayList<>(SHOW_RECENT_APP_COUNT);
        int count = 0;
        for (NotifyingApp app : mApps) {
            final ApplicationsState.AppEntry appEntry = mApplicationsState.getEntry(
                    app.getPackage(), mUserId);
            if (appEntry == null) {
                continue;
            }
            if (!shouldIncludePkgInRecents(app.getPackage())) {
                continue;
            }
            displayableApps.add(app);
            count++;
            if (count >= SHOW_RECENT_APP_COUNT) {
                break;
            }
        }
        return displayableApps;
    }


    /**
     * Whether or not the app should be included in recent list.
     */
    private boolean shouldIncludePkgInRecents(String pkgName) {
        final Intent launchIntent = new Intent().addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(pkgName);

        if (mPm.resolveActivity(launchIntent, 0) == null) {
            // Not visible on launcher -> likely not a user visible app, skip if non-instant.
            final ApplicationsState.AppEntry appEntry =
                    mApplicationsState.getEntry(pkgName, mUserId);
            if (appEntry == null || appEntry.info == null || !AppUtils.isInstant(appEntry.info)) {
                Log.d(TAG, "Not a user visible or instant app, skipping " + pkgName);
                return false;
            }
        }
        return true;
    }
}
