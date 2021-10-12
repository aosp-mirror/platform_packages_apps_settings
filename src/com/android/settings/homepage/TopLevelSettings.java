/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage;

import static com.android.settings.search.actionbar.SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR;
import static com.android.settingslib.search.SearchIndexable.MOBILE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.support.SupportPreferenceController;
import com.android.settings.widget.HighlightableTopLevelPreferenceAdapter;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable(forTarget = MOBILE)
public class TopLevelSettings extends DashboardFragment implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TAG = "TopLevelSettings";
    private static final String SAVED_HIGHLIGHTED_PREF = "highlighted_pref";

    private HighlightableTopLevelPreferenceAdapter mTopLevelAdapter;

    private String mHighlightedPreferenceKey;

    public TopLevelSettings() {
        final Bundle args = new Bundle();
        // Disable the search icon because this page uses a full search view in actionbar.
        args.putBoolean(NEED_SEARCH_ICON_IN_ACTION_BAR, false);
        setArguments(args);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.top_level_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DASHBOARD_SUMMARY;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        HighlightableMenu.fromXml(context, getPreferenceScreenResId());
        use(SupportPreferenceController.class).setActivity(getActivity());
    }

    @Override
    public int getHelpResource() {
        // Disable the help icon because this page uses a full search view in actionbar.
        return 0;
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        setHighlightPreferenceKey(preference.getKey());
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        new SubSettingLauncher(getActivity())
                .setDestination(pref.getFragment())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(caller instanceof Instrumentable
                        ? ((Instrumentable) caller).getMetricsCategory()
                        : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setTitleRes(-1)
                .launch();
        return true;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            mHighlightedPreferenceKey = icicle.getString(SAVED_HIGHLIGHTED_PREF);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_HIGHLIGHTED_PREF, mHighlightedPreferenceKey);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return;
        }
        // Tint the homepage icons
        final int tintColor = Utils.getHomepageIconColor(getContext());
        final int count = screen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            final Preference preference = screen.getPreference(i);
            if (preference == null) {
                break;
            }
            final Drawable icon = preference.getIcon();
            if (icon != null) {
                icon.setTint(tintColor);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        highlightPreferenceIfNeeded();
    }

    @Override
    public void highlightPreferenceIfNeeded() {
        if (mTopLevelAdapter != null) {
            mTopLevelAdapter.requestHighlight();
        }
    }

    /** Highlight a preference with specified key */
    public void setHighlightPreferenceKey(String prefKey) {
        if (mTopLevelAdapter != null) {
            mHighlightedPreferenceKey = prefKey;
            mTopLevelAdapter.highlightPreference(prefKey, /* scrollNeeded= */ false);
        }
    }

    /** Highlight the previous preference */
    public void restorePreviousHighlight() {
        if (mTopLevelAdapter != null) {
            mTopLevelAdapter.restorePreviousHighlight();
        }
    }

    @Override
    protected boolean shouldForceRoundedIcon() {
        return getContext().getResources()
                .getBoolean(R.bool.config_force_rounded_icon_TopLevelSettings);
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(getContext())) {
            return super.onCreateAdapter(preferenceScreen);
        }

        if (TextUtils.isEmpty(mHighlightedPreferenceKey)) {
            mHighlightedPreferenceKey = getHighlightPrefKeyFromArguments();
        }

        Log.d(TAG, "onCreateAdapter, pref key: " + mHighlightedPreferenceKey);
        mTopLevelAdapter = new HighlightableTopLevelPreferenceAdapter(preferenceScreen,
                getListView(), mHighlightedPreferenceKey);
        return mTopLevelAdapter;
    }

    void reloadHighlightMenuKey() {
        if (mTopLevelAdapter == null) {
            return;
        }

        mHighlightedPreferenceKey = getHighlightPrefKeyFromArguments();
        Log.d(TAG, "reloadHighlightMenuKey, pref key: " + mHighlightedPreferenceKey);
        mTopLevelAdapter.highlightPreference(mHighlightedPreferenceKey, /* scrollNeeded= */ true);
    }

    private String getHighlightPrefKeyFromArguments() {
        final Bundle arguments = getArguments();
        final String menuKey = arguments.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
        final String prefKey = HighlightableMenu.lookupPreferenceKey(menuKey);
        if (TextUtils.isEmpty(prefKey)) {
            Log.e(TAG, "Invalid highlight menu key: " + menuKey);
        } else {
            Log.d(TAG, "Menu key: " + menuKey);
        }
        return prefKey;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.top_level_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    // Never searchable, all entries in this page are already indexed elsewhere.
                    return false;
                }
            };
}
