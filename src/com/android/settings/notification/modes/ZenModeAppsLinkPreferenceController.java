/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.ZenPolicy;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Preference with a link and summary about what apps can break through the mode
 */
class ZenModeAppsLinkPreferenceController extends AbstractZenModePreferenceController {

    private static final String TAG = "ZenModeAppsLinkPreferenceController";

    private final ZenModeSummaryHelper mSummaryHelper;
    private final ApplicationsState mApplicationsState;
    private final UserManager mUserManager;
    private ApplicationsState.Session mAppSession;
    private final ZenHelperBackend mHelperBackend;
    private ZenMode mZenMode;
    private CircularIconsPreference mPreference;
    private final Fragment mHost;

    ZenModeAppsLinkPreferenceController(Context context, String key, Fragment host,
            ZenModesBackend backend, ZenHelperBackend helperBackend) {
        this(context, key, host,
                ApplicationsState.getInstance((Application) context.getApplicationContext()),
                backend, helperBackend);
    }

    @VisibleForTesting
    ZenModeAppsLinkPreferenceController(Context context, String key, Fragment host,
            ApplicationsState applicationsState, ZenModesBackend backend,
            ZenHelperBackend helperBackend) {
        super(context, key, backend);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, helperBackend);
        mHelperBackend = helperBackend;
        mApplicationsState = applicationsState;
        mUserManager = context.getSystemService(UserManager.class);
        mHost = host;
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        return zenMode.getRule().getInterruptionFilter() != INTERRUPTION_FILTER_ALL;
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_AUTOMATIC_ZEN_RULE_ID, zenMode.getId());
        // TODO(b/332937635): Update metrics category
        preference.setIntent(new SubSettingLauncher(mContext)
                .setDestination(ZenModeAppsFragment.class.getName())
                .setSourceMetricsCategory(0)
                .setArguments(bundle)
                .toIntent());
        mZenMode = zenMode;
        mPreference = (CircularIconsPreference) preference;

        if (zenMode.getPolicy().getAllowedChannels() == ZenPolicy.CHANNEL_POLICY_NONE) {
            mPreference.setSummary(R.string.zen_mode_apps_none_apps);
            mPreference.displayIcons(CircularIconSet.EMPTY);
        } else {
            if (TextUtils.isEmpty(mPreference.getSummary())) {
                mPreference.setSummary(R.string.zen_mode_apps_calculating);
            }
            if (mApplicationsState != null && mHost != null) {
                mAppSession = mApplicationsState.newSession(mAppSessionCallbacks,
                        mHost.getLifecycle());
            }
            triggerUpdateAppsBypassingDnd();
        }
    }

    private void triggerUpdateAppsBypassingDnd() {
        if (mAppSession == null) {
            return;
        }

        ApplicationsState.AppFilter filter = android.multiuser.Flags.enablePrivateSpaceFeatures()
                && android.multiuser.Flags.handleInterleavedSettingsForPrivateSpace()
                ? ApplicationsState.FILTER_ENABLED_NOT_QUIET
                : ApplicationsState.FILTER_ALL_ENABLED;
        // We initiate a rebuild in the background here. Once the rebuild is completed,
        // the onRebuildComplete() callback will be invoked, which will trigger the summary text
        // to be initialized.
        mAppSession.rebuild(filter, ApplicationsState.ALPHA_COMPARATOR, false);
    }

    private void displayAppsBypassingDnd(List<AppEntry> allApps) {
        ImmutableList<AppEntry> apps = getAppsBypassingDndSortedByName(allApps);

        mPreference.setSummary(mSummaryHelper.getAppsSummary(mZenMode, apps));

        mPreference.displayIcons(new CircularIconSet<>(apps,
                app -> Utils.getBadgedIcon(mContext, app.info)));
    }

    @VisibleForTesting
    ImmutableList<AppEntry> getAppsBypassingDndSortedByName(@NonNull List<AppEntry> allApps) {
        Multimap<Integer, String> packagesBypassingDnd = HashMultimap.create();
        for (UserHandle userHandle : mUserManager.getUserProfiles()) {
            packagesBypassingDnd.putAll(userHandle.getIdentifier(),
                    mHelperBackend.getPackagesBypassingDnd(userHandle.getIdentifier(),
                            /* includeConversationChannels= */ false));
        }

        return ImmutableList.copyOf(
                allApps.stream()
                        .filter(app -> packagesBypassingDnd.containsEntry(
                                UserHandle.getUserId(app.info.uid), app.info.packageName))
                        .sorted(Comparator.comparing((AppEntry app) -> app.label)
                                .thenComparing(app -> UserHandle.getUserId(app.info.uid)))
                        .toList());
    }

    @VisibleForTesting
    final ApplicationsState.Callbacks mAppSessionCallbacks =
            new ApplicationsState.Callbacks() {

                @Override
                public void onRunningStateChanged(boolean running) {
                }

                @Override
                public void onPackageListChanged() {
                    triggerUpdateAppsBypassingDnd();
                }

                @Override
                public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                    displayAppsBypassingDnd(apps);
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
                    triggerUpdateAppsBypassingDnd();
                }
            };
}
