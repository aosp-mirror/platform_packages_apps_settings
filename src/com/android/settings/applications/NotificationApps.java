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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import com.android.settings.R;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.dashboard.SummaryLoader.SummaryProvider;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;

import java.util.ArrayList;

/**
 * Extension of ManageApplications with no changes other than having its own
 * SummaryProvider.
 */
public class NotificationApps extends ManageApplications {

    private static class SummaryProvider implements SummaryLoader.SummaryProvider,
            Callbacks, Callback {

        private final Context mContext;
        private final SummaryLoader mLoader;

        private final ApplicationsState mAppState;
        private final ApplicationsState.Session mSession;
        private final NotificationBackend mNotifBackend;
        private final AppStateNotificationBridge mExtraInfoBridge;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
            mAppState =
                    ApplicationsState.getInstance((Application) context.getApplicationContext());
            mSession = mAppState.newSession(this);
            mNotifBackend = new NotificationBackend();
            mExtraInfoBridge = new AppStateNotificationBridge(mContext.getPackageManager(),
                    mAppState, this, mNotifBackend);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                mSession.resume();
                mExtraInfoBridge.resume();
            } else {
                mSession.pause();
                mExtraInfoBridge.pause();
            }
        }

        private void updateSummary(ArrayList<AppEntry> apps) {
            if (apps == null) return;
            if (apps.size() == 0) {
                mLoader.setSummary(this, mContext.getString(R.string.notification_summary_none));
            } else {
                mLoader.setSummary(this, mContext.getString(R.string.notification_summary,
                        apps.size()));
            }
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            updateSummary(apps);
        }

        @Override
        public void onExtraInfoUpdated() {
            updateSummary(mSession.rebuild(
                    AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED,
                    ApplicationsState.ALPHA_COMPARATOR));
        }

        @Override
        public void onPackageListChanged() {
        }

        @Override
        public void onLauncherInfoChanged() {
        }

        @Override
        public void onLoadEntriesCompleted() {
        }

        @Override
        public void onRunningStateChanged(boolean running) {
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
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
}
