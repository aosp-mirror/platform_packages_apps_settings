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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.location.SettingInjectorService;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
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
 *         <li>In switch bar: location master switch. Used to toggle location on and off.
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
public class LocationSettings extends DashboardFragment {

    private static final String TAG = "LocationSettings";

    private LocationSwitchBarController mSwitchBarController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.LOCATION;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SwitchBar switchBar = activity.getSwitchBar();
        switchBar.setSwitchBarText(R.string.location_settings_master_switch_title,
                R.string.location_settings_master_switch_title);
        mSwitchBarController = new LocationSwitchBarController(activity, switchBar,
                getSettingsLifecycle());
        switchBar.show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        use(AppLocationPermissionPreferenceController.class).init(this);
        use(RecentLocationRequestPreferenceController.class).init(this);
        use(LocationServicePreferenceController.class).init(this);
        use(LocationFooterPreferenceController.class).init(this);
        use(LocationForWorkPreferenceController.class).init(this);
        use(LocationServiceForWorkPreferenceController.class).init(this);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
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
