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

import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
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
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.State;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SearchIndexable
public final class ToggleDaltonizerPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements DaltonizerRadioButtonPreferenceController.OnChangeListener,
        SwitchBar.OnSwitchChangeListener, ShortcutPreference.OnClickListener {

    private static final String ENABLED = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED;
    private static final String CATEGORY_FOOTER_KEY = "daltonizer_footer_category";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcut_type";
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    private static final int DIALOG_ID_EDIT_SHORTCUT = 1;
    private static final List<AbstractPreferenceController> sControllers = new ArrayList<>();
    private final Handler mHandler = new Handler();
    private SettingsContentObserver mSettingsContentObserver;
    private int mUserShortcutType = UserShortcutType.DEFAULT;
    // Used to restore the edit dialog status.
    private int mUserShortcutTypeCache = UserShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;

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

    @Override
    public void onCheckedChanged(Preference preference) {
        for (AbstractPreferenceController controller : sControllers) {
            controller.updateState(preference);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final List<String> enableServiceFeatureKeys = new ArrayList<>(/* initialCapacity= */ 1);
        enableServiceFeatureKeys.add(ENABLED);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Restore the user shortcut type.
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SHORTCUT_TYPE)) {
            mUserShortcutTypeCache = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE,
                    UserShortcutType.DEFAULT);
        }
        initShortcutPreference();

        super.onViewCreated(view, savedInstanceState);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        final PreferenceCategory footerCategory = preferenceScreen.findPreference(
                CATEGORY_FOOTER_KEY);
        updateFooterTitle(footerCategory);
        footerCategory.setOrder(Integer.MAX_VALUE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mUserShortcutTypeCache);
        super.onSaveInstanceState(outState);
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
        updateShortcutPreferenceData();
        updateShortcutPreference();
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
        updateCheckStatus(mSoftwareTypeCheckBox, UserShortcutType.SOFTWARE);
        updateCheckStatus(mHardwareTypeCheckBox, UserShortcutType.HARDWARE);
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

    private void updateCheckStatus(CheckBox checkBox, @UserShortcutType int type) {
        checkBox.setChecked((mUserShortcutTypeCache & type) == type);
        checkBox.setOnClickListener(v -> {
            updateUserShortcutType(/* saveChanges= */ false);
            updateAlertDialogEnableState();
        });
    }

    private void updateUserShortcutType(boolean saveChanges) {
        mUserShortcutTypeCache = UserShortcutType.DEFAULT;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mUserShortcutTypeCache |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mUserShortcutTypeCache |= UserShortcutType.HARDWARE;
        }
        if (saveChanges) {
            mUserShortcutType = mUserShortcutTypeCache;
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        }
    }

    private void setUserShortcutType(Context context, int type) {
        Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
        if (info.isEmpty()) {
            info = new HashSet<>();
        } else {
            final Set<String> filtered = info.stream().filter(
                    str -> str.contains(getComponentName().flattenToString())).collect(
                    Collectors.toSet());
            info.removeAll(filtered);
        }
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                getComponentName().flattenToString(), type);
        info.add(shortcut.flattenToString());
        SharedPreferenceUtils.setUserShortcutType(context, info);
    }

    private String getShortcutTypeSummary(Context context) {
        final int shortcutType = getUserShortcutType(context, UserShortcutType.SOFTWARE);
        final CharSequence softwareTitle =
                context.getText(AccessibilityUtil.isGestureNavigateEnabled(context)
                        ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture
                        : R.string.accessibility_shortcut_edit_dialog_title_software);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE) {
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

    private int getUserShortcutType(Context context, @UserShortcutType int defaultValue) {
        final Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
        final String componentName = getComponentName().flattenToString();
        final Set<String> filtered = info.stream().filter(
                str -> str.contains(componentName)).collect(
                Collectors.toSet());
        if (filtered.isEmpty()) {
            return defaultValue;
        }

        final String str = (String) filtered.toArray()[0];
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(str);
        return shortcut.getUserShortcutType();
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateUserShortcutType(/* saveChanges= */ true);
        if (mShortcutPreference.getChecked()) {
            AccessibilityUtil.optInAllValuesToSettings(getContext(), mUserShortcutType,
                    getComponentName());
            AccessibilityUtil.optOutAllValuesFromSettings(getContext(), ~mUserShortcutType,
                    getComponentName());
        }
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

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
        Settings.Secure.putInt(getContentResolver(), ENABLED, enabled ? State.OFF : State.ON);
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
    protected void updateFooterTitle(PreferenceCategory category) {
        final String titleText = getString(R.string.accessibility_footer_title,
                        getString(R.string.accessibility_display_daltonizer_preference_title));
        category.setTitle(titleText);
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
    public void onCheckboxClicked(ShortcutPreference preference) {
        final int shortcutTypes = getUserShortcutType(getContext(), UserShortcutType.SOFTWARE);
        if (preference.getChecked()) {
            AccessibilityUtil.optInAllValuesToSettings(getContext(), shortcutTypes,
                    getComponentName());
        } else {
            AccessibilityUtil.optOutAllValuesFromSettings(getContext(), shortcutTypes,
                    getComponentName());
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        mUserShortcutTypeCache = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
        showDialog(DIALOG_ID_EDIT_SHORTCUT);
    }

    private void initShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mShortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setTitle(R.string.accessibility_shortcut_title);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        mShortcutPreference.setOnClickListener(this);
    }

    private void updateShortcutPreferenceData() {
        // Get the user shortcut type from settings provider.
        mUserShortcutType = AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                getComponentName());
        if (mUserShortcutType != UserShortcutType.DEFAULT) {
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        } else {
            //  Get the user shortcut type from shared_prefs if cannot get from settings provider.
            mUserShortcutType = getUserShortcutType(getPrefContext(), UserShortcutType.SOFTWARE);
        }
    }

    private void updateShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ShortcutPreference shortcutPreference = preferenceScreen.findPreference(
                getShortcutPreferenceKey());

        if (shortcutPreference != null) {
            final int shortcutTypes = getUserShortcutType(getContext(), UserShortcutType.SOFTWARE);
            shortcutPreference.setChecked(
                    AccessibilityUtil.hasValuesInSettings(getContext(), shortcutTypes,
                            getComponentName()));
            shortcutPreference.setSummary(getShortcutTypeSummary(getContext()));
        }

    }

    private String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    private ComponentName getComponentName() {
        return DALTONIZER_COMPONENT_NAME;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_daltonizer_settings);
}
