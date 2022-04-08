/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.location;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Dashboard Fragment to display all recent location requests, sorted by recency. */
@SearchIndexable
public class RecentLocationRequestSeeAllFragment extends DashboardFragment {
    private static final String TAG = "RecentLocationReqAll";
    public static final String PATH =
            "com.android.settings.location.RecentLocationRequestSeeAllFragment";

    private static final int MENU_SHOW_SYSTEM = Menu.FIRST + 1;
    private static final int MENU_HIDE_SYSTEM = Menu.FIRST + 2;

    private boolean mShowSystem = false;
    private MenuItem mShowSystemMenu;
    private MenuItem mHideSystemMenu;
    private RecentLocationRequestSeeAllPreferenceController mController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RECENT_LOCATION_REQUESTS_ALL;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final int profileType = getArguments().getInt(ProfileSelectFragment.EXTRA_PROFILE);

        mController = use(RecentLocationRequestSeeAllPreferenceController.class);
        mController.init(this);
        if (profileType != 0) {
            mController.setProfileType(profileType);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_recent_requests_see_all;
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
            new BaseSearchIndexProvider(R.xml.location_recent_requests_see_all);
}
