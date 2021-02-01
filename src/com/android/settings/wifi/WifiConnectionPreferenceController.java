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

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

// TODO(b/151133650): Replace AbstractPreferenceController with BasePreferenceController.
/**
 * This places a preference into a PreferenceGroup owned by some parent
 * controller class when there is a wifi connection present.
 */
public class WifiConnectionPreferenceController extends AbstractPreferenceController implements
        WifiTracker.WifiListener {

    private static final String TAG = "WifiConnPrefCtrl";

    private static final String KEY = "active_wifi_connection";

    private UpdateListener mUpdateListener;
    private Context mPrefContext;
    private String mPreferenceGroupKey;
    private PreferenceGroup mPreferenceGroup;
    private WifiTracker mWifiTracker;
    private AccessPointPreference mPreference;
    private AccessPointPreference.UserBadgeCache mBadgeCache;
    private int order;
    private int mMetricsCategory;

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
        mUpdateListener = updateListener;
        mPreferenceGroupKey = preferenceGroupKey;
        mWifiTracker = WifiTrackerFactory.create(context, this, lifecycle, true /* includeSaved */,
                true /* includeScans */);
        this.order = order;
        mMetricsCategory = metricsCategory;
        mBadgeCache = new AccessPointPreference.UserBadgeCache(context.getPackageManager());
    }

    @Override
    public boolean isAvailable() {
        return mWifiTracker.isConnected() && getCurrentAccessPoint() != null;
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

    private AccessPoint getCurrentAccessPoint() {
        for (AccessPoint accessPoint : mWifiTracker.getAccessPoints()) {
            if (accessPoint.isActive()) {
                return accessPoint;
            }
        }
        return null;
    }

    private void updatePreference(AccessPoint accessPoint) {
        if (mPreference != null) {
            mPreferenceGroup.removePreference(mPreference);
            mPreference = null;
        }
        if (accessPoint == null) {
            return;
        }
        if (mPrefContext != null) {
            mPreference = new AccessPointPreference(accessPoint, mPrefContext, mBadgeCache,
                    R.drawable.ic_wifi_signal_0, false /* forSavedNetworks */);
            mPreference.setKey(KEY);
            mPreference.refresh();
            mPreference.setOrder(order);

            mPreference.setOnPreferenceClickListener(pref -> {
                Bundle args = new Bundle();
                mPreference.getAccessPoint().saveWifiState(args);
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
    }

    private void update() {
        AccessPoint connectedAccessPoint = null;
        if (mWifiTracker.isConnected()) {
            connectedAccessPoint = getCurrentAccessPoint();
        }
        if (connectedAccessPoint == null) {
            updatePreference(null);
        } else {
          if (mPreference == null || !mPreference.getAccessPoint().equals(connectedAccessPoint)) {
              updatePreference(connectedAccessPoint);
          } else if (mPreference != null) {
              mPreference.refresh();
          }
        }
        mUpdateListener.onChildrenUpdated();
    }

    @Override
    public void onWifiStateChanged(int state) {
        update();
    }

    @Override
    public void onConnectedChanged() {
        update();
    }

    @Override
    public void onAccessPointsChanged() {
        update();
    }
}
