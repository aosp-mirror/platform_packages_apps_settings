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

import static com.android.settings.notification.modes.ZenModeFragmentBase.MODE_ID;

import android.content.Context;
import android.os.Bundle;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.text.BidiFormatter;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Preference with a link and summary about what apps can break through the mode
 */
class ZenModeAppsLinkPreferenceController extends AbstractZenModePreferenceController {

    private static final String TAG = "ZenModeAppsLinkPreferenceController";

    private final ZenModeSummaryHelper mSummaryHelper;
    private ApplicationsState.Session mAppSession;
    private NotificationBackend mNotificationBackend = new NotificationBackend();
    private ZenMode mZenMode;
    private Preference mPreference;

    ZenModeAppsLinkPreferenceController(Context context, String key, Fragment host,
            ApplicationsState applicationsState, ZenModesBackend backend) {
        super(context, key, backend);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, mBackend);
        if (applicationsState != null && host != null) {
            mAppSession = applicationsState.newSession(mAppSessionCallbacks, host.getLifecycle());
        }
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        Bundle bundle = new Bundle();
        bundle.putString(MODE_ID, zenMode.getId());
        // TODO(b/332937635): Update metrics category
        preference.setIntent(new SubSettingLauncher(mContext)
                .setDestination(ZenModeAppsFragment.class.getName())
                .setSourceMetricsCategory(0)
                .setArguments(bundle)
                .toIntent());
        mZenMode = zenMode;
        mPreference = preference;
        triggerUpdateAppsBypassingDndSummaryText();
    }

    private void triggerUpdateAppsBypassingDndSummaryText() {
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

    private void updateAppsBypassingDndSummaryText(List<ApplicationsState.AppEntry> apps) {
        Set<String> appNames = getAppsBypassingDnd(apps);
        mPreference.setSummary(mSummaryHelper.getAppsSummary(mZenMode, appNames));
    }

    @VisibleForTesting
    ArraySet<String> getAppsBypassingDnd(@NonNull List<ApplicationsState.AppEntry> apps) {
        ArraySet<String> appsBypassingDnd = new ArraySet<>();

        Map<String, String> pkgLabelMap = new HashMap<String, String>();
        for (ApplicationsState.AppEntry entry : apps) {
            if (entry.info != null) {
                pkgLabelMap.put(entry.info.packageName, entry.label);
            }
        }
        for (String pkg : mNotificationBackend.getPackagesBypassingDnd(mContext.getUserId(),
                /* includeConversationChannels= */ false)) {
            // Settings may hide some packages from the user, so if they're not present here
            // we skip displaying them, even if they bypass dnd.
            if (pkgLabelMap.get(pkg) == null) {
                continue;
            }
            appsBypassingDnd.add(BidiFormatter.getInstance().unicodeWrap(pkgLabelMap.get(pkg)));
        }
        return appsBypassingDnd;
    }

    @VisibleForTesting final ApplicationsState.Callbacks mAppSessionCallbacks =
            new ApplicationsState.Callbacks() {

                @Override
                public void onRunningStateChanged(boolean running) { }

                @Override
                public void onPackageListChanged() {
                    triggerUpdateAppsBypassingDndSummaryText();
                }

                @Override
                public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                    updateAppsBypassingDndSummaryText(apps);
                }

                @Override
                public void onPackageIconChanged() { }

                @Override
                public void onPackageSizeChanged(String packageName) { }

                @Override
                public void onAllSizesComputed() { }

                @Override
                public void onLauncherInfoChanged() { }

                @Override
                public void onLoadEntriesCompleted() {
                    triggerUpdateAppsBypassingDndSummaryText();
                }
            };
}
