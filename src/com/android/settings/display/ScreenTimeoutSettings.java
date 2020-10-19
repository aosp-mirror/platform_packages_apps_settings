/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.actionbar.SearchMenuController;
import com.android.settings.support.actionbar.HelpMenuController;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.widget.RadioButtonPreferenceWithExtraWidget;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that is used to control screen timeout.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ScreenTimeoutSettings extends RadioButtonPickerFragment implements
        HelpResourceProvider {
    private static final String TAG = "ScreenTimeout";
    /** If there is no setting in the provider, use this. */
    public static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private CharSequence[] mInitialEntries;
    private CharSequence[] mInitialValues;
    private FooterPreference mPrivacyPreference;
    private AdaptiveSleepPreferenceController mAdaptiveSleepController;
    private AdaptiveSleepPermissionPreferenceController mAdaptiveSleepPermissionController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SearchMenuController.init(this /* host */);
        HelpMenuController.init(this /* host */);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mInitialEntries = getResources().getStringArray(R.array.screen_timeout_entries);
        mInitialValues = getResources().getStringArray(R.array.screen_timeout_values);
        mAdaptiveSleepController = new AdaptiveSleepPreferenceController(context);
        mAdaptiveSleepPermissionController = new AdaptiveSleepPermissionPreferenceController(
                context);
        mPrivacyPreference = new FooterPreference(context);
        mPrivacyPreference.setIcon(R.drawable.ic_privacy_shield_24dp);
        mPrivacyPreference.setTitle(R.string.adaptive_sleep_privacy);
        mPrivacyPreference.setSelectable(false);
        mPrivacyPreference.setLayoutResource(R.layout.preference_footer);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final Context context = getContext();
        final List<CandidateInfo> candidates = new ArrayList<>();
        final long maxTimeout = getMaxScreenTimeout(context);
        for (int i = 0; i < mInitialValues.length; ++i) {
            if (Long.parseLong(mInitialValues[i].toString()) <= maxTimeout) {
                candidates.add(
                        new TimeoutCandidateInfo(mInitialEntries[i], mInitialValues[i].toString(),
                                true));
            }
        }
        return candidates;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdaptiveSleepPermissionController.updateVisibility();
        mAdaptiveSleepController.updatePreference();
    }

    @Override
    public void updateCandidates() {
        final String defaultKey = getDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList == null) {
            return;
        }

        for (CandidateInfo info : candidateList) {
            RadioButtonPreferenceWithExtraWidget pref =
                    new RadioButtonPreferenceWithExtraWidget(getPrefContext());
            bindPreference(pref, info.getKey(), info, defaultKey);
            screen.addPreference(pref);
        }

        mAdaptiveSleepPermissionController.addToScreen(screen);
        mAdaptiveSleepController.addToScreen(screen);
        screen.addPreference(mPrivacyPreference);
    }

    @Override
    protected String getDefaultKey() {
        return getCurrentSystemScreenTimeout(getContext());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        setCurrentSystemScreenTimeout(getContext(), key);
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_TIMEOUT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_timeout_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_adaptive_sleep;
    }

    private Long getMaxScreenTimeout(Context context) {
        final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        if (dpm == null) {
            return Long.MAX_VALUE;
        }

        final RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtilsInternal.checkIfMaximumTimeToLockIsSet(context);
        if (admin != null) {
            return dpm.getMaximumTimeToLock(null /* admin */, UserHandle.myUserId());
        }
        return Long.MAX_VALUE;
    }

    private String getCurrentSystemScreenTimeout(Context context) {
        if (context == null) {
            return Long.toString(FALLBACK_SCREEN_TIMEOUT_VALUE);
        } else {
            return Long.toString(Settings.System.getLong(context.getContentResolver(),
                    SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE));
        }
    }

    private void setCurrentSystemScreenTimeout(Context context, String key) {
        try {
            if (context != null) {
                final long value = Long.parseLong(key);
                Settings.System.putLong(context.getContentResolver(), SCREEN_OFF_TIMEOUT, value);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist screen timeout setting", e);
        }
    }

    private static class TimeoutCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        TimeoutCandidateInfo(CharSequence label, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_timeout_settings) {
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final Resources res = context.getResources();
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.adaptive_sleep_title);
                    data.key = AdaptiveSleepPreferenceController.PREFERENCE_KEY;
                    data.screenTitle = res.getString(R.string.screen_timeout_title);
                    data.keywords = res.getString(R.string.adaptive_sleep_title);

                    final List<SearchIndexableRaw> result = new ArrayList<>(1);
                    result.add(data);
                    return result;
                }
            };
}
