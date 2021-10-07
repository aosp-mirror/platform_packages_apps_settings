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

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.icu.text.CaseMap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.widget.CheckBox;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogType;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.utils.LocaleUtils;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Fragment that shows the actual UI for providing basic magnification accessibility service setup
 * and does not have toggle bar to turn on service to use.
 */
public class ToggleScreenMagnificationPreferenceFragment extends
        ToggleFeaturePreferenceFragment implements
        MagnificationModePreferenceController.DialogHelper {
    // TODO(b/147021230): Move duplicated functions with android/internal/accessibility into util.
    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;

    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;
    private CheckBox mTripleTapTypeCheckBox;

    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    private MagnificationModePreferenceController mModePreferenceController;
    private DialogCreatable mDialogDelegate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.accessibility_screen_magnification_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mPackageName = getString(R.string.accessibility_screen_magnification_title);
        mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(getPrefContext().getPackageName())
                .appendPath(String.valueOf(R.raw.accessibility_magnification_banner))
                .build();
        mTouchExplorationStateChangeListener = isTouchExplorationEnabled -> {
            removeDialog(DialogEnums.EDIT_SHORTCUT);
            mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        };
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
    }

    @Override
    public void onPause() {
        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);

        super.onPause();
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (mDialogDelegate != null) {
            final Dialog dialog = mDialogDelegate.onCreateDialog(dialogId);
            if (dialog != null) {
                return dialog;
            }
        }
        final AlertDialog dialog;
        switch (dialogId) {
            case DialogEnums.GESTURE_NAVIGATION_TUTORIAL:
                return AccessibilityGestureNavigationTutorial
                        .showGestureNavigationTutorialDialog(getPrefContext());
            case DialogEnums.MAGNIFICATION_EDIT_SHORTCUT:
                final CharSequence dialogTitle = getPrefContext().getString(
                        R.string.accessibility_shortcut_title, mPackageName);
                final int dialogType = WizardManagerHelper.isAnySetupWizard(getIntent())
                        ? DialogType.EDIT_SHORTCUT_MAGNIFICATION_SUW
                        : DialogType.EDIT_SHORTCUT_MAGNIFICATION;
                dialog = AccessibilityDialogUtils.showEditShortcutDialog(getPrefContext(),
                        dialogType, dialogTitle, this::callOnAlertDialogCheckboxClicked);
                setupMagnificationEditShortcutDialog(dialog);
                return dialog;
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    @Override
    protected void initSettingsPreference() {
        // If the device doesn't support magnification area, it should hide the settings preference.
        if (!getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_magnification_area)) {
            return;
        }
        mSettingsPreference = new Preference(getPrefContext());
        mSettingsPreference.setTitle(R.string.accessibility_magnification_mode_title);
        mSettingsPreference.setKey(MagnificationModePreferenceController.PREF_KEY);
        mSettingsPreference.setPersistent(false);

        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        generalCategory.addPreference(mSettingsPreference);

        mModePreferenceController = new MagnificationModePreferenceController(getContext(),
                MagnificationModePreferenceController.PREF_KEY);
        mModePreferenceController.setDialogHelper(this);
        getSettingsLifecycle().addObserver(mModePreferenceController);
        mModePreferenceController.displayPreference(getPreferenceScreen());
    }

    @Override
    public void showDialog(int dialogId) {
        super.showDialog(dialogId);
    }

    @Override
    public void setDialogDelegate(DialogCreatable delegate) {
        mDialogDelegate = delegate;
    }

    @Override
    protected int getShortcutTypeCheckBoxValue() {
        if (mSoftwareTypeCheckBox == null || mHardwareTypeCheckBox == null) {
            return NOT_SET;
        }

        int value = UserShortcutType.EMPTY;
        if (mSoftwareTypeCheckBox.isChecked()) {
            value |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            value |= UserShortcutType.HARDWARE;
        }
        if (mTripleTapTypeCheckBox.isChecked()) {
            value |= UserShortcutType.TRIPLETAP;
        }
        return value;
    }

    @VisibleForTesting
    void setupMagnificationEditShortcutDialog(AlertDialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogSoftwareView, mSoftwareTypeCheckBox);

        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogHardwareView, mHardwareTypeCheckBox);

        final View dialogTripleTapView = dialog.findViewById(R.id.triple_tap_shortcut);
        mTripleTapTypeCheckBox = dialogTripleTapView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogTripleTapView, mTripleTapTypeCheckBox);

        final View advancedView = dialog.findViewById(R.id.advanced_shortcut);
        if (mTripleTapTypeCheckBox.isChecked()) {
            advancedView.setVisibility(View.GONE);
            dialogTripleTapView.setVisibility(View.VISIBLE);
        }

        updateMagnificationEditShortcutDialogCheckBox();
    }

    private void setDialogTextAreaClickListener(View dialogView, CheckBox checkBox) {
        final View dialogTextArea = dialogView.findViewById(R.id.container);
        dialogTextArea.setOnClickListener(v -> checkBox.toggle());
    }

    private void updateMagnificationEditShortcutDialogCheckBox() {
        // If it is during onConfigChanged process then restore the value, or get the saved value
        // when shortcutPreference is checked.
        int value = restoreOnConfigChangedValue();
        if (value == NOT_SET) {
            final int lastNonEmptyUserShortcutType = PreferredShortcuts.retrieveUserShortcutType(
                    getPrefContext(), MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.SOFTWARE);
            value = mShortcutPreference.isChecked() ? lastNonEmptyUserShortcutType
                    : UserShortcutType.EMPTY;
        }

        mSoftwareTypeCheckBox.setChecked(
                hasShortcutType(value, UserShortcutType.SOFTWARE));
        mHardwareTypeCheckBox.setChecked(
                hasShortcutType(value, UserShortcutType.HARDWARE));
        mTripleTapTypeCheckBox.setChecked(
                hasShortcutType(value, UserShortcutType.TRIPLETAP));
    }

    private int restoreOnConfigChangedValue() {
        final int savedValue = mSavedCheckBoxValue;
        mSavedCheckBoxValue = NOT_SET;
        return savedValue;
    }

    private boolean hasShortcutType(int value, @UserShortcutType int type) {
        return (value & type) == type;
    }

    @Override
    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.switch_off_text);
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(context,
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.SOFTWARE);

        final List<CharSequence> list = new ArrayList<>();
        final CharSequence softwareTitle = context.getText(
                R.string.accessibility_shortcut_edit_summary_software);

        if (hasShortcutType(shortcutTypes, UserShortcutType.SOFTWARE)) {
            list.add(softwareTitle);
        }
        if (hasShortcutType(shortcutTypes, UserShortcutType.HARDWARE)) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_hardware_keyword);
            list.add(hardwareTitle);
        }

        if (hasShortcutType(shortcutTypes, UserShortcutType.TRIPLETAP)) {
            final CharSequence tripleTapTitle = context.getText(
                    R.string.accessibility_shortcut_triple_tap_keyword);
            list.add(tripleTapTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }

        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, LocaleUtils.getConcatenatedString(list));
    }

    @Override
    protected void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        final int value = getShortcutTypeCheckBoxValue();

        saveNonEmptyUserShortcutType(value);
        optInAllMagnificationValuesToSettings(getPrefContext(), value);
        optOutAllMagnificationValuesFromSettings(getPrefContext(), ~value);
        mShortcutPreference.setChecked(value != UserShortcutType.EMPTY);
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_magnification;
    }

    @Override
    public int getMetricsCategory() {
        // TODO: Distinguish between magnification modes
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (mDialogDelegate != null) {
            final int category = mDialogDelegate.getDialogMetricsCategory(dialogId);
            if (category != 0) {
                return category;
            }
        }

        switch (dialogId) {
            case DialogEnums.GESTURE_NAVIGATION_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_GESTURE_NAVIGATION;
            case DialogEnums.ACCESSIBILITY_BUTTON_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_ACCESSIBILITY_BUTTON;
            case DialogEnums.MAGNIFICATION_EDIT_SHORTCUT:
                return SettingsEnums.DIALOG_MAGNIFICATION_EDIT_SHORTCUT;
            default:
                return super.getDialogMetricsCategory(dialogId);
        }
    }

    @Override
    int getUserShortcutTypes() {
        return getUserShortcutTypeFromSettings(getPrefContext());
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        if (enabled && TextUtils.equals(
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED,
                preferenceKey)) {
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        }
        MagnificationPreferenceFragment.setChecked(getContentResolver(), preferenceKey, enabled);
    }

    @Override
    protected void onInstallSwitchPreferenceToggleSwitch() {
        mToggleServiceSwitchPreference.setVisible(false);
    }

    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(getPrefContext(),
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.SOFTWARE);
        if (preference.isChecked()) {
            optInAllMagnificationValuesToSettings(getPrefContext(), shortcutTypes);
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        } else {
            optOutAllMagnificationValuesFromSettings(getPrefContext(), shortcutTypes);
        }
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        showDialog(DialogEnums.MAGNIFICATION_EDIT_SHORTCUT);
    }

    @Override
    protected void updateShortcutPreferenceData() {
        final int shortcutTypes = getUserShortcutTypeFromSettings(getPrefContext());
        if (shortcutTypes != UserShortcutType.EMPTY) {
            final PreferredShortcut shortcut = new PreferredShortcut(
                    MAGNIFICATION_CONTROLLER_NAME, shortcutTypes);
            PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
        }
    }

    @Override
    protected void initShortcutPreference() {
        mShortcutPreference = new ShortcutPreference(getPrefContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        mShortcutPreference.setOnClickCallback(this);

        final CharSequence title = getString(R.string.accessibility_shortcut_title, mPackageName);
        mShortcutPreference.setTitle(title);

        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        generalCategory.addPreference(mShortcutPreference);
    }

    @Override
    protected void updateShortcutPreference() {
        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(getPrefContext(),
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.SOFTWARE);
        mShortcutPreference.setChecked(
                hasMagnificationValuesInSettings(getPrefContext(), shortcutTypes));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    @VisibleForTesting
    void saveNonEmptyUserShortcutType(int type) {
        if (type == UserShortcutType.EMPTY) {
            return;
        }

        final PreferredShortcut shortcut = new PreferredShortcut(
                MAGNIFICATION_CONTROLLER_NAME, type);
        PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
    }

    @VisibleForTesting
    static void optInAllMagnificationValuesToSettings(Context context, int shortcutTypes) {
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            optInMagnificationValueToSettings(context, UserShortcutType.SOFTWARE);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            optInMagnificationValueToSettings(context, UserShortcutType.HARDWARE);
        }
        if (((shortcutTypes & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP)) {
            optInMagnificationValueToSettings(context, UserShortcutType.TRIPLETAP);
        }
    }

    private static void optInMagnificationValueToSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (shortcutType == UserShortcutType.TRIPLETAP) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, ON);
            return;
        }

        if (hasMagnificationValueInSettings(context, shortcutType)) {
            return;
        }

        final String targetKey = AccessibilityUtil.convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);
        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));

        if (!TextUtils.isEmpty(targetString)) {
            joiner.add(targetString);
        }
        joiner.add(MAGNIFICATION_CONTROLLER_NAME);

        Settings.Secure.putString(context.getContentResolver(), targetKey, joiner.toString());
    }

    @VisibleForTesting
    static void optOutAllMagnificationValuesFromSettings(Context context,
            int shortcutTypes) {
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            optOutMagnificationValueFromSettings(context, UserShortcutType.SOFTWARE);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            optOutMagnificationValueFromSettings(context, UserShortcutType.HARDWARE);
        }
        if (((shortcutTypes & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP)) {
            optOutMagnificationValueFromSettings(context, UserShortcutType.TRIPLETAP);
        }
    }

    private static void optOutMagnificationValueFromSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (shortcutType == UserShortcutType.TRIPLETAP) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF);
            return;
        }

        final String targetKey = AccessibilityUtil.convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (TextUtils.isEmpty(targetString)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));

        sStringColonSplitter.setString(targetString);
        while (sStringColonSplitter.hasNext()) {
            final String name = sStringColonSplitter.next();
            if (TextUtils.isEmpty(name) || MAGNIFICATION_CONTROLLER_NAME.equals(name)) {
                continue;
            }
            joiner.add(name);
        }

        Settings.Secure.putString(context.getContentResolver(), targetKey, joiner.toString());
    }

    @VisibleForTesting
    static boolean hasMagnificationValuesInSettings(Context context, int shortcutTypes) {
        boolean exist = false;

        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            exist = hasMagnificationValueInSettings(context, UserShortcutType.SOFTWARE);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            exist |= hasMagnificationValueInSettings(context, UserShortcutType.HARDWARE);
        }
        if (((shortcutTypes & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP)) {
            exist |= hasMagnificationValueInSettings(context, UserShortcutType.TRIPLETAP);
        }
        return exist;
    }

    private static boolean hasMagnificationValueInSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (shortcutType == UserShortcutType.TRIPLETAP) {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
        }

        final String targetKey = AccessibilityUtil.convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (TextUtils.isEmpty(targetString)) {
            return false;
        }

        sStringColonSplitter.setString(targetString);
        while (sStringColonSplitter.hasNext()) {
            final String name = sStringColonSplitter.next();
            if (MAGNIFICATION_CONTROLLER_NAME.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWindowMagnification(Context context) {
        final int mode = Settings.Secure.getIntForUser(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN,
                context.getContentResolver().getUserId());
        return mode == Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW;
    }

    private static int getUserShortcutTypeFromSettings(Context context) {
        int shortcutTypes = UserShortcutType.EMPTY;
        if (hasMagnificationValuesInSettings(context, UserShortcutType.SOFTWARE)) {
            shortcutTypes |= UserShortcutType.SOFTWARE;
        }
        if (hasMagnificationValuesInSettings(context, UserShortcutType.HARDWARE)) {
            shortcutTypes |= UserShortcutType.HARDWARE;
        }
        if (hasMagnificationValuesInSettings(context, UserShortcutType.TRIPLETAP)) {
            shortcutTypes |= UserShortcutType.TRIPLETAP;
        }
        return shortcutTypes;
    }

    /**
     * Gets the service summary of magnification.
     *
     * @param context The current context.
     */
    public static CharSequence getServiceSummary(Context context) {
        // Get the user shortcut type from settings provider.
        final int uerShortcutType = getUserShortcutTypeFromSettings(context);
        return (uerShortcutType != AccessibilityUtil.UserShortcutType.EMPTY)
                ? context.getText(R.string.accessibility_summary_shortcut_enabled)
                : context.getText(R.string.accessibility_summary_shortcut_disabled);
    }
}
