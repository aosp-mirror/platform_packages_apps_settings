/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.quarantine;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.view.MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.applications.AppIconCacheManager;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.material.appbar.AppBarLayout;

// TODO: b/297934650 - Update this to use SPA framework
@SearchIndexable
public class QuarantinedAppsFragment extends DashboardFragment implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        MenuItem.OnActionExpandListener {
    private static final String TAG = "QuarantinedApps";

    private static final int MENU_SEARCH_APPS = Menu.FIRST + 42;
    private static final int MENU_SHOW_SYSTEM = Menu.FIRST + 43;
    private static final String EXTRA_SHOW_SYSTEM = "show_system";

    private boolean mShowSystem;
    private SearchView mSearchView;
    private String mCurQuery;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mShowSystem = icicle != null && icicle.getBoolean(EXTRA_SHOW_SYSTEM);
        use(QuarantinedAppsScreenController.class).setFilter(mCustomFilter);
        use(QuarantinedAppsScreenController.class).setSession(getSettingsLifecycle());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mSearchView = new SearchView(getContext());
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setIconifiedByDefault(true);

        menu.add(Menu.NONE, MENU_SEARCH_APPS, Menu.NONE, R.string.search_settings)
                .setIcon(R.drawable.ic_find_in_page_24px)
                .setActionView(mSearchView)
                .setOnActionExpandListener(this)
                .setShowAsAction(SHOW_AS_ACTION_ALWAYS | SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        menu.add(Menu.NONE, MENU_SHOW_SYSTEM, Menu.NONE,
                mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_SHOW_SYSTEM) {
            mShowSystem = !mShowSystem;
            item.setTitle(mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
            use(QuarantinedAppsScreenController.class).setFilter(mCustomFilter);
            use(QuarantinedAppsScreenController.class).rebuild();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mCurQuery = !TextUtils.isEmpty(newText) ? newText : null;
        use(QuarantinedAppsScreenController.class).rebuild();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Don't care about this.
        return true;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        return true;
    }

    public final AppFilter mCustomFilter = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry entry) {
            final AppFilter defaultFilter = mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED
                    : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
            return defaultFilter.filterApp(entry) && (mCurQuery == null
                    || entry.label.toLowerCase().contains(mCurQuery.toLowerCase()));
        }
    };

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        final AppBarLayout mAppBarLayout = getActivity().findViewById(R.id.app_bar);
        // To prevent a large space on tool bar.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        final AppBarLayout mAppBarLayout = getActivity().findViewById(R.id.app_bar);
        // To prevent a large space on tool bar.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        return true;
    }

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.quarantined_apps;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.QUARANTINED_APPS_DEV_CONTROL;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AppIconCacheManager.getInstance().release();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.quarantined_apps);
}
