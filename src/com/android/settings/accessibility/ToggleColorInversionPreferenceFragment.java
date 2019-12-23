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

package com.android.settings.accessibility;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.search.SearchIndexable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/** Settings page for color inversion. */
@SearchIndexable
public class ToggleColorInversionPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, ShortcutPreference.OnClickListener {

    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;
    private static final String PREVIEW_PREFERENCE_KEY = "color_inversion_preview";
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    private static final int DIALOG_ID_EDIT_SHORTCUT = 1;
    private static final String DISPLAY_INVERSION_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;
    private final DialogInterface.OnClickListener mDialogListener =
            (DialogInterface dialog, int id) -> {
                if (id == DialogInterface.BUTTON_POSITIVE) {
                    // TODO(b/142531156): Save the shortcut type preference.
                }
            };
    private final Handler mHandler = new Handler();
    private Dialog mDialog;
    private SettingsContentObserver mSettingsContentObserver;

    @Override
    public void onStart() {
        super.onStart();
        mSettingsContentObserver.register(getContentResolver());
    }

    @Override
    public void onStop() {
        mSettingsContentObserver.unregister(getContentResolver());
        super.onStop();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? State.OFF : State.ON);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_color_inversion_settings;
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        final String switchBarText = getString(
                R.string.accessibility_display_inversion_switch_title,
                getString(R.string.accessibility_display_inversion_switch_title));
        switchBar.setSwitchBarText(switchBarText, switchBarText);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.Secure.putInt(getContentResolver(), ENABLED, isChecked ? State.ON : State.OFF);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mSwitchBar.setCheckedInternal(
                Settings.Secure.getInt(getContentResolver(), ENABLED, State.OFF) == State.ON);
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        initShortcutPreference();
        final List<String> shortcutFeatureKeys = new ArrayList<>(1);
        shortcutFeatureKeys.add(DISPLAY_INVERSION_ENABLED);
        mSettingsContentObserver = new SettingsContentObserver(mHandler, shortcutFeatureKeys) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                mSwitchBar.setCheckedInternal(
                        Settings.Secure.getInt(getContentResolver(), ENABLED, State.OFF)
                                == State.ON);
            }
        };
        return super.onCreateView(inflater, container, savedInstanceState);
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
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_SHORTCUT) {
            return SettingsEnums.DIALOG_COLOR_INVERSION_EDIT_SHORTCUT;
        }
        return 0;
    }

    private void initShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ShortcutPreference shortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        final Preference previewPreference = findPreference(PREVIEW_PREFERENCE_KEY);
        // Put the shortcutPreference before radioButtonPreference.
        shortcutPreference.setPersistent(false);
        shortcutPreference.setKey(getShortcutPreferenceKey());
        shortcutPreference.setOrder(previewPreference.getOrder() - 1);
        shortcutPreference.setTitle(R.string.accessibility_shortcut_title);
        shortcutPreference.setOnClickListener(this);
        // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
        // TODO(b/142530063): Check if gesture mode is on to decide which summary should be shown.
        // TODO(b/142530063): Check the new key to decide whether checkbox should be checked.
        preferenceScreen.addPreference(shortcutPreference);
    }

    public String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        if (preference.getChecked()) {
            // TODO(b/142530063): Enable shortcut when checkbox is checked.
        } else {
            // TODO(b/142530063): Disable shortcut when checkbox is unchecked.
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        showDialog(DIALOG_ID_EDIT_SHORTCUT);
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
        int OFF = 0;
        int ON = 1;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_color_inversion_settings);
}
