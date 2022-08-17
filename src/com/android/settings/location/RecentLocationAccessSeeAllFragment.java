/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context;
import android.os.Bundle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Dashboard Fragment to display all recent location access (apps), sorted by recency. */
@SearchIndexable
public class RecentLocationAccessSeeAllFragment extends DashboardFragment {
    private static final String TAG = "RecentLocAccessSeeAll";
    public static final String PATH =
            "com.android.settings.location.RecentLocationAccessSeeAllFragment";

    private static final int MENU_SHOW_SYSTEM = Menu.FIRST + 1;
    private static final int MENU_HIDE_SYSTEM = Menu.FIRST + 2;

    private boolean mShowSystem = true;
    private MenuItem mShowSystemMenu;
    private MenuItem mHideSystemMenu;
    private RecentLocationAccessSeeAllPreferenceController mController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RECENT_LOCATION_REQUESTS_ALL;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mController = use(RecentLocationAccessSeeAllPreferenceController.class);
        mController.init(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShowSystem = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
            SystemUiDeviceConfigFlags.PROPERTY_LOCATION_INDICATORS_SMALL_ENABLED, true)
            ? Settings.Secure.getInt(getContentResolver(),
            Settings.Secure.LOCATION_SHOW_SYSTEM_OPS, 1) == 1
            : false;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_recent_access_see_all;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case MENU_SHOW_SYSTEM:
            case MENU_HIDE_SYSTEM:
                mShowSystem = menuItem.getItemId() == MENU_SHOW_SYSTEM;
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCATION_SHOW_SYSTEM_OPS, mShowSystem ? 1 : 0);
                updateMenu();
                if (mController != null) {
                    mController.setShowSystem(mShowSystem);
                }
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void updateMenu() {
        mShowSystemMenu.setVisible(!mShowSystem);
        mHideSystemMenu.setVisible(mShowSystem);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mShowSystemMenu = menu.add(Menu.NONE, MENU_SHOW_SYSTEM, Menu.NONE,
                R.string.menu_show_system);
        mHideSystemMenu = menu.add(Menu.NONE, MENU_HIDE_SYSTEM, Menu.NONE,
                R.string.menu_hide_system);
        updateMenu();
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.location_recent_access_see_all);
}
