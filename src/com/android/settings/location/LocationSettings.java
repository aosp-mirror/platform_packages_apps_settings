/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.SettingInjectorService;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Location access settings.
 */
public class LocationSettings extends LocationSettingsBase
        implements SwitchBar.OnSwitchChangeListener {

    private static final String TAG = "LocationSettings";

    /** Key for preference screen "Mode" */
    private static final String KEY_LOCATION_MODE = "location_mode";
    /** Key for preference category "Recent location requests" */
    private static final String KEY_RECENT_LOCATION_REQUESTS = "recent_location_requests";
    /** Key for preference category "Location services" */
    private static final String KEY_LOCATION_SERVICES = "location_services";

    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private boolean mValidListener = false;
    private Preference mLocationMode;
    private PreferenceCategory mCategoryRecentLocationRequests;
    /** Receives UPDATE_INTENT  */
    private BroadcastReceiver mReceiver;
    private SettingsInjector injector;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
    }

    @Override
    public void onPause() {
        try {
            getActivity().unregisterReceiver(mReceiver);
        } catch (RuntimeException e) {
            // Ignore exceptions caused by race condition
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Swallowing " + e);
            }
        }
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }
        super.onPause();
    }

    private void addPreferencesSorted(List<Preference> prefs, PreferenceGroup container) {
        // If there's some items to display, sort the items and add them to the container.
        Collections.sort(prefs, new Comparator<Preference>() {
            @Override
            public int compare(Preference lhs, Preference rhs) {
                return lhs.getTitle().toString().compareTo(rhs.getTitle().toString());
            }
        });
        for (Preference entry : prefs) {
            container.addPreference(entry);
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        root = getPreferenceScreen();

        mLocationMode = root.findPreference(KEY_LOCATION_MODE);
        mLocationMode.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        activity.startPreferencePanel(
                                LocationMode.class.getName(), null,
                                R.string.location_mode_screen_title, null, LocationSettings.this,
                                0);
                        return true;
                    }
                });

        mCategoryRecentLocationRequests =
                (PreferenceCategory) root.findPreference(KEY_RECENT_LOCATION_REQUESTS);
        RecentLocationApps recentApps = new RecentLocationApps(activity);
        List<Preference> recentLocationRequests = recentApps.getAppList();
        if (recentLocationRequests.size() > 0) {
            addPreferencesSorted(recentLocationRequests, mCategoryRecentLocationRequests);
        } else {
            // If there's no item to display, add a "No recent apps" item.
            Preference banner = new Preference(activity);
            banner.setLayoutResource(R.layout.location_list_no_item);
            banner.setTitle(R.string.location_no_recent_apps);
            banner.setSelectable(false);
            mCategoryRecentLocationRequests.addPreference(banner);
        }

        addLocationServices(activity, root);

        refreshLocationMode();
        return root;
    }

    /**
     * Add the settings injected by external apps into the "App Settings" category. Hides the
     * category if there are no injected settings.
     *
     * Reloads the settings whenever receives
     * {@link SettingInjectorService#ACTION_INJECTED_SETTING_CHANGED}.
     */
    private void addLocationServices(Context context, PreferenceScreen root) {
        PreferenceCategory categoryLocationServices =
                (PreferenceCategory) root.findPreference(KEY_LOCATION_SERVICES);
        injector = new SettingsInjector(context);
        List<Preference> locationServices = injector.getInjectedSettings();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Received settings change intent: " + intent);
                }
                injector.reloadStatusMessages();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(SettingInjectorService.ACTION_INJECTED_SETTING_CHANGED);
        context.registerReceiver(mReceiver, filter);

        if (locationServices.size() > 0) {
            addPreferencesSorted(locationServices, categoryLocationServices);
        } else {
            // If there's no item to display, remove the whole category.
            root.removePreference(categoryLocationServices);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    @Override
    public void onModeChanged(int mode, boolean restricted) {
        switch (mode) {
            case android.provider.Settings.Secure.LOCATION_MODE_OFF:
                mLocationMode.setSummary(R.string.location_mode_location_off_title);
                break;
            case android.provider.Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                mLocationMode.setSummary(R.string.location_mode_sensors_only_title);
                break;
            case android.provider.Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                mLocationMode.setSummary(R.string.location_mode_battery_saving_title);
                break;
            case android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                mLocationMode.setSummary(R.string.location_mode_high_accuracy_title);
                break;
            default:
                break;
        }

        // Restricted user can't change the location mode, so disable the master switch. But in some
        // corner cases, the location might still be enabled. In such case the master switch should
        // be disabled but checked.
        boolean enabled = (mode != android.provider.Settings.Secure.LOCATION_MODE_OFF);
        // Disable the whole switch bar instead of the switch itself. If we disabled the switch
        // only, it would be re-enabled again if the switch bar is not disabled.
        mSwitchBar.setEnabled(!restricted);
        mLocationMode.setEnabled(enabled && !restricted);
        mCategoryRecentLocationRequests.setEnabled(enabled);

        if (enabled != mSwitch.isChecked()) {
            // set listener to null so that that code below doesn't trigger onCheckedChanged()
            if (mValidListener) {
                mSwitchBar.removeOnSwitchChangeListener(this);
            }
            mSwitch.setChecked(enabled);
            if (mValidListener) {
                mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
        // As a safety measure, also reloads on location mode change to ensure the settings are
        // up-to-date even if an affected app doesn't send the setting changed broadcast.
        injector.reloadStatusMessages();
    }

    /**
     * Listens to the state change of the location master switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            setLocationMode(android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        } else {
            setLocationMode(android.provider.Settings.Secure.LOCATION_MODE_OFF);
        }
    }
}
