/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display.darkmode;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for Dark UI Mode
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class DarkModeSettingsFragment extends DashboardFragment {

    private static final String TAG = "DarkModeSettingsFrag";
    private static final String DARK_THEME_END_TIME = "dark_theme_end_time";
    private static final String DARK_THEME_START_TIME = "dark_theme_start_time";
    private DarkModeObserver mContentObserver;
    private DarkModeCustomPreferenceController mCustomStartController;
    private DarkModeCustomPreferenceController mCustomEndController;
    private Runnable mCallback = () -> {
        updatePreferenceStates();
    };
    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getContext();
        mContentObserver = new DarkModeObserver(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Listen for changes only while visible.
        mContentObserver.subscribe(mCallback);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers =  new ArrayList(2);
        mCustomStartController = new DarkModeCustomPreferenceController(getContext(),
                DARK_THEME_START_TIME, this);
        mCustomEndController = new DarkModeCustomPreferenceController(getContext(),
                DARK_THEME_END_TIME, this);
        controllers.add(mCustomStartController);
        controllers.add(mCustomEndController);
        return controllers;
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop listening for state changes.
        mContentObserver.unsubscribe();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (DARK_THEME_END_TIME.equals(preference.getKey())) {
            showDialog(DIALOG_END_TIME);
            return true;
        } else if (DARK_THEME_START_TIME.equals(preference.getKey())) {
            showDialog(DIALOG_START_TIME);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    public void refresh() {
        this.updatePreferenceStates();
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        if (dialogId == DIALOG_START_TIME || dialogId == DIALOG_END_TIME) {
            if (dialogId == DIALOG_START_TIME) {
                return mCustomStartController.getDialog();
            } else {
                return mCustomEndController.getDialog();
            }
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dark_mode_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_dark_theme;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DARK_UI_SETTINGS;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_START_TIME:
                return SettingsEnums.DIALOG_DARK_THEME_SET_START_TIME;
            case DIALOG_END_TIME:
                return SettingsEnums.DIALOG_DARK_THEME_SET_END_TIME;
            default:
                return 0;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.dark_mode_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return !context.getSystemService(PowerManager.class).isPowerSaveMode();
                }
            };

}
