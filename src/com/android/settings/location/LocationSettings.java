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

import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LOCATION_SWITCH_TITLE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.database.ContentObserver;
import android.location.SettingInjectorService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.location.RecentLocationApps;
import com.android.settingslib.search.SearchIndexable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * System location settings (Settings &gt; Location). The screen has three parts:
 * <ul>
 *     <li>Platform location controls</li>
 *     <ul>
 *         <li>In switch bar: location primary switch. Used to toggle location on and off.
 *         </li>
 *     </ul>
 *     <li>Recent location requests: automatically populated by {@link RecentLocationApps}</li>
 *     <li>Location services: multi-app settings provided from outside the Android framework. Each
 *     is injected by a system-partition app via the {@link SettingInjectorService} API.</li>
 * </ul>
 * <p>
 * Note that as of KitKat, the {@link SettingInjectorService} is the preferred method for OEMs to
 * add their own settings to this page, rather than directly modifying the framework code. Among
 * other things, this simplifies integration with future changes to the default (AOSP)
 * implementation.
 */
@SearchIndexable
public class LocationSettings extends DashboardFragment implements
        LocationEnabler.LocationModeChangeListener {

    private static final String TAG = "LocationSettings";
    private static final String RECENT_LOCATION_ACCESS_PREF_KEY = "recent_location_access";

    private LocationSwitchBarController mSwitchBarController;
    private LocationEnabler mLocationEnabler;
    private RecentLocationAccessPreferenceController mController;
    private ContentObserver mContentObserver;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.LOCATION;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
        switchBar.setTitle(getContext().getString(R.string.location_settings_primary_switch_title));
        switchBar.show();
        mSwitchBarController = new LocationSwitchBarController(activity, switchBar,
                getSettingsLifecycle());
        mLocationEnabler = new LocationEnabler(getContext(), this, getSettingsLifecycle());
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                mController.updateShowSystem();
            }
        };
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(
                        Settings.Secure.LOCATION_SHOW_SYSTEM_OPS), /* notifyForDescendants= */
                false, mContentObserver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        use(AppLocationPermissionPreferenceController.class).init(this);
        mController = use(RecentLocationAccessPreferenceController.class);
        mController.init(this);
        use(RecentLocationAccessSeeAllButtonPreferenceController.class).init(this);
        use(LocationForWorkPreferenceController.class).init(this);
        use(LocationSettingsFooterPreferenceController.class).init(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_settings;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        replaceEnterpriseStringTitle("managed_profile_location_switch",
                WORK_PROFILE_LOCATION_SWITCH_TITLE, R.string.managed_profile_location_switch_title);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        if (mLocationEnabler.isEnabled(mode)) {
            scrollToPreference(RECENT_LOCATION_ACCESS_PREF_KEY);
        }
    }

    static void addPreferencesSorted(List<Preference> prefs, PreferenceGroup container) {
        // If there's some items to display, sort the items and add them to the container.
        Collections.sort(prefs,
                Comparator.comparing(lhs -> lhs.getTitle().toString()));
        for (Preference entry : prefs) {
            container.addPreference(entry);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.location_settings);
}
