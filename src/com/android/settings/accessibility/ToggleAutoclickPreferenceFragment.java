/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SeekBarPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for preference screen for settings related to Automatically click after mouse stops
 * feature.
 */
@SearchIndexable
public class ToggleAutoclickPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceChangeListener {

    /** Min allowed autoclick delay value. */
    private static final int MIN_AUTOCLICK_DELAY = 200;
    /** Max allowed autoclick delay value. */
    private static final int MAX_AUTOCLICK_DELAY = 1000;
    /**
     * Allowed autoclick delay values are discrete. This is the difference between two allowed
     * values.
     */
    private static final int AUTOCLICK_DELAY_STEP = 100;

    /**
     * Resource ids from which autoclick preference summaries should be derived. The strings have
     * placeholder for integer delay value.
     */
    private static final int[] mAutoclickPreferenceSummaries = {
            R.plurals.accessibilty_autoclick_preference_subtitle_extremely_short_delay,
            R.plurals.accessibilty_autoclick_preference_subtitle_very_short_delay,
            R.plurals.accessibilty_autoclick_preference_subtitle_short_delay,
            R.plurals.accessibilty_autoclick_preference_subtitle_long_delay,
            R.plurals.accessibilty_autoclick_preference_subtitle_very_long_delay
    };

    /**
     * Seek bar preference for autoclick delay value. The seek bar has values between 0 and
     * number of possible discrete autoclick delay values. These will have to be converted to actual
     * delay values before saving them in settings.
     */
    private SeekBarPreference mDelay;

    /**
     * Gets string that should be used as a autoclick preference summary for provided autoclick
     * delay.
     * @param resources Resources from which string should be retrieved.
     * @param delay Delay for whose value summary should be retrieved.
     */
    static CharSequence getAutoclickPreferenceSummary(Resources resources, int delay) {
        int summaryIndex = getAutoclickPreferenceSummaryIndex(delay);
        return resources.getQuantityString(
                mAutoclickPreferenceSummaries[summaryIndex], delay, delay);
    }

    /**
     * Finds index of the summary that should be used for the provided autoclick delay.
     */
    private static int getAutoclickPreferenceSummaryIndex(int delay) {
        if (delay <= MIN_AUTOCLICK_DELAY) {
            return 0;
        }
        if (delay >= MAX_AUTOCLICK_DELAY) {
            return mAutoclickPreferenceSummaries.length - 1;
        }
        int rangeSize = (MAX_AUTOCLICK_DELAY - MIN_AUTOCLICK_DELAY) /
                (mAutoclickPreferenceSummaries.length - 1);
        return (delay - MIN_AUTOCLICK_DELAY) / rangeSize;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), preferenceKey, enabled ? 1 : 0);
        mDelay.setEnabled(enabled);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_autoclick;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_autoclick_settings;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int delay = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_DEFAULT);

        // Initialize seek bar preference. Sets seek bar size to the number of possible delay
        // values.
        mDelay = (SeekBarPreference) findPreference("autoclick_delay");
        mDelay.setMax(delayToSeekBarProgress(MAX_AUTOCLICK_DELAY));
        mDelay.setProgress(delayToSeekBarProgress(delay));
        mDelay.setOnPreferenceChangeListener(this);
        mFooterPreferenceMixin.createFooterPreference()
                .setTitle(R.string.accessibility_autoclick_description);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();

        int value = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 0);
        mSwitchBar.setCheckedInternal(value == 1);
        mSwitchBar.addOnSwitchChangeListener(this);
        mDelay.setEnabled(value == 1);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        onPreferenceToggled(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, isChecked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDelay && newValue instanceof Integer) {
            Settings.Secure.putInt(getContentResolver(),
                   Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                   seekBarProgressToDelay((int)newValue));
            return true;
         }
         return false;
    }

    /**
     * Converts seek bar preference progress value to autoclick delay associated with it.
     */
    private int seekBarProgressToDelay(int progress) {
        return progress * AUTOCLICK_DELAY_STEP + MIN_AUTOCLICK_DELAY;
    }

    /**
     * Converts autoclick delay value to seek bar preference progress values that represents said
     * delay.
     */
    private int delayToSeekBarProgress(int delay) {
        return (delay - MIN_AUTOCLICK_DELAY) / AUTOCLICK_DELAY_STEP;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.accessibility_autoclick_settings;
                    result.add(sir);
                    return result;
                }
            };
}
