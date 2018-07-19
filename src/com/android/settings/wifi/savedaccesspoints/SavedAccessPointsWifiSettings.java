/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.wifi.WifiConfigUiBase;
import com.android.settings.wifi.WifiDialog;
import com.android.settings.wifi.WifiSettings;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiSavedConfigUtils;

import java.util.Collections;
import java.util.List;

/**
 * UI to manage saved networks/access points.
 */
public class SavedAccessPointsWifiSettings extends DashboardFragment
        implements WifiDialog.WifiDialogListener {
    private static final String TAG = "SavedAccessPoints";
    @VisibleForTesting
    static final int MSG_UPDATE_PREFERENCES = 1;

    @VisibleForTesting
    final WifiManager.ActionListener mForgetListener = new WifiManager.ActionListener() {
        @Override
        public void onSuccess() {
            postUpdatePreference();
        }

        @Override
        public void onFailure(int reason) {
            postUpdatePreference();
        }
    };

    @VisibleForTesting
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            if (msg.what == MSG_UPDATE_PREFERENCES) {
                initPreferences();
            }
        }
    };

    private final WifiManager.ActionListener mSaveListener = new WifiManager.ActionListener() {
        @Override
        public void onSuccess() {
            postUpdatePreference();
        }

        @Override
        public void onFailure(int reason) {
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity,
                        R.string.wifi_failed_save_message,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private WifiDialog mDialog;
    private WifiManager mWifiManager;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;
    private AccessPoint mSelectedAccessPoint;

    private AccessPointPreference.UserBadgeCache mUserBadgeCache;

    // Instance state key
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.WIFI_SAVED_ACCESS_POINTS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_display_saved_access_points;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiManager = (WifiManager) getContext()
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                        savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }
        }
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        final Context context = getPrefContext();

        final List<AccessPoint> accessPoints =
                WifiSavedConfigUtils.getAllConfigs(context, mWifiManager);
        Collections.sort(accessPoints, SavedNetworkComparator.INSTANCE);
        cacheRemoveAllPrefs(preferenceScreen);

        final int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; ++i) {
            AccessPoint ap = accessPoints.get(i);
            String key = ap.getKey();
            AccessPointPreference preference =
                    (AccessPointPreference) getCachedPreference(key);
            if (preference == null) {
                preference = new AccessPointPreference(ap, context, mUserBadgeCache, true);
                preference.setKey(key);
                preference.setIcon(null);
                preferenceScreen.addPreference(preference);
            }
            preference.setOrder(i);
        }

        removeCachedPrefs(preferenceScreen);

        if (getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w(TAG, "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    private void postUpdatePreference() {
        if (!mHandler.hasMessages(MSG_UPDATE_PREFERENCES)) {
            mHandler.sendEmptyMessage(MSG_UPDATE_PREFERENCES);
        }
    }

    private void showWifiDialog(@Nullable AccessPointPreference accessPoint) {
        if (mDialog != null) {
            removeDialog(WifiSettings.WIFI_DIALOG_ID);
            mDialog = null;
        }

        if (accessPoint != null) {
            // Save the access point and edit mode
            mDlgAccessPoint = accessPoint.getAccessPoint();
        } else {
            // No access point is selected. Clear saved state.
            mDlgAccessPoint = null;
            mAccessPointSavedState = null;
        }

        showDialog(WifiSettings.WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WifiSettings.WIFI_DIALOG_ID:
                if (mDlgAccessPoint == null && mAccessPointSavedState == null) {
                    // Add new network
                    mDialog = WifiDialog.createFullscreen(getActivity(), this, null,
                            WifiConfigUiBase.MODE_CONNECT);
                } else {
                    // Modify network
                    if (mDlgAccessPoint == null) {
                        // Restore AP from save state
                        mDlgAccessPoint = new AccessPoint(getActivity(), mAccessPointSavedState);
                        // Reset the saved access point data
                        mAccessPointSavedState = null;
                    }
                    mDialog = WifiDialog.createModal(getActivity(), this, mDlgAccessPoint,
                            WifiConfigUiBase.MODE_VIEW);
                }
                mSelectedAccessPoint = mDlgAccessPoint;

                return mDialog;
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case WifiSettings.WIFI_DIALOG_ID:
                return MetricsProto.MetricsEvent.DIALOG_WIFI_SAVED_AP_EDIT;
            default:
                return 0;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }
    }

    @Override
    public void onForget(WifiDialog dialog) {
        if (mSelectedAccessPoint != null) {
            if (mSelectedAccessPoint.isPasspointConfig()) {
                try {
                    mWifiManager.removePasspointConfiguration(
                            mSelectedAccessPoint.getPasspointFqdn());
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to remove Passpoint configuration for "
                            + mSelectedAccessPoint.getConfigName());
                }
                postUpdatePreference();
            } else {
                // mForgetListener will call initPreferences upon completion
                mWifiManager.forget(mSelectedAccessPoint.getConfig().networkId, mForgetListener);
            }
            mSelectedAccessPoint = null;
        }
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        mWifiManager.save(dialog.getController().getConfig(), mSaveListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof AccessPointPreference) {
            showWifiDialog((AccessPointPreference) preference);
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }
}
