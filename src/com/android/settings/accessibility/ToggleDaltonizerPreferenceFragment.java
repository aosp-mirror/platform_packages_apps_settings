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
import android.content.res.Resources;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public final class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements DaltonizerPreferenceController.OnChangeListener,
        SwitchBar.OnSwitchChangeListener {

    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;

    private static final List<AbstractPreferenceController> sControllers = new ArrayList<>();

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AbstractPreferenceController controller : sControllers) {
            controller.updateState(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerPreferenceController) controller).setOnChangeListener(this);
            ((DaltonizerPreferenceController) controller).displayPreference(getPreferenceScreen());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerPreferenceController) controller).setOnChangeListener(null);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_color_correction;
    }


    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_daltonizer_settings;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? 0 : 1);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        final String switchBarText = getString(R.string.accessibility_service_master_switch_title,
                getString(R.string.accessibility_display_daltonizer_preference_title));
        switchBar.setSwitchBarText(switchBarText, switchBarText);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, isChecked ? 1 : 0);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mSwitchBar.setCheckedInternal(
                Settings.Secure.getInt(getContentResolver(), ENABLED, 0) == 1);
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        if (sControllers.size() == 0) {
            final Resources resources = context.getResources();
            final String[] daltonizerKeys = resources.getStringArray(
                    R.array.daltonizer_mode_keys);

            for (int i = 0; i < daltonizerKeys.length; i++) {
                sControllers.add(new DaltonizerPreferenceController(
                        context, lifecycle, daltonizerKeys[i]));
            }
        }
        return sControllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_daltonizer_settings);
}
