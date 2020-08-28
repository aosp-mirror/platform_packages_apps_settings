/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class UnrestrictedDataAccess extends DashboardFragment {

    private static final String TAG = "UnrestrictedDataAccess";

    private static final int MENU_SHOW_SYSTEM = Menu.FIRST + 42;
    private static final String EXTRA_SHOW_SYSTEM = "show_system";

    private boolean mShowSystem;
    private AppFilter mFilter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mShowSystem = icicle != null && icicle.getBoolean(EXTRA_SHOW_SYSTEM);

        use(UnrestrictedDataAccessPreferenceController.class).setParentFragment(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_SHOW_SYSTEM, Menu.NONE,
                mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SHOW_SYSTEM:
                mShowSystem = !mShowSystem;
                item.setTitle(mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
                mFilter = mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED
                        : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;

                use(UnrestrictedDataAccessPreferenceController.class).setFilter(mFilter);
                use(UnrestrictedDataAccessPreferenceController.class).rebuild();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mFilter = mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED
                : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
        use(UnrestrictedDataAccessPreferenceController.class).setSession(getSettingsLifecycle());
        use(UnrestrictedDataAccessPreferenceController.class).setFilter(mFilter);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_unrestricted_data_access;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DATA_USAGE_UNRESTRICTED_ACCESS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.unrestricted_data_access_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.unrestricted_data_access_settings);
}
