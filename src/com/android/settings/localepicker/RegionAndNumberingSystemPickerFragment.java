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
import android.content.Context;
import android.os.Bundle;
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
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.TopIntroPreference;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A locale picker fragment to show region country and numbering system.
 *
 * <p>It shows suggestions at the top, then the rest of the locales.
 * Allows the user to search for locales using both their native name and their name in the
 * default locale.</p>
 */
public class RegionAndNumberingSystemPickerFragment extends DashboardFragment implements
        SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

    public static final String EXTRA_TARGET_LOCALE = "extra_target_locale";
    public static final String EXTRA_IS_NUMBERING_SYSTEM = "extra_is_numbering_system";

    private static final String TAG = "RegionAndNumberingSystemPickerFragment";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_LIST = "system_locale_list";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST =
            "system_locale_suggested_list";
    private static final String KEY_TOP_INTRO_PREFERENCE = "top_intro_region";
    private static final String EXTRA_EXPAND_SEARCH_VIEW = "expand_search_view";

    @Nullable
    private SearchView mSearchView = null;
    @Nullable
    private SearchFilter mSearchFilter = null;
    @SuppressWarnings("NullAway")
    private SystemLocaleAllListPreferenceController mSystemLocaleAllListPreferenceController;
    @SuppressWarnings("NullAway")
    private SystemLocaleSuggestedListPreferenceController mSuggestedListPreferenceController;
    @Nullable
    private LocaleStore.LocaleInfo mLocaleInfo;
    @Nullable
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    @SuppressWarnings("NullAway")
    private List<LocaleStore.LocaleInfo> mOriginalLocaleInfos;
    private AppBarLayout mAppBarLayout;
    private RecyclerView mRecyclerView;
    private Activity mActivity;
    private boolean mExpandSearch;
    private boolean mIsNumberingMode;
    @Nullable
    private CharSequence mPrefix;

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        super.onCreate(icicle);
        mActivity = getActivity();
        if (mActivity == null || mActivity.isFinishing()) {
            Log.d(TAG, "onCreate, no activity or activity is finishing");
            return;
        }
        setHasOptionsMenu(true);

        mExpandSearch = mActivity.getIntent().getBooleanExtra(EXTRA_EXPAND_SEARCH_VIEW, false);
        if (icicle != null) {
            mExpandSearch = icicle.getBoolean(EXTRA_EXPAND_SEARCH_VIEW);
        }

        Log.d(TAG, "onCreate, mIsNumberingMode = " + mIsNumberingMode);
        if (!mIsNumberingMode) {
            mActivity.setTitle(R.string.region_selection_title);
        }

        TopIntroPreference topIntroPreference = findPreference(KEY_TOP_INTRO_PREFERENCE);
        if (topIntroPreference != null && mIsNumberingMode) {
            topIntroPreference.setTitle(R.string.top_intro_numbering_system_title);
        }

        if (mSystemLocaleAllListPreferenceController != null) {
            mOriginalLocaleInfos =
                    mSystemLocaleAllListPreferenceController.getSupportedLocaleList();
        }
    }

    @Override
    public @NonNull View onCreateView(@NonNull LayoutInflater inflater,
            @NonNull ViewGroup container, @NonNull Bundle savedInstanceState) {
        mAppBarLayout = mActivity.findViewById(R.id.app_bar);
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
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
                    getContext().getResources().getText(R.string.search_region_hint));
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setMaxWidth(Integer.MAX_VALUE);
            if (mExpandSearch) {
                searchMenuItem.expandActionView();
            }
        }
    }

    private void filterSearch(@Nullable String query) {
        if (mSearchFilter == null) {
            mSearchFilter = new SearchFilter();
        }

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
            mPrefix = prefix;
            if (TextUtils.isEmpty(prefix)) {
                results.values = mOriginalLocaleInfos;
                results.count = mOriginalLocaleInfos.size();
            } else {
                // TODO: decide if we should use the string's locale
                List<LocaleStore.LocaleInfo> newList = new ArrayList<>(mOriginalLocaleInfos);
                newList.addAll(mSystemLocaleAllListPreferenceController.getSuggestedLocaleList());
                Locale locale = Locale.getDefault();
                String prefixString = LocaleHelper.normalizeForSearch(prefix.toString(), locale);
                final int count = newList.size();
                final ArrayList<LocaleStore.LocaleInfo> newValues = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    final LocaleStore.LocaleInfo value = newList.get(i);
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
            // TODO: Need to scroll to first preference when searching.
            if (mRecyclerView != null) {
                mRecyclerView.post(() -> mRecyclerView.scrollToPosition(0));
            }

            mSystemLocaleAllListPreferenceController.onSearchListChanged(mLocaleOptions, mPrefix);
            mSuggestedListPreferenceController.onSearchListChanged(mLocaleOptions, mPrefix);
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

            // For example: English (Australia), Arabic (Egypt)
            Pattern pattern = Pattern.compile("^.*?\\((.*)");
            Matcher matcher = pattern.matcher(valueText);
            if (matcher.find()) {
                String region = matcher.group(1);
                return region.startsWith(prefixString);
            }

            return false;
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
    protected int getPreferenceScreenResId() {
        return R.xml.system_language_picker;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        mLocaleInfo = (LocaleStore.LocaleInfo) getArguments().getSerializable(EXTRA_TARGET_LOCALE);
        mIsNumberingMode = getArguments().getBoolean(EXTRA_IS_NUMBERING_SYSTEM);
        mSuggestedListPreferenceController = new SystemLocaleSuggestedListPreferenceController(
                context, KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST, mLocaleInfo,
                mIsNumberingMode);
        mSystemLocaleAllListPreferenceController = new SystemLocaleAllListPreferenceController(
                context, KEY_PREFERENCE_SYSTEM_LOCALE_LIST, mLocaleInfo, mIsNumberingMode);
        controllers.add(mSuggestedListPreferenceController);
        controllers.add(mSystemLocaleAllListPreferenceController);

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.system_language_picker);

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
