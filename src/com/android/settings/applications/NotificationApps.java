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
import android.os.Handler;
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
        private final NotificationBackend mNotifBackend;
        private final Handler mHandler;
        private AppStateNotificationBridge mExtraInfoBridge;
        private ApplicationsState.Session mSession;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
            mAppState =
                    ApplicationsState.getInstance((Application) context.getApplicationContext());
            mNotifBackend = new NotificationBackend();
            mHandler = new Handler(mAppState.getBackgroundLooper());
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                mSession = mAppState.newSession(this);
                mExtraInfoBridge = new AppStateNotificationBridge(mContext,
                        mAppState, this, mNotifBackend);
                mSession.resume();
                mExtraInfoBridge.resume();
            } else {
                mSession.pause();
                mExtraInfoBridge.pause();
                mSession.release();
                mExtraInfoBridge.release();
            }
        }

        private void updateSummary(ArrayList<AppEntry> apps) {
            if (apps == null) return;
            if (apps.size() == 0) {
                mLoader.setSummary(this, mContext.getString(R.string.notification_summary_none));
            } else {
                mLoader.setSummary(this, mContext.getResources().getQuantityString(
                        R.plurals.notification_summary, apps.size(), apps.size()));
            }
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            updateSummary(apps);
        }

        @Override
        public void onExtraInfoUpdated() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateSummary(mSession.rebuild(
                            AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED,
                            null, false));
                }
            });
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
