/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
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
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.AppPreference;

import java.util.ArrayList;
import java.util.List;


/**
 * When clicked, populates the PreferenceScreen with apps that aren't already bypassing DND. The
 * user can click on these Preferences to allow notification channels from the app to bypass DND.
 */
public class ZenModeAddBypassingAppsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    public static final String KEY_NO_APPS = "add_none";
    private static final String KEY = "zen_mode_non_bypassing_apps_list";
    private static final String KEY_ADD = "zen_mode_bypassing_apps_add";
    private final NotificationBackend mNotificationBackend;

    @VisibleForTesting ApplicationsState mApplicationsState;
    @VisibleForTesting PreferenceScreen mPreferenceScreen;
    @VisibleForTesting PreferenceCategory mPreferenceCategory;
    @VisibleForTesting Context mPrefContext;

    private Preference mAddPreference;
    private ApplicationsState.Session mAppSession;
    private Fragment mHostFragment;

    public ZenModeAddBypassingAppsPreferenceController(Context context, Application app,
            Fragment host, NotificationBackend notificationBackend) {
        this(context, app == null ? null : ApplicationsState.getInstance(app), host,
                notificationBackend);
    }

    private ZenModeAddBypassingAppsPreferenceController(Context context, ApplicationsState appState,
            Fragment host, NotificationBackend notificationBackend) {
        super(context);
        mNotificationBackend = notificationBackend;
        mApplicationsState = appState;
        mHostFragment = host;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceScreen = screen;
        mAddPreference = screen.findPreference(KEY_ADD);
        mAddPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mAddPreference.setVisible(false);
                if (mApplicationsState != null && mHostFragment != null) {
                    mAppSession = mApplicationsState.newSession(mAppSessionCallbacks,
                            mHostFragment.getLifecycle());
                }
                return true;
            }
        });
        mPrefContext = screen.getContext();
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
        mAppSession.rebuild(filter, ApplicationsState.ALPHA_COMPARATOR);
    }

    // Set the icon for the given preference to the entry icon from cache if available, or look
    // it up.
    private void updateIcon(Preference pref, ApplicationsState.AppEntry entry) {
        synchronized (entry) {
            final Drawable cachedIcon = AppUtils.getIconFromCache(entry);
            if (cachedIcon != null && entry.mounted) {
                pref.setIcon(cachedIcon);
            } else {
                ThreadUtils.postOnBackgroundThread(() -> {
                    final Drawable icon = AppUtils.getIcon(mPrefContext, entry);
                    if (icon != null) {
                        ThreadUtils.postOnMainThread(() -> pref.setIcon(icon));
                    }
                });
            }
        }
    }

    @VisibleForTesting
    void updateAppList(List<ApplicationsState.AppEntry> apps) {
        if (apps == null) {
            return;
        }

        if (mPreferenceCategory == null) {
            mPreferenceCategory = new PreferenceCategory(mPrefContext);
            mPreferenceCategory.setTitle(R.string.zen_mode_bypassing_apps_add_header);
            mPreferenceScreen.addPreference(mPreferenceCategory);
        }

        boolean doAnyAppsPassCriteria = false;
        for (ApplicationsState.AppEntry app : apps) {
            String pkg = app.info.packageName;
            final String key = getKey(pkg, app.info.uid);
            final int appChannels = mNotificationBackend.getChannelCount(pkg, app.info.uid);
            final int appChannelsBypassingDnd = mNotificationBackend
                    .getNotificationChannelsBypassingDnd(pkg, app.info.uid).getList().size();
            if (appChannelsBypassingDnd == 0 && appChannels > 0) {
                doAnyAppsPassCriteria = true;
            }

            Preference pref = mPreferenceCategory.findPreference(key);

            if (pref == null) {
                if (appChannelsBypassingDnd == 0 && appChannels > 0) {
                    // does not exist but should
                    pref = new AppPreference(mPrefContext);
                    pref.setKey(key);
                    pref.setOnPreferenceClickListener(preference -> {
                        Bundle args = new Bundle();
                        args.putString(AppInfoBase.ARG_PACKAGE_NAME, app.info.packageName);
                        args.putInt(AppInfoBase.ARG_PACKAGE_UID, app.info.uid);
                        new SubSettingLauncher(mContext)
                                .setDestination(AppChannelsBypassingDndSettings.class.getName())
                                .setArguments(args)
                                .setResultListener(mHostFragment, 0)
                                .setUserHandle(new UserHandle(UserHandle.getUserId(app.info.uid)))
                                .setSourceMetricsCategory(
                                        SettingsEnums.NOTIFICATION_ZEN_MODE_OVERRIDING_APP)
                                .launch();
                        return true;
                    });
                    pref.setTitle(BidiFormatter.getInstance().unicodeWrap(app.label));
                    updateIcon(pref, app);
                    mPreferenceCategory.addPreference(pref);
                }
            } else if (appChannelsBypassingDnd != 0 || appChannels == 0) {
                // exists but shouldn't anymore
                mPreferenceCategory.removePreference(pref);
            }
        }

        Preference pref = mPreferenceCategory.findPreference(KEY_NO_APPS);
        if (!doAnyAppsPassCriteria) {
            if (pref == null) {
                pref = new Preference(mPrefContext);
                pref.setKey(KEY_NO_APPS);
                pref.setTitle(R.string.zen_mode_bypassing_apps_none);
            }
            mPreferenceCategory.addPreference(pref);
        } else if (pref != null) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    static String getKey(String pkg, int uid) {
        return "add|" + pkg + "|" + uid;
    }

    private final ApplicationsState.Callbacks mAppSessionCallbacks =
            new ApplicationsState.Callbacks() {

                @Override
                public void onRunningStateChanged(boolean running) {

                }

                @Override
                public void onPackageListChanged() {

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

                }

                @Override
                public void onAllSizesComputed() { }

                @Override
                public void onLauncherInfoChanged() {

                }

                @Override
                public void onLoadEntriesCompleted() {
                    updateAppList();
                }
            };
}
