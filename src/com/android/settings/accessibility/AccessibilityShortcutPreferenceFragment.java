/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_GENERAL_CATEGORY;
import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_SAVED_QS_TOOLTIP_TYPE;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.icu.text.CaseMap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.utils.LocaleUtils;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Base class for accessibility fragments shortcut functions and dialog management.
 */
public abstract class AccessibilityShortcutPreferenceFragment extends RestrictedDashboardFragment
        implements ShortcutPreference.OnClickCallback {
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    protected static final String KEY_SAVED_USER_SHORTCUT_TYPE = "shortcut_type";
    protected static final String KEY_SAVED_QS_TOOLTIP_RESHOW = "qs_tooltip_reshow";
    protected static final int NOT_SET = -1;
    // Save user's shortcutType value when savedInstance has value (e.g. device rotated).
    protected int mSavedCheckBoxValue = NOT_SET;

    protected ShortcutPreference mShortcutPreference;
    protected Dialog mDialog;
    private AccessibilityManager.TouchExplorationStateChangeListener
            mTouchExplorationStateChangeListener;
    private AccessibilitySettingsContentObserver mSettingsContentObserver;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;
    private AccessibilityQuickSettingsTooltipWindow mTooltipWindow;
    private boolean mNeedsQSTooltipReshow = false;
    private int mNeedsQSTooltipType = QuickSettingsTooltipType.GUIDE_TO_EDIT;

    public AccessibilityShortcutPreferenceFragment(String restrictionKey) {
        super(restrictionKey);
    }

    /** Returns the accessibility component name. */
    protected abstract ComponentName getComponentName();

    /** Returns the accessibility feature name. */
    protected abstract CharSequence getLabelName();

    /** Returns the accessibility tile component name. */
    protected abstract ComponentName getTileComponentName();

    /** Returns the accessibility tile tooltip content. */
    protected abstract CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore the user shortcut type and tooltip.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_SAVED_USER_SHORTCUT_TYPE)) {
                mSavedCheckBoxValue = savedInstanceState.getInt(KEY_SAVED_USER_SHORTCUT_TYPE,
                        NOT_SET);
            }
            if (savedInstanceState.containsKey(KEY_SAVED_QS_TOOLTIP_RESHOW)) {
                mNeedsQSTooltipReshow = savedInstanceState.getBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW);
            }
            if (savedInstanceState.containsKey(KEY_SAVED_QS_TOOLTIP_TYPE)) {
                mNeedsQSTooltipType = savedInstanceState.getInt(KEY_SAVED_QS_TOOLTIP_TYPE);
            }
        }

        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            final PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getPrefContext());
            setPreferenceScreen(preferenceScreen);
        }

        if (showGeneralCategory()) {
            initGeneralCategory();
        }

        final List<String> shortcutFeatureKeys = new ArrayList<>();
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
        mSettingsContentObserver = new AccessibilitySettingsContentObserver(new Handler());
        mSettingsContentObserver.registerKeysToObserverCallback(shortcutFeatureKeys, key -> {
            updateShortcutPreferenceData();
            updateShortcutPreference();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mShortcutPreference = new ShortcutPreference(getPrefContext(), /* attrs= */ null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setOnClickCallback(this);
        mShortcutPreference.setTitle(getShortcutTitle());

        getPreferenceScreen().addPreference(mShortcutPreference);

        mTouchExplorationStateChangeListener = isTouchExplorationEnabled -> {
            removeDialog(DialogEnums.EDIT_SHORTCUT);
            mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        };

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Reshow tooltip when activity recreate, such as rotate device.
        if (mNeedsQSTooltipReshow) {
            view.post(() -> {
                final Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    showQuickSettingsTooltipIfNeeded();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        mSettingsContentObserver.register(getContentResolver());
        updateShortcutPreferenceData();
        updateShortcutPreference();

        updateEditShortcutDialogIfNeeded();
    }

    @Override
    public void onPause() {
        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        final int value = getShortcutTypeCheckBoxValue();
        if (value != NOT_SET) {
            outState.putInt(KEY_SAVED_USER_SHORTCUT_TYPE, value);
        }
        final boolean isTooltipWindowShowing = mTooltipWindow != null && mTooltipWindow.isShowing();
        if (mNeedsQSTooltipReshow || isTooltipWindowShowing) {
            outState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, /* value= */ true);
            outState.putInt(KEY_SAVED_QS_TOOLTIP_TYPE, mNeedsQSTooltipType);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogEnums.EDIT_SHORTCUT:
                final int dialogType = WizardManagerHelper.isAnySetupWizard(getIntent())
                        ? AccessibilityDialogUtils.DialogType.EDIT_SHORTCUT_GENERIC_SUW :
                        AccessibilityDialogUtils.DialogType.EDIT_SHORTCUT_GENERIC;
                mDialog = AccessibilityDialogUtils.showEditShortcutDialog(
                        getPrefContext(), dialogType, getShortcutTitle(),
                        this::callOnAlertDialogCheckboxClicked);
                setupEditShortcutDialog(mDialog);
                return mDialog;
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
                    mDialog = AccessibilityGestureNavigationTutorial
                            .createAccessibilityTutorialDialogForSetupWizard(
                                    getPrefContext(), getUserShortcutTypes(),
                                    this::callOnTutorialDialogButtonClicked);
                } else {
                    mDialog = AccessibilityGestureNavigationTutorial
                            .createAccessibilityTutorialDialog(
                                    getPrefContext(), getUserShortcutTypes(),
                                    this::callOnTutorialDialogButtonClicked);
                }
                mDialog.setCanceledOnTouchOutside(false);
                return mDialog;
            default:
                throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
        }
    }

    protected CharSequence getShortcutTitle() {
        return getString(R.string.accessibility_shortcut_title, getLabelName());
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogEnums.EDIT_SHORTCUT:
                return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT;
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                return SettingsEnums.DIALOG_ACCESSIBILITY_TUTORIAL;
            default:
                return SettingsEnums.ACTION_UNKNOWN;
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        if (Flags.editShortcutsInFullScreen()) {
            EditShortcutsPreferenceFragment.showEditShortcutScreen(
                    getContext(),
                    getMetricsCategory(),
                    getShortcutTitle(),
                    getComponentName(),
                    getIntent()
            );
        } else {
            showDialog(DialogEnums.EDIT_SHORTCUT);
        }
    }

    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        if (getComponentName() == null) {
            return;
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(getPrefContext(),
                getComponentName().flattenToString());
        if (preference.isChecked()) {
            AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), shortcutTypes,
                    getComponentName());
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        } else {
            AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), shortcutTypes,
                    getComponentName());
        }
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    /**
     * Overrides to return specific shortcut preference key
     *
     * @return String The specific shortcut preference key
     */
    protected String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    @VisibleForTesting
    void setupEditShortcutDialog(Dialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogSoftwareView, mSoftwareTypeCheckBox);

        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogHardwareView, mHardwareTypeCheckBox);

        updateEditShortcutDialogCheckBox();
    }

    /**
     * Returns accumulated {@link AccessibilityUtil.UserShortcutType} checkbox value or
     * {@code NOT_SET} if checkboxes did not exist.
     */
    protected int getShortcutTypeCheckBoxValue() {
        if (mSoftwareTypeCheckBox == null || mHardwareTypeCheckBox == null) {
            return NOT_SET;
        }

        int value = AccessibilityUtil.UserShortcutType.EMPTY;
        if (mSoftwareTypeCheckBox.isChecked()) {
            value |= AccessibilityUtil.UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            value |= AccessibilityUtil.UserShortcutType.HARDWARE;
        }
        return value;
    }

    /**
     * Returns the shortcut type list which has been checked by user.
     */
    protected int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                getComponentName());
    };

    private static CharSequence getSoftwareShortcutTypeSummary(Context context) {
        int resId;
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software_gesture;
        } else {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        }
        return context.getText(resId);
    }

    /**
     * This method will be invoked when a button in the tutorial dialog is clicked.
     *
     * @param dialog The dialog that received the click
     * @param which  The button that was clicked
     */
    private void callOnTutorialDialogButtonClicked(DialogInterface dialog, int which) {
        dialog.dismiss();
        showQuickSettingsTooltipIfNeeded();
    }

    /**
     * This method will be invoked when a button in the edit shortcut dialog is clicked.
     *
     * @param dialog The dialog that received the click
     * @param which  The button that was clicked
     */
    protected void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        if (getComponentName() == null) {
            return;
        }

        final int value = getShortcutTypeCheckBoxValue();
        saveNonEmptyUserShortcutType(value);
        AccessibilityUtil.optInAllValuesToSettings(getPrefContext(), value, getComponentName());
        AccessibilityUtil.optOutAllValuesFromSettings(getPrefContext(), ~value, getComponentName());
        final boolean shortcutAssigned = value != AccessibilityUtil.UserShortcutType.EMPTY;
        mShortcutPreference.setChecked(shortcutAssigned);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));

        if (mHardwareTypeCheckBox.isChecked()) {
            AccessibilityUtil.skipVolumeShortcutDialogTimeoutRestriction(getPrefContext());
        }

        // Show the quick setting tooltip if the shortcut assigned in the first time
        if (shortcutAssigned) {
            showQuickSettingsTooltipIfNeeded();
        }
    }

    @VisibleForTesting
    void initGeneralCategory() {
        final PreferenceCategory generalCategory = new PreferenceCategory(getPrefContext());
        generalCategory.setKey(KEY_GENERAL_CATEGORY);
        generalCategory.setTitle(getGeneralCategoryDescription(null));

        getPreferenceScreen().addPreference(generalCategory);
    }

    private void updateEditShortcutDialogIfNeeded() {
        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }
        AccessibilityDialogUtils.updateShortcutInDialog(getContext(), mDialog);
    }

    @VisibleForTesting
    void saveNonEmptyUserShortcutType(int type) {
        if (type == AccessibilityUtil.UserShortcutType.EMPTY) {
            return;
        }

        final PreferredShortcut shortcut = new PreferredShortcut(
                getComponentName().flattenToString(), type);
        PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
    }

    /**
     * Overrides to return customized description for general category above shortcut
     *
     * @return CharSequence The customized description for general category
     */
    protected CharSequence getGeneralCategoryDescription(@Nullable CharSequence title) {
        if (title == null || title.toString().isEmpty()) {
            // Return default 'Options' string for category
            return getContext().getString(R.string.accessibility_screen_option);
        }
        return title;
    }

    /**
     * Overrides to determinate if showing additional category description above shortcut
     *
     * @return boolean true to show category, false otherwise.
     */
    protected boolean showGeneralCategory() {
        return false;
    }

    private void setDialogTextAreaClickListener(View dialogView, CheckBox checkBox) {
        final View dialogTextArea = dialogView.findViewById(R.id.container);
        dialogTextArea.setOnClickListener(v -> checkBox.toggle());
    }

    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isSettingsEditable()) {
            return context.getText(R.string.accessibility_shortcut_edit_dialog_title_hardware);
        }

        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.accessibility_shortcut_state_off);
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(context,
                getComponentName().flattenToString());

        final List<CharSequence> list = new ArrayList<>();

        if (hasShortcutType(shortcutTypes, AccessibilityUtil.UserShortcutType.SOFTWARE)) {
            list.add(getSoftwareShortcutTypeSummary(context));
        }
        if (hasShortcutType(shortcutTypes, AccessibilityUtil.UserShortcutType.HARDWARE)) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_hardware_keyword);
            list.add(hardwareTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(getSoftwareShortcutTypeSummary(context));
        }

        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, LocaleUtils.getConcatenatedString(list));
    }

    private void updateEditShortcutDialogCheckBox() {
        // If it is during onConfigChanged process then restore the value, or get the saved value
        // when shortcutPreference is checked.
        int value = restoreOnConfigChangedValue();
        if (value == NOT_SET) {
            final int lastNonEmptyUserShortcutType = PreferredShortcuts.retrieveUserShortcutType(
                    getPrefContext(), getComponentName().flattenToString()
            );
            value = mShortcutPreference.isChecked() ? lastNonEmptyUserShortcutType
                    : AccessibilityUtil.UserShortcutType.EMPTY;
        }

        mSoftwareTypeCheckBox.setChecked(
                hasShortcutType(value, AccessibilityUtil.UserShortcutType.SOFTWARE));
        mHardwareTypeCheckBox.setChecked(
                hasShortcutType(value, AccessibilityUtil.UserShortcutType.HARDWARE));
    }

    private int restoreOnConfigChangedValue() {
        final int savedValue = mSavedCheckBoxValue;
        mSavedCheckBoxValue = NOT_SET;
        return savedValue;
    }

    private boolean hasShortcutType(int value, @AccessibilityUtil.UserShortcutType int type) {
        return (value & type) == type;
    }

    protected void updateShortcutPreferenceData() {
        if (getComponentName() == null) {
            return;
        }

        final int shortcutTypes = AccessibilityUtil.getUserShortcutTypesFromSettings(
                getPrefContext(), getComponentName());
        if (shortcutTypes != AccessibilityUtil.UserShortcutType.EMPTY) {
            final PreferredShortcut shortcut = new PreferredShortcut(
                    getComponentName().flattenToString(), shortcutTypes);
            PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
        }
    }

    protected void updateShortcutPreference() {
        if (getComponentName() == null) {
            return;
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(getPrefContext(),
                getComponentName().flattenToString());
        mShortcutPreference.setChecked(
                AccessibilityUtil.hasValuesInSettings(getPrefContext(), shortcutTypes,
                        getComponentName()));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    /**
     * Shows the quick settings tooltip if the quick settings feature is assigned. The tooltip only
     * shows once.
     *
     * @param type The quick settings tooltip type
     */
    protected void showQuickSettingsTooltipIfNeeded(@QuickSettingsTooltipType int type) {
        mNeedsQSTooltipType = type;
        showQuickSettingsTooltipIfNeeded();
    }

    private void showQuickSettingsTooltipIfNeeded() {
        final ComponentName tileComponentName = getTileComponentName();
        if (tileComponentName == null) {
            // Returns if no tile service assigned.
            return;
        }

        if (!mNeedsQSTooltipReshow && AccessibilityQuickSettingUtils.hasValueInSharedPreferences(
                getContext(), tileComponentName)) {
            // Returns if quick settings tooltip only show once.
            return;
        }

        final CharSequence content = getTileTooltipContent(mNeedsQSTooltipType);
        if (TextUtils.isEmpty(content)) {
            // Returns if no content of tile tooltip assigned.
            return;
        }

        final int imageResId = mNeedsQSTooltipType == QuickSettingsTooltipType.GUIDE_TO_EDIT
                ? R.drawable.accessibility_qs_tooltip_illustration
                : R.drawable.accessibility_auto_added_qs_tooltip_illustration;
        mTooltipWindow = new AccessibilityQuickSettingsTooltipWindow(getContext());
        mTooltipWindow.setup(content, imageResId);
        mTooltipWindow.showAtTopCenter(getView());
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(getContext(),
                tileComponentName);
        mNeedsQSTooltipReshow = false;
    }
}
