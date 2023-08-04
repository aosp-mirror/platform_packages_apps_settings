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

package com.android.settings.wifi;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.time.Clock;
import java.time.ZoneOffset;

// TODO(b/151133650): Replace AbstractPreferenceController with BasePreferenceController.
/**
 * This places a preference into a PreferenceGroup owned by some parent
 * controller class when there is a wifi connection present.
 */
public class WifiConnectionPreferenceController extends AbstractPreferenceController implements
        WifiPickerTracker.WifiPickerTrackerCallback, LifecycleObserver {

    private static final String TAG = "WifiConnPrefCtrl";

    private static final String KEY = "active_wifi_connection";

    // Max age of tracked WifiEntries.
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating WifiPickerTracker scans.
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    private UpdateListener mUpdateListener;
    private Context mPrefContext;
    private String mPreferenceGroupKey;
    private PreferenceGroup mPreferenceGroup;
    @VisibleForTesting
    public WifiPickerTracker mWifiPickerTracker;
    private WifiEntryPreference mPreference;
    private int order;
    private int mMetricsCategory;
    // Worker thread used for WifiPickerTracker work.
    private HandlerThread mWorkerThread;

    /**
     * Used to notify a parent controller that this controller has changed in availability, or has
     * updated the content in the preference that it manages.
     */
    public interface UpdateListener {
        void onChildrenUpdated();
    }

    /**
     * @param context            the context for the UI where we're placing the preference
     * @param lifecycle          for listening to lifecycle events for the UI
     * @param updateListener     for notifying a parent controller of changes
     * @param preferenceGroupKey the key to use to lookup the PreferenceGroup where this controller
     *                           will add its preference
     * @param order              the order that the preference added by this controller should use -
     *                           useful when this preference needs to be ordered in a specific way
     *                           relative to others in the PreferenceGroup
     * @param metricsCategory    - the category to use as the source when handling the click on the
     *                           pref to go to the wifi connection detail page
     */
    public WifiConnectionPreferenceController(Context context, Lifecycle lifecycle,
            UpdateListener updateListener, String preferenceGroupKey, int order,
            int metricsCategory) {
        super(context);
        lifecycle.addObserver(this);
        mUpdateListener = updateListener;
        mPreferenceGroupKey = preferenceGroupKey;
        this.order = order;
        mMetricsCategory = metricsCategory;

        mWorkerThread = new HandlerThread(
                TAG + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        mWifiPickerTracker = FeatureFactory.getFeatureFactory()
                .getWifiTrackerLibProvider()
                .createWifiPickerTracker(lifecycle, context,
                        new Handler(Looper.getMainLooper()),
                        mWorkerThread.getThreadHandler(),
                        elapsedRealtimeClock,
                        MAX_SCAN_AGE_MILLIS,
                        SCAN_INTERVAL_MILLIS,
                        this);
    }

    /**
     * This event is triggered when users click back button at 'Network & internet'.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mWorkerThread.quit();
    }

    @Override
    public boolean isAvailable() {
        return mWifiPickerTracker.getConnectedWifiEntry() != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(mPreferenceGroupKey);
        mPrefContext = screen.getContext();
        update();
    }

    private void updatePreference(WifiEntry wifiEntry) {
        if (mPreference != null) {
            mPreferenceGroup.removePreference(mPreference);
            mPreference = null;
        }
        if (wifiEntry == null || mPrefContext == null) {
            return;
        }

        mPreference = new WifiEntryPreference(mPrefContext, wifiEntry);
        mPreference.setKey(KEY);
        mPreference.refresh();
        mPreference.setOrder(order);
        mPreference.setOnPreferenceClickListener(pref -> {
            final Bundle args = new Bundle();
            args.putString(WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY,
                    wifiEntry.getKey());
            new SubSettingLauncher(mPrefContext)
                    .setTitleRes(R.string.pref_title_network_details)
                    .setDestination(WifiNetworkDetailsFragment.class.getName())
                    .setArguments(args)
                    .setSourceMetricsCategory(mMetricsCategory)
                    .launch();
            return true;
        });
        mPreferenceGroup.addPreference(mPreference);
    }

    private void update() {
        final WifiEntry connectedWifiEntry = mWifiPickerTracker.getConnectedWifiEntry();
        if (connectedWifiEntry == null) {
            updatePreference(null);
        } else {
            if (mPreference == null || !mPreference.getWifiEntry().equals(connectedWifiEntry)) {
                updatePreference(connectedWifiEntry);
            } else if (mPreference != null) {
                mPreference.refresh();
            }
        }
        mUpdateListener.onChildrenUpdated();
    }

    /** Called when the state of Wifi has changed. */
    @Override
    public void onWifiStateChanged() {
        update();
    }

    /**
     * Update the results when data changes.
     */
    @Override
    public void onWifiEntriesChanged() {
        update();
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
        // Do nothing.
    }

    @Override
    public void onNumSavedNetworksChanged() {
        // Do nothing.
    }
}
