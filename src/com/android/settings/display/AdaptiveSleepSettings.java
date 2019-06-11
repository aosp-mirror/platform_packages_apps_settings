/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.display.AdaptiveSleepPreferenceController.hasSufficientPermission;
import static com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice.PREF;
import static com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice.PREF_KEY_INTERACTED;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

import java.util.Arrays;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AdaptiveSleepSettings extends DashboardFragment {

    private static final String TAG = "AdaptiveSleepSettings";
    private Context mContext;
    private String mPackageName;
    private PackageManager mPackageManager;

    @VisibleForTesting
    Preference mPermissionRequiredPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final FooterPreference footerPreference =
                mFooterPreferenceMixin.createFooterPreference();
        mContext = getContext();
        mPermissionRequiredPreference = createPermissionMissionPreference();

        footerPreference.setIcon(R.drawable.ic_privacy_shield_24dp);
        footerPreference.setTitle(R.string.adaptive_sleep_privacy);

        getPreferenceScreen().addPreference(mPermissionRequiredPreference);
        mPermissionRequiredPreference.setVisible(false);
        mPackageManager = mContext.getPackageManager();
        mPackageName = mPackageManager.getAttentionServicePackageName();
        mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_INTERACTED, true)
                .apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasSufficientPermission(mPackageManager)) {
            mPermissionRequiredPreference.setVisible(true);
        }
        else {
            mPermissionRequiredPreference.setVisible(false);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adaptive_sleep_detail;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ADAPTIVE_SLEEP;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_adaptive_sleep;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.adaptive_sleep_detail;
                    return Arrays.asList(sir);
                }
            };

    private Preference createPermissionMissionPreference() {
        Preference preference = new Preference(mContext, null);
        preference.setIcon(R.drawable.ic_info_outline_24);
        // Makes sure it's above the toggle.
        preference.setOrder(1);
        preference.setPersistent(true);
        preference.setTitle(R.string.adaptive_sleep_title_no_permission);
        preference.setSummary(R.string.adaptive_sleep_summary_no_permission);
        preference.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + mPackageName));
            mContext.startActivity(intent);
            return true;
        });
        return preference;
    }

    @VisibleForTesting
    void setupForTesting(PackageManager packageManager, Context context) {
        mContext = context;
        mPackageManager = packageManager;
        mPermissionRequiredPreference = createPermissionMissionPreference();
    }
}
