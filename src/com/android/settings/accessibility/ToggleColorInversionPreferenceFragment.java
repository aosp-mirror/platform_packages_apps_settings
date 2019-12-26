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

import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.PreferredShortcutType;
import com.android.settings.accessibility.AccessibilityUtil.State;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.search.SearchIndexable;

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
    private static final String EXTRA_SHORTCUT_TYPE = "shortcutType";
    // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
    private static final String KEY_SHORTCUT_TYPE = Settings.System.MASTER_MONO;
    private final Handler mHandler = new Handler();
    private ShortcutPreference mShortcutPreference;
    private SettingsContentObserver mSettingsContentObserver;
    private int mPreferredShortcutType = PreferredShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;

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
        initShortcutPreference(savedInstanceState);
        final List<String> enableServiceFeatureKeys = new ArrayList<>(/* initialCapacity= */ 1);
        enableServiceFeatureKeys.add(DISPLAY_INVERSION_ENABLED);
        mSettingsContentObserver = new SettingsContentObserver(mHandler, enableServiceFeatureKeys) {
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mPreferredShortcutType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateShortcutPreference();
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_SHORTCUT) {
            final CharSequence dialogTitle = getActivity().getString(
                    R.string.accessibility_shortcut_edit_dialog_title_daltonizer);
            final AlertDialog dialog = AccessibilityEditDialogUtils.showEditShortcutDialog(
                    getActivity(),
                    dialogTitle, this::callOnAlertDialogCheckboxClicked);
            initializeDialogCheckBox(dialog);
            return dialog;
        }
        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    private void initializeDialogCheckBox(AlertDialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        updateAlertDialogCheckState();
        updateAlertDialogEnableState();
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mSoftwareTypeCheckBox, PreferredShortcutType.SOFTWARE);
        updateCheckStatus(mHardwareTypeCheckBox, PreferredShortcutType.HARDWARE);
    }

    private void updateAlertDialogEnableState() {
        if (!mSoftwareTypeCheckBox.isChecked()) {
            mHardwareTypeCheckBox.setEnabled(false);
        } else if (!mHardwareTypeCheckBox.isChecked()) {
            mSoftwareTypeCheckBox.setEnabled(false);
        } else {
            mSoftwareTypeCheckBox.setEnabled(true);
            mHardwareTypeCheckBox.setEnabled(true);
        }
    }

    private void updateCheckStatus(CheckBox checkBox, @PreferredShortcutType int type) {
        checkBox.setChecked((mPreferredShortcutType & type) == type);
        checkBox.setOnClickListener(v -> {
            updatePreferredShortcutType(false);
            updateAlertDialogEnableState();
        });
    }

    private void updatePreferredShortcutType(boolean saveToDB) {
        mPreferredShortcutType = PreferredShortcutType.DEFAULT;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mPreferredShortcutType |= PreferredShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mPreferredShortcutType |= PreferredShortcutType.HARDWARE;
        }
        if (saveToDB) {
            setPreferredShortcutType(mPreferredShortcutType);
        }
    }

    private void setSecureIntValue(String key, @PreferredShortcutType int value) {
        Settings.Secure.putIntForUser(getPrefContext().getContentResolver(),
                key, value, getPrefContext().getContentResolver().getUserId());
    }

    private void setPreferredShortcutType(@PreferredShortcutType int type) {
        setSecureIntValue(KEY_SHORTCUT_TYPE, type);
    }

    private String getShortcutTypeSummary(Context context) {
        final int shortcutType = getPreferredShortcutType(context);
        final CharSequence softwareTitle =
                context.getText(AccessibilityUtil.isGestureNavigateEnabled(context)
                        ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture
                        : R.string.accessibility_shortcut_edit_dialog_title_software);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & PreferredShortcutType.SOFTWARE) == PreferredShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & PreferredShortcutType.HARDWARE) == PreferredShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_edit_dialog_title_hardware);
            list.add(hardwareTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }
        final String joinStrings = TextUtils.join(/* delimiter= */", ", list);
        return AccessibilityUtil.capitalize(joinStrings);
    }

    @PreferredShortcutType
    private int getPreferredShortcutType(Context context) {
        return getSecureIntValue(context, KEY_SHORTCUT_TYPE, PreferredShortcutType.SOFTWARE);
    }

    @PreferredShortcutType
    private int getSecureIntValue(Context context, String key,
            @PreferredShortcutType int defaultValue) {
        return Settings.Secure.getIntForUser(
                context.getContentResolver(),
                key, defaultValue, context.getContentResolver().getUserId());
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updatePreferredShortcutType(true);
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_ID_EDIT_SHORTCUT) {
            return SettingsEnums.DIALOG_COLOR_INVERSION_EDIT_SHORTCUT;
        }
        return 0;
    }

    private void initShortcutPreference(Bundle savedInstanceState) {
        // Restore the PreferredShortcut type
        if (savedInstanceState != null) {
            mPreferredShortcutType = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE,
                    PreferredShortcutType.DEFAULT);
        }
        if (mPreferredShortcutType == PreferredShortcutType.DEFAULT) {
            mPreferredShortcutType = getPreferredShortcutType(getPrefContext());
        }

        // Initial ShortcutPreference widget
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mShortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        final Preference previewPreference = findPreference(PREVIEW_PREFERENCE_KEY);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setTitle(R.string.accessibility_shortcut_title);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        mShortcutPreference.setOnClickListener(this);
        // Put the shortcutPreference before previewPreference.
        mShortcutPreference.setOrder(previewPreference.getOrder() - 1);
        // TODO(b/142530063): Check the new key to decide whether checkbox should be checked.
        preferenceScreen.addPreference(mShortcutPreference);
    }

    private void updateShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ShortcutPreference shortcutPreference = preferenceScreen.findPreference(
                getShortcutPreferenceKey());

        if (shortcutPreference != null) {
            // TODO(b/142531156): Replace PreferredShortcutType.SOFTWARE value with dialog shortcut
            //  preferred key.
            shortcutPreference.setChecked(
                    AccessibilityUtil.hasValueInSettings(getContext(),
                            PreferredShortcutType.SOFTWARE,
                            getComponentName()));
        }
    }

    private String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    private ComponentName getComponentName() {
        return COLOR_INVERSION_COMPONENT_NAME;
    }

    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        if (preference.getChecked()) {
            // TODO(b/142531156): Replace PreferredShortcutType.SOFTWARE value with dialog shortcut
            //  preferred key.
            AccessibilityUtil.optInValueToSettings(getContext(), PreferredShortcutType.SOFTWARE,
                    getComponentName());
        } else {
            // TODO(b/142531156): Replace PreferredShortcutType.SOFTWARE value with dialog shortcut
            //  preferred key.
            AccessibilityUtil.optOutValueFromSettings(getContext(), PreferredShortcutType.SOFTWARE,
                    getComponentName());
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        mPreferredShortcutType = getPreferredShortcutType(getPrefContext());
        showDialog(DIALOG_ID_EDIT_SHORTCUT);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_color_inversion_settings);
}
