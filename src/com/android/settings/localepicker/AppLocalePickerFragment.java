/**
 * Copyright (C) 2025 The Android Open Source Project
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
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
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.AppLocaleCollector;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppLocaleUtil;
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
 * A locale picker fragment to show app languages.
 *
 * <p>It shows suggestions at the top, then the rest of the locales.
 * Allows the user to search for locales using both their native name and their name in the
 * default locale.</p>
 */
public class AppLocalePickerFragment extends DashboardFragment implements
        SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {
    public static final String ARG_PACKAGE_NAME = "package";
    public static final String ARG_PACKAGE_UID = "uid";

    private static final String TAG = "AppLocalePickerFragment";
    private static final String EXTRA_EXPAND_SEARCH_VIEW = "expand_search_view";
    private static final String KEY_PREFERENCE_APP_LOCALE_LIST = "app_locale_list";
    private static final String KEY_PREFERENCE_APP_LOCALE_SUGGESTED_LIST =
            "app_locale_suggested_list";
    private static final String KEY_PREFERENCE_APP_DISCLAIMER = "app_locale_disclaimer";
    private static final String KEY_PREFERENCE_APP_INTRO = "app_intro";
    private static final String KEY_PREFERENCE_APP_DESCRIPTION = "app_locale_description";

    @Nullable
    private SearchView mSearchView = null;
    @Nullable
    private SearchFilter mSearchFilter = null;
    @Nullable
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    @Nullable
    private List<LocaleStore.LocaleInfo> mOriginalLocaleInfos;
    @Nullable
    private LocaleStore.LocaleInfo mLocaleInfo;
    @Nullable
    private AppLocaleAllListPreferenceController mAppLocaleAllListPreferenceController;
    @Nullable
    private AppLocaleSuggestedListPreferenceController mSuggestedListPreferenceController;
    private AppBarLayout mAppBarLayout;
    private RecyclerView mRecyclerView;
    private PreferenceScreen mPreferenceScreen;
    private boolean mExpandSearch;
    private int mUid;
    private Activity mActivity;
    @SuppressWarnings("NullAway")
    private String mPackageName;
    @Nullable
    private ApplicationInfo mApplicationInfo;
    private boolean mIsNumberingMode;

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        super.onCreate(icicle);
        mActivity = getActivity();

        if (mActivity.isFinishing()) {
            return;
        }

        if (TextUtils.isEmpty(mPackageName)) {
            Log.d(TAG, "There is no package name.");
            return;
        }

        if (!canDisplayLocaleUi()) {
            Log.w(TAG, "Not allow to display Locale Settings UI.");
            return;
        }

        mPreferenceScreen = getPreferenceScreen();
        setHasOptionsMenu(true);
        mApplicationInfo = getApplicationInfo(mPackageName, mUid);
        setupDisclaimerPreference();
        setupIntroPreference();
        setupDescriptionPreference();
        mExpandSearch = mActivity.getIntent().getBooleanExtra(EXTRA_EXPAND_SEARCH_VIEW, false);
        if (icicle != null) {
            mExpandSearch = icicle.getBoolean(EXTRA_EXPAND_SEARCH_VIEW);
        }

        AppLocaleCollector appLocaleCollector = new AppLocaleCollector(mActivity, mPackageName);
        Set<LocaleStore.LocaleInfo> localeList = appLocaleCollector.getSupportedLocaleList(null,
                false, false);
        mLocaleOptions = new ArrayList<>(localeList.size());
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
                    mActivity.getResources().getText(R.string.search_language_hint));
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setMaxWidth(Integer.MAX_VALUE);
            if (mExpandSearch) {
                searchMenuItem.expandActionView();
            }
        }
    }

    private void setupDisclaimerPreference() {
        final Preference pref = mPreferenceScreen.findPreference(KEY_PREFERENCE_APP_DISCLAIMER);
        boolean shouldShowPref = pref != null && FeatureFlagUtils.isEnabled(
                mActivity, FeatureFlagUtils.SETTINGS_APP_LOCALE_OPT_IN_ENABLED);
        pref.setVisible(shouldShowPref);
    }

    private void setupIntroPreference() {
        final Preference pref = mPreferenceScreen.findPreference(KEY_PREFERENCE_APP_INTRO);
        if (pref != null && mApplicationInfo != null) {
            pref.setIcon(Utils.getBadgedIcon(mActivity, mApplicationInfo));
            pref.setTitle(mApplicationInfo.loadLabel(mActivity.getPackageManager()));
        }
    }

    private void setupDescriptionPreference() {
        final Preference pref = mPreferenceScreen.findPreference(
                KEY_PREFERENCE_APP_DESCRIPTION);
        int res = getAppDescription();
        if (pref != null && res != -1) {
            pref.setVisible(true);
            pref.setTitle(mActivity.getString(res));
        } else {
            pref.setVisible(false);
        }
    }

    private int getAppDescription() {
        LocaleList packageLocaleList = AppLocaleUtil.getPackageLocales(mActivity, mPackageName);
        String[] assetLocaleList = AppLocaleUtil.getAssetLocales(mActivity, mPackageName);
        // TODO add appended url string, "Learn more", to these both sentences.
        if ((packageLocaleList != null && packageLocaleList.isEmpty())
                || (packageLocaleList == null && assetLocaleList.length == 0)) {
            return R.string.desc_no_available_supported_locale;
        }
        return -1;
    }

    private @Nullable ApplicationInfo getApplicationInfo(String packageName, int userId) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = mActivity.getPackageManager()
                    .getApplicationInfoAsUser(packageName, /* flags= */ 0, userId);
            return applicationInfo;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Application info not found for: " + packageName);
            return null;
        }
    }

    private boolean canDisplayLocaleUi() {
        try {
            PackageManager packageManager = getPackageManager();
            return AppLocaleUtil.canDisplayLocaleUi(mActivity,
                    packageManager.getApplicationInfo(mPackageName, 0),
                    packageManager.queryIntentActivities(AppLocaleUtil.LAUNCHER_ENTRY_INTENT,
                            PackageManager.GET_META_DATA));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find info for package: " + mPackageName);
        }

        return false;
    }

    private void filterSearch(@Nullable String query) {
        if (mAppLocaleAllListPreferenceController == null) {
            Log.d(TAG, "filterSearch(), can not get preference.");
            return;
        }

        if (mSearchFilter == null) {
            mSearchFilter = new SearchFilter();
        }

        mOriginalLocaleInfos = mAppLocaleAllListPreferenceController.getSupportedLocaleList();
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
                mOriginalLocaleInfos = new ArrayList<>(mLocaleOptions);
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
            if (mAppLocaleAllListPreferenceController == null
                    || mSuggestedListPreferenceController == null) {
                Log.d(TAG, "publishResults(), can not get preference.");
                return;
            }

            mLocaleOptions = (ArrayList<LocaleStore.LocaleInfo>) results.values;
            // Need to scroll to first preference when searching.
            if (mRecyclerView != null) {
                mRecyclerView.post(() -> mRecyclerView.scrollToPosition(0));
            }

            mAppLocaleAllListPreferenceController.onSearchListChanged(mLocaleOptions, null);
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
        return SettingsEnums.APPS_LOCALE_LIST;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_language_picker;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context) {
        Bundle args = getArguments();
        mPackageName = args.getString(ARG_PACKAGE_NAME);
        mUid = args.getInt(ARG_PACKAGE_UID);
        mLocaleInfo = (LocaleStore.LocaleInfo) args.getSerializable(
                RegionAndNumberingSystemPickerFragment.EXTRA_TARGET_LOCALE);
        mIsNumberingMode = args.getBoolean(
                RegionAndNumberingSystemPickerFragment.EXTRA_IS_NUMBERING_SYSTEM);

        mSuggestedListPreferenceController =
                new AppLocaleSuggestedListPreferenceController(context,
                        KEY_PREFERENCE_APP_LOCALE_SUGGESTED_LIST, mPackageName, mIsNumberingMode,
                        mLocaleInfo);
        mAppLocaleAllListPreferenceController = new AppLocaleAllListPreferenceController(
                context, KEY_PREFERENCE_APP_LOCALE_LIST, mPackageName, mIsNumberingMode,
                mLocaleInfo);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(mSuggestedListPreferenceController);
        controllers.add(mAppLocaleAllListPreferenceController);

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.app_language_picker);
}
