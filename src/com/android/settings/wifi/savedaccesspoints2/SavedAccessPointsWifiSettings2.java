/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints2;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.NetworkProviderSettings;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.wifitrackerlib.SavedNetworkTracker;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * UI to manage saved networks/access points.
 */
public class SavedAccessPointsWifiSettings2 extends DashboardFragment
        implements SavedNetworkTracker.SavedNetworkTrackerCallback {

    @VisibleForTesting static final String TAG = "SavedAccessPoints2";

    // Max age of tracked WifiEntries
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating SavedNetworkTracker scans
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    @VisibleForTesting SavedNetworkTracker mSavedNetworkTracker;
    @VisibleForTesting HandlerThread mWorkerThread;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_SAVED_ACCESS_POINTS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_display_saved_access_points2;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(SavedAccessPointsPreferenceController2.class).setHost(this);
        use(SubscribedAccessPointsPreferenceController2.class).setHost(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();
        mWorkerThread = new HandlerThread(TAG
                + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        mSavedNetworkTracker = new SavedNetworkTracker(getSettingsLifecycle(), context,
                context.getSystemService(WifiManager.class),
                context.getSystemService(ConnectivityManager.class),
                new Handler(Looper.getMainLooper()),
                mWorkerThread.getThreadHandler(),
                elapsedRealtimeClock,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                this);
    }

    @Override
    public void onStart() {
        super.onStart();

        onSavedWifiEntriesChanged();
        onSubscriptionWifiEntriesChanged();
    }

    @Override
    public void onDestroy() {
        mWorkerThread.quit();

        super.onDestroy();
    }

    /**
     * Shows {@link WifiNetworkDetailsFragment} for assigned key of {@link WifiEntry}.
     */
    public void showWifiPage(@NonNull String key, CharSequence title) {
        removeDialog(NetworkProviderSettings.WIFI_DIALOG_ID);

        if (TextUtils.isEmpty(key)) {
            Log.e(TAG, "Not able to show WifiEntry of an empty key");
            return;
        }

        final Bundle bundle = new Bundle();
        bundle.putString(WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY, key);

        new SubSettingLauncher(getContext())
                .setTitleText(title)
                .setDestination(WifiNetworkDetailsFragment.class.getName())
                .setArguments(bundle)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    @Override
    public void onWifiStateChanged() {
        // Do nothing.
    }

    @Override
    public void onSavedWifiEntriesChanged() {
        if (isFinishingOrDestroyed()) {
            return;
        }
        final PreferenceScreen screen = getPreferenceScreen();
        use(SavedAccessPointsPreferenceController2.class)
                .displayPreference(screen, mSavedNetworkTracker.getSavedWifiEntries());
    }

    @Override
    public void onSubscriptionWifiEntriesChanged() {
        if (isFinishingOrDestroyed()) {
            return;
        }
        final PreferenceScreen screen = getPreferenceScreen();
        use(SubscribedAccessPointsPreferenceController2.class)
                .displayPreference(screen, mSavedNetworkTracker.getSubscriptionWifiEntries());
    }
}
