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

import static com.android.settings.accessibility.ToggleAutoclickCustomSeekbarController.MAX_AUTOCLICK_DELAY_MS;
import static com.android.settings.accessibility.ToggleAutoclickCustomSeekbarController.MIN_AUTOCLICK_DELAY_MS;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.IntDef;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for preference screen for settings related to Automatically click after mouse stops
 * feature.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleAutoclickPreferenceFragment extends DashboardFragment
        implements ToggleAutoclickPreferenceController.OnChangeListener {

    private static final String TAG = "AutoclickPrefFragment";
    private static final List<AbstractPreferenceController> sControllers = new ArrayList<>();

    @Retention(SOURCE)
    @IntDef({
            Quantity.OTHER,
            Quantity.ONE,
            Quantity.FEW
    })
    @interface Quantity {
        int OTHER = 0;
        int ONE = 1;
        int FEW = 3;
    }

    /**
     * Resource ids from which autoclick preference summaries should be derived. The strings have
     * placeholder for integer delay value.
     */
    private static final int[] AUTOCLICK_PREFERENCE_SUMMARIES = {
            R.plurals.accessibilty_autoclick_preference_subtitle_short_delay,
            R.plurals.accessibilty_autoclick_preference_subtitle_medium_delay,
            R.plurals.accessibilty_autoclick_preference_subtitle_long_delay
    };

    /**
     * Gets string that should be used as a autoclick preference summary for provided autoclick
     * delay.
     *
     * @param resources Resources from which string should be retrieved.
     * @param delayMillis Delay for whose value summary should be retrieved.
     */
    static CharSequence getAutoclickPreferenceSummary(Resources resources, int delayMillis) {
        final int summaryIndex = getAutoclickPreferenceSummaryIndex(delayMillis);
        final int quantity = (delayMillis == 1000) ? Quantity.ONE : Quantity.FEW;
        final float delaySecond =  (float) delayMillis / 1000;
        // Only show integer when delay time is 1.
        final String decimalFormat = (delaySecond == 1) ? "%.0f" : "%.1f";

        return resources.getQuantityString(AUTOCLICK_PREFERENCE_SUMMARIES[summaryIndex],
                quantity, String.format(decimalFormat, delaySecond));
    }

    /**
     * Finds index of the summary that should be used for the provided autoclick delay.
     */
    private static int getAutoclickPreferenceSummaryIndex(int delay) {
        if (delay <= MIN_AUTOCLICK_DELAY_MS) {
            return 0;
        }
        if (delay >= MAX_AUTOCLICK_DELAY_MS) {
            return AUTOCLICK_PREFERENCE_SUMMARIES.length - 1;
        }
        int delayRange = MAX_AUTOCLICK_DELAY_MS - MIN_AUTOCLICK_DELAY_MS;
        int rangeSize = (delayRange) / (AUTOCLICK_PREFERENCE_SUMMARIES.length - 1);
        return (delay - MIN_AUTOCLICK_DELAY_MS) / rangeSize;
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
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_autoclick_settings;
    }

    @Override
    public void onResume() {
        super.onResume();

        for (AbstractPreferenceController controller : sControllers) {
            ((ToggleAutoclickPreferenceController) controller).setOnChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        for (AbstractPreferenceController controller : sControllers) {
            ((ToggleAutoclickPreferenceController) controller).setOnChangeListener(null);
        }
    }

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AbstractPreferenceController controller : sControllers) {
            controller.updateState(preference);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        Resources resources = context.getResources();

        String[] autoclickKeys = resources.getStringArray(
                R.array.accessibility_autoclick_control_selector_keys);

        final int length = autoclickKeys.length;
        for (int i = 0; i < length; i++) {
            sControllers.add(new ToggleAutoclickPreferenceController(
                    context, lifecycle, autoclickKeys[i]));
        }
        return sControllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_autoclick_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}
