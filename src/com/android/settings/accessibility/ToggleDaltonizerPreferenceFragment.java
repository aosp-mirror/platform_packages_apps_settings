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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public final class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements DaltonizerRadioButtonPreferenceController.OnChangeListener,
        SwitchBar.OnSwitchChangeListener {

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_daltonizer_settings);
    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String RADIOPREFERENCE_KEY = "daltonizer_mode_deuteranomaly";
    private static final int DIALOG_ID_EDIT_SHORTCUT = 1;
    private static final List<AbstractPreferenceController> sControllers = new ArrayList<>();

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        if (sControllers.size() == 0) {
            final Resources resources = context.getResources();
            final String[] daltonizerKeys = resources.getStringArray(
                    R.array.daltonizer_mode_keys);

            for (int i = 0; i < daltonizerKeys.length; i++) {
                sControllers.add(new DaltonizerRadioButtonPreferenceController(
                        context, lifecycle, daltonizerKeys[i]));
            }
        }
        return sControllers;
    }

    private final DialogInterface.OnClickListener mDialogListener =
            (DialogInterface dialog, int id) -> {
                if (id == DialogInterface.BUTTON_POSITIVE) {
                    // TODO(b/142531156): Save the shortcut type preference.
                }
            };

    private final View.OnClickListener mSettingButtonListener =
            (View view) -> showDialog(DIALOG_ID_EDIT_SHORTCUT);

    private final View.OnClickListener mCheckBoxListener = (View view) -> {
        CheckBox checkBox = (CheckBox) view;
        if (checkBox.isChecked()) {
            // TODO(b/142530063): Enable shortcut when checkbox is checked.
        } else {
            // TODO(b/142530063): Disable shortcut when checkbox is unchecked.
        }
    };

    private Dialog mDialog;

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AbstractPreferenceController controller : sControllers) {
            controller.updateState(preference);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        initShortcutPreference();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerRadioButtonPreferenceController) controller).setOnChangeListener(this);
            ((DaltonizerRadioButtonPreferenceController) controller).displayPreference(
                    getPreferenceScreen());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (AbstractPreferenceController controller :
                buildPreferenceControllers(getPrefContext(), getSettingsLifecycle())) {
            ((DaltonizerRadioButtonPreferenceController) controller).setOnChangeListener(null);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_SHORTCUT) {
            final CharSequence dialogTitle = getActivity().getString(
                    R.string.accessibility_shortcut_edit_dialog_title_daltonizer);
            mDialog = AccessibilityEditDialogUtils.showEditShortcutDialog(getActivity(),
                    dialogTitle, mDialogListener);
        }

        return mDialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_SHORTCUT) {
            return SettingsEnums.DIALOG_DALTONIZER_EDIT_SHORTCUT;
        }
        return 0;
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

    private void initShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ShortcutPreference shortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        final RadioButtonPreference radioButtonPreference = findPreference(RADIOPREFERENCE_KEY);
        // Put the shortcutPreference before radioButtonPreference.
        shortcutPreference.setOrder(radioButtonPreference.getOrder() - 1);
        shortcutPreference.setTitle(R.string.accessibility_shortcut_title);
        // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
        // TODO(b/142530063): Check if gesture mode is on to decide which summary should be shown.
        // TODO(b/142530063): Check the new key to decide whether checkbox should be checked.
        shortcutPreference.setSettingButtonListener(mSettingButtonListener);
        shortcutPreference.setCheckBoxListener(mCheckBoxListener);
        preferenceScreen.addPreference(shortcutPreference);
    }
}
