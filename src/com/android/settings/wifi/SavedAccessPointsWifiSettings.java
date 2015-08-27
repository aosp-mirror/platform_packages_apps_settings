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

package com.android.settings.wifi;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.wifi.AccessPointPreference.UserBadgeCache;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * UI to manage saved networks/access points.
 */
public class SavedAccessPointsWifiSettings extends SettingsPreferenceFragment
        implements Indexable, WifiDialog.WifiDialogListener {
    private static final String TAG = "SavedAccessPointsWifiSettings";

    private WifiDialog mDialog;
    private WifiManager mWifiManager;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;
    private AccessPoint mSelectedAccessPoint;

    private UserBadgeCache mUserBadgeCache;

    // Instance state key
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.WIFI_SAVED_ACCESS_POINTS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
        mUserBadgeCache = new UserBadgeCache(getPackageManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                    savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }
        }
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        final Context context = getActivity();

        final List<AccessPoint> accessPoints = WifiTracker.getCurrentAccessPoints(context, true,
                false, true);
        Collections.sort(accessPoints, new Comparator<AccessPoint>() {
            public int compare(AccessPoint ap1, AccessPoint ap2) {
                if (ap1.getConfigName() != null) {
                    return ap1.getConfigName().compareTo(ap2.getConfigName());
                } else {
                    return -1;
                }
            }
        });
        preferenceScreen.removeAll();

        final int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; ++i){
            AccessPointPreference preference = new AccessPointPreference(accessPoints.get(i),
                    context, mUserBadgeCache, true);
            preference.setIcon(null);
            preferenceScreen.addPreference(preference);
        }

        if(getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w(TAG, "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    private void showDialog(AccessPointPreference accessPoint, boolean edit) {
        if (mDialog != null) {
            removeDialog(WifiSettings.WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDlgAccessPoint = accessPoint.getAccessPoint();

        showDialog(WifiSettings.WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WifiSettings.WIFI_DIALOG_ID:
                if (mDlgAccessPoint == null) { // For re-launch from saved state
                    mDlgAccessPoint = new AccessPoint(getActivity(), mAccessPointSavedState);
                    // Reset the saved access point data
                    mAccessPointSavedState = null;
                }
                mSelectedAccessPoint = mDlgAccessPoint;

                // Hide forget button if config editing is locked down
                final boolean hideForgetButton = WifiSettings.isEditabilityLockedDown(getActivity(),
                        mDlgAccessPoint.getConfig());
                mDialog = new WifiDialog(getActivity(), this, mDlgAccessPoint,
                        false /* not editting */, false, true /* hide the submit button */,
                        hideForgetButton);
                return mDialog;

        }
        return super.onCreateDialog(dialogId);
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
            mWifiManager.forget(mSelectedAccessPoint.getConfig().networkId, null);
            getPreferenceScreen().removePreference((Preference) mSelectedAccessPoint.getTag());
            mSelectedAccessPoint = null;
        }
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        // Ignored
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPointPreference) {
            showDialog((AccessPointPreference) preference, false);
            return true;
        } else{
            return super.onPreferenceTreeClick(screen, preference);
        }
    }

    /**
     * For search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();
                final String title = res.getString(R.string.wifi_saved_access_points_titlebar);

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = title;
                data.screenTitle = title;
                data.enabled = enabled;
                result.add(data);

                // Add available Wi-Fi access points
                final List<AccessPoint> accessPoints = WifiTracker.getCurrentAccessPoints(context,
                        true, false, true);

                final int accessPointsSize = accessPoints.size();
                for (int i = 0; i < accessPointsSize; ++i){
                    data = new SearchIndexableRaw(context);
                    data.title = accessPoints.get(i).getSsidStr();
                    data.screenTitle = title;
                    data.enabled = enabled;
                    result.add(data);
                }

                return result;
            }
        };
}
