/**
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.localepicker;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceCategory;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.SystemLocaleCollector;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A locale picker fragment to show system languages.
 *
 * <p>It shows suggestions at the top, then the rest of the locales.
 * Allows the user to search for locales using both their native name and their name in the
 * default locale.</p>
 */
public class SystemLocalePickerFragment extends DashboardFragment implements
        SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

    private static final String TAG = "SystemLocalePickerFragment";
    private static final String EXTRA_EXPAND_SEARCH_VIEW = "expand_search_view";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_LIST = "system_locale_list";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST =
            "system_locale_suggested_list";

    @Nullable
    private SearchView mSearchView = null;
    @Nullable
    private SearchFilter mSearchFilter = null;
    @Nullable
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    @Nullable
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    @Nullable
    private List<LocaleStore.LocaleInfo> mOriginalLocaleInfos;
    @Nullable
    private SystemLocaleAllListPreferenceController mSystemLocaleAllListPreferenceController;
    @Nullable
    private SystemLocaleSuggestedListPreferenceController mSuggestedListPreferenceController;
    private AppBarLayout mAppBarLayout;
    private RecyclerView mRecyclerView;
    private Activity mActivity;
    private boolean mExpandSearch;

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        super.onCreate(icicle);
        mActivity = getActivity();
        if (mActivity.isFinishing()) {
            return;
        }
        setHasOptionsMenu(true);

        mExpandSearch = mActivity.getIntent().getBooleanExtra(EXTRA_EXPAND_SEARCH_VIEW, false);
        if (icicle != null) {
            mExpandSearch = icicle.getBoolean(EXTRA_EXPAND_SEARCH_VIEW);
        }

        SystemLocaleCollector systemLocaleCollector = new SystemLocaleCollector(getContext(), null);
        mLocaleList = systemLocaleCollector.getSupportedLocaleList(null, false, false);
        mLocaleOptions = new ArrayList<>(mLocaleList.size());
    }

    @Override
    public @NonNull View onCreateView(@NonNull LayoutInflater inflater,
            @NonNull ViewGroup container, @NonNull Bundle savedInstanceState) {
        mAppBarLayout = mActivity.findViewById(R.id.app_bar);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = view.findViewById(R.id.recycler_view);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSearchView != null) {
            outState.putBoolean(EXTRA_EXPAND_SEARCH_VIEW, !mSearchView.isIconified());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.language_selection_list, menu);
        final MenuItem searchMenuItem = menu.findItem(R.id.locale_search_menu);
        if (searchMenuItem != null) {
            searchMenuItem.setOnActionExpandListener(this);
            mSearchView = (SearchView) searchMenuItem.getActionView();
            mSearchView.setQueryHint(
                    getContext().getResources().getText(R.string.search_language_hint));
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setMaxWidth(Integer.MAX_VALUE);
            if (mExpandSearch) {
                searchMenuItem.expandActionView();
            }
        }
    }

    private void filterSearch(@Nullable String query) {
        if (mSystemLocaleAllListPreferenceController == null) {
            Log.d(TAG, "filterSearch(), can not get preference.");
            return;
        }

        if (mSearchFilter == null) {
            mSearchFilter = new SearchFilter();
        }

        mOriginalLocaleInfos = mSystemLocaleAllListPreferenceController.getSupportedLocaleList();
        // If we haven't load apps list completely, don't filter anything.
        if (mOriginalLocaleInfos == null) {
            Log.w(TAG, "Locales haven't loaded completely yet, so nothing can be filtered");
            return;
        }
        mSearchFilter.filter(query);
    }

    private class SearchFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOriginalLocaleInfos == null) {
                mOriginalLocaleInfos = new ArrayList<>(mLocaleList);
            }

            if (TextUtils.isEmpty(prefix)) {
                results.values = mOriginalLocaleInfos;
                results.count = mOriginalLocaleInfos.size();
            } else {
                // TODO: decide if we should use the string's locale
                Locale locale = Locale.getDefault();
                String prefixString = LocaleHelper.normalizeForSearch(prefix.toString(), locale);

                final int count = mOriginalLocaleInfos.size();
                final ArrayList<LocaleStore.LocaleInfo> newValues = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    final LocaleStore.LocaleInfo value = mOriginalLocaleInfos.get(i);
                    final String nameToCheck = LocaleHelper.normalizeForSearch(
                            value.getFullNameInUiLanguage(), locale);
                    final String nativeNameToCheck = LocaleHelper.normalizeForSearch(
                            value.getFullNameNative(), locale);
                    if ((wordMatches(nativeNameToCheck, prefixString)
                            || wordMatches(nameToCheck, prefixString)) && !newValues.contains(
                            value)) {
                        newValues.add(value);
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (mSystemLocaleAllListPreferenceController == null
                    || mSuggestedListPreferenceController == null) {
                Log.d(TAG, "publishResults(), can not get preference.");
                return;
            }

            mLocaleOptions = (ArrayList<LocaleStore.LocaleInfo>) results.values;
            // Need to scroll to first preference when searching.
            if (mRecyclerView != null) {
                mRecyclerView.post(() -> mRecyclerView.scrollToPosition(0));
            }

            mSystemLocaleAllListPreferenceController.onSearchListChanged(mLocaleOptions, null);
            mSuggestedListPreferenceController.onSearchListChanged(mLocaleOptions, null);
        }

        // TODO: decide if this is enough, or we want to use a BreakIterator...
        private boolean wordMatches(String valueText, String prefixString) {
            if (valueText == null) {
                return false;
            }

            // First match against the whole, non-split value
            if (valueText.startsWith(prefixString)) {
                return true;
            }

            return Arrays.stream(valueText.split(" "))
                    .anyMatch(word -> word.startsWith(prefixString));
        }
    }

    @Override
    public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
        // To prevent a large space on tool bar.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        // To prevent user can expand the collapsing tool bar view.
        ViewCompat.setNestedScrollingEnabled(mRecyclerView, false);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
        // We keep the collapsed status after user cancel the search function.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        ViewCompat.setNestedScrollingEnabled(mRecyclerView, true);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(@Nullable String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(@Nullable String newText) {
        filterSearch(newText);
        return false;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER_LOCALE_LIST;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_language_picker;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle) {
        LocaleList explicitLocales = null;
        if (isDeviceDemoMode()) {
            Bundle bundle = getIntent().getExtras();
            explicitLocales = bundle == null
                    ? null
                    : bundle.getParcelable(Settings.EXTRA_EXPLICIT_LOCALES, LocaleList.class);
            Log.i(TAG, "Has explicit locales : " + explicitLocales);
        }
        mSuggestedListPreferenceController =
                new SystemLocaleSuggestedListPreferenceController(context,
                        KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST);
        mSystemLocaleAllListPreferenceController = new SystemLocaleAllListPreferenceController(
                context, KEY_PREFERENCE_SYSTEM_LOCALE_LIST, explicitLocales);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(mSuggestedListPreferenceController);
        controllers.add(mSystemLocaleAllListPreferenceController);

        return controllers;
    }

    private boolean isDeviceDemoMode() {
        return Settings.Global.getInt(
                getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 0) == 1;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.system_language_picker);
}
