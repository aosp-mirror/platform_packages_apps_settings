/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.hardware.display.ColorDisplayManager;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String TYPE = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER;
    private static final int DEFAULT_TYPE = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY;

    private ListPreference mType;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_color_correction;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mType = (ListPreference) findPreference("type");

        if (!ColorDisplayManager.isColorTransformAccelerated(getActivity())) {
            mFooterPreferenceMixin.createFooterPreference().setTitle(
                    R.string.accessibility_display_daltonizer_preference_subtitle);
        }
        initPreferences();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_daltonizer_settings;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? 1 : 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mType) {
            Settings.Secure.putInt(getContentResolver(), TYPE, Integer.parseInt((String) newValue));
            preference.setSummary("%s");
        }

        return true;
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();

        mSwitchBar.setCheckedInternal(
                Settings.Secure.getInt(getContentResolver(), ENABLED, 0) == 1);
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        switchBar.setSwitchBarText(R.string.accessibility_daltonizer_master_switch_title,
                R.string.accessibility_daltonizer_master_switch_title);
    }

    private void initPreferences() {
        final String value = Integer.toString(
                Settings.Secure.getInt(getContentResolver(), TYPE, DEFAULT_TYPE));
        mType.setValue(value);
        mType.setOnPreferenceChangeListener(this);
        final int index = mType.findIndexOfValue(value);
        if (index < 0) {
            // We're using a mode controlled by developer preferences.
            mType.setSummary(getString(R.string.daltonizer_type_overridden,
                    getString(R.string.simulate_color_space)));
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        onPreferenceToggled(mPreferenceKey, isChecked);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.accessibility_daltonizer_settings;
                    result.add(sir);
                    return result;
                }
            };

}
