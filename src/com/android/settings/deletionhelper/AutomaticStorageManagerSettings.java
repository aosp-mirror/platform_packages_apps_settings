/**
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deletionhelper;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * AutomaticStorageManagerSettings is the Settings screen for configuration and management of the
 * automatic storage manager.
 */
@SearchIndexable
public class AutomaticStorageManagerSettings extends DashboardFragment
        implements OnPreferenceChangeListener {
    private static final String KEY_DAYS = "days";

    private AutomaticStorageManagerSwitchBarController mSwitchController;
    private DropDownPreference mDaysToRetain;
    private SwitchBar mSwitchBar;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        initializeDaysToRetainPreference();
        initializeSwitchBar();

        return view;
    }

    private void initializeDaysToRetainPreference() {
        mDaysToRetain = (DropDownPreference) findPreference(KEY_DAYS);
        mDaysToRetain.setOnPreferenceChangeListener(this);

        ContentResolver cr = getContentResolver();
        int photosDaysToRetain =
                Settings.Secure.getInt(
                        cr,
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                        Utils.getDefaultStorageManagerDaysToRetain(getResources()));
        String[] stringValues =
                getResources().getStringArray(R.array.automatic_storage_management_days_values);
        mDaysToRetain.setValue(stringValues[daysValueToIndex(photosDaysToRetain, stringValues)]);
    }

    private void initializeSwitchBar() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.setSwitchBarText(R.string.automatic_storage_manager_master_switch_title,
                R.string.automatic_storage_manager_master_switch_title);
        mSwitchBar.show();
        mSwitchController =
                new AutomaticStorageManagerSwitchBarController(
                        getContext(),
                        mSwitchBar,
                        mMetricsFeatureProvider,
                        mDaysToRetain,
                        getFragmentManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        mDaysToRetain.setEnabled(Utils.isStorageManagerEnabled(getContext()));
    }

    @Override
    protected String getLogTag() {
        return null;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.automatic_storage_management_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.hide();
        mSwitchController.tearDown();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_DAYS.equals(preference.getKey())) {
            Settings.Secure.putInt(
                    getContentResolver(),
                    Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                    Integer.parseInt((String) newValue));
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.STORAGE_MANAGER_SETTINGS;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_storage;
    }

    private static int daysValueToIndex(int value, String[] indices) {
        for (int i = 0; i < indices.length; i++) {
            int thisValue = Integer.parseInt(indices[i]);
            if (value == thisValue) {
                return i;
            }
        }
        return indices.length - 1;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AutomaticStorageManagerDescriptionPreferenceController(context));
        return controllers;
    }

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }
            };
}
