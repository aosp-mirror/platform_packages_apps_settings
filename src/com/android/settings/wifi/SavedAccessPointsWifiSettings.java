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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.icu.text.Collator;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import android.widget.Toast;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiSavedConfigUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * UI to manage saved networks/access points.
 * TODO(b/64806699): convert to {@link DashboardFragment} with {@link PreferenceController}s
 */
public class SavedAccessPointsWifiSettings extends SettingsPreferenceFragment
        implements Indexable, WifiDialog.WifiDialogListener {
    private static final String TAG = "SavedAccessPointsWifiSettings";
    private static final Comparator<AccessPoint> SAVED_NETWORK_COMPARATOR =
            new Comparator<AccessPoint>() {
        final Collator mCollator = Collator.getInstance();
        @Override
        public int compare(AccessPoint ap1, AccessPoint ap2) {
            return mCollator.compare(
                    nullToEmpty(ap1.getConfigName()), nullToEmpty(ap2.getConfigName()));
        }

        private String nullToEmpty(String string) {
            return (string == null) ? "" : string;
        }
    };

    private final WifiManager.ActionListener mForgetListener = new WifiManager.ActionListener() {
        @Override
        public void onSuccess() {
            initPreferences();
        }

        @Override
        public void onFailure(int reason) {
            initPreferences();
        }
    };

    private final WifiManager.ActionListener mSaveListener = new WifiManager.ActionListener() {
        @Override
        public void onSuccess() {
            initPreferences();
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
    private Preference mAddNetworkPreference;

    private AccessPointPreference.UserBadgeCache mUserBadgeCache;

    // Instance state key
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.WIFI_SAVED_ACCESS_POINTS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
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
        final Context context = getPrefContext();

        final List<AccessPoint> accessPoints =
                WifiSavedConfigUtils.getAllConfigs(context, mWifiManager);
        Collections.sort(accessPoints, SAVED_NETWORK_COMPARATOR);
        cacheRemoveAllPrefs(preferenceScreen);

        final int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; ++i) {
            AccessPoint ap = accessPoints.get(i);
            String key = AccessPointPreference.generatePreferenceKey(ap);
            LongPressAccessPointPreference preference =
                    (LongPressAccessPointPreference) getCachedPreference(key);
            if (preference == null) {
                preference = new LongPressAccessPointPreference(
                        ap, context, mUserBadgeCache, true, this);
                preference.setKey(key);
                preference.setIcon(null);
                preferenceScreen.addPreference(preference);
            }
            preference.setOrder(i);
        }

        removeCachedPrefs(preferenceScreen);

        if (mAddNetworkPreference == null) {
            mAddNetworkPreference = new Preference(getPrefContext());
            mAddNetworkPreference.setIcon(R.drawable.ic_menu_add_inset);
            mAddNetworkPreference.setTitle(R.string.wifi_add_network);
        }
        mAddNetworkPreference.setOrder(accessPointsSize);
        preferenceScreen.addPreference(mAddNetworkPreference);

        if(getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w(TAG, "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    private void showWifiDialog(@Nullable LongPressAccessPointPreference accessPoint) {
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
                initPreferences();
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
        if (preference instanceof LongPressAccessPointPreference) {
            showWifiDialog((LongPressAccessPointPreference) preference);
            return true;
        } else if (preference == mAddNetworkPreference) {
            showWifiDialog(null);
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    /**
     * For search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.wifi_display_saved_access_points;
                return Arrays.asList(sir);
            }

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
                final List<AccessPoint> accessPoints = WifiSavedConfigUtils.getAllConfigs(
                        context, context.getSystemService(WifiManager.class));

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
