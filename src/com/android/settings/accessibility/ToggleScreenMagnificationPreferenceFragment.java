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

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Fragment that shows the actual UI for providing basic magnification accessibility service setup
 * and does not have toggle bar to turn on service to use.
 */
public class ToggleScreenMagnificationPreferenceFragment extends
        ToggleFeaturePreferenceFragment {

    private static final String EXTRA_SHORTCUT_TYPE = "shortcut_type";
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";
    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;
    private int mUserShortcutType = UserShortcutType.EMPTY;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;
    private CheckBox mTripleTapTypeCheckBox;

    // TODO(b/147021230): Will move common functions and variables to
    //  android/internal/accessibility folder. For now, magnification need to be treated
    //  individually.
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

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
                .appendPath(String.valueOf(R.drawable.accessibility_magnification_banner))
                .build();
        mTouchExplorationStateChangeListener = isTouchExplorationEnabled -> {
            removeDialog(DialogEnums.EDIT_SHORTCUT);
            mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        };
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initShortcutPreference();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mUserShortcutTypesCache);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);

        updateShortcutPreferenceData();
        updateShortcutPreference();
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
        final AlertDialog dialog;
        switch (dialogId) {
            case DialogEnums.GESTURE_NAVIGATION_TUTORIAL:
                return AccessibilityGestureNavigationTutorial
                        .showGestureNavigationTutorialDialog(getPrefContext());
            case DialogEnums.MAGNIFICATION_EDIT_SHORTCUT:
                final CharSequence dialogTitle = getPrefContext().getString(
                        R.string.accessibility_shortcut_title, mPackageName);
                dialog = AccessibilityEditDialogUtils.showMagnificationEditShortcutDialog(
                                getPrefContext(), dialogTitle,
                                this::callOnAlertDialogCheckboxClicked);
                initializeDialogCheckBox(dialog);
                return dialog;
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    private void setDialogTextAreaClickListener(View dialogView, CheckBox checkBox) {
        final View dialogTextArea = dialogView.findViewById(R.id.container);
        dialogTextArea.setOnClickListener(v -> {
            checkBox.toggle();
            updateUserShortcutType(/* saveChanges= */ false);
        });
    }

    private void initializeDialogCheckBox(AlertDialog dialog) {
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
        updateAlertDialogCheckState();

        // Window magnification mode doesn't support advancedView.
        if (isWindowMagnification(getPrefContext())) {
            advancedView.setVisibility(View.GONE);
            return;
        }
        // Shows the triple tap checkbox directly if clicked.
        if (mTripleTapTypeCheckBox.isChecked()) {
            advancedView.setVisibility(View.GONE);
            dialogTripleTapView.setVisibility(View.VISIBLE);
        }
    }

    private void updateAlertDialogCheckState() {
        if (mUserShortcutTypesCache != UserShortcutType.EMPTY) {
            updateCheckStatus(mSoftwareTypeCheckBox, UserShortcutType.SOFTWARE);
            updateCheckStatus(mHardwareTypeCheckBox, UserShortcutType.HARDWARE);
            updateCheckStatus(mTripleTapTypeCheckBox, UserShortcutType.TRIPLETAP);
        }
    }

    private void updateCheckStatus(CheckBox checkBox, @UserShortcutType int type) {
        checkBox.setChecked((mUserShortcutTypesCache & type) == type);
    }

    private void updateUserShortcutType(boolean saveChanges) {
        mUserShortcutTypesCache = UserShortcutType.EMPTY;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mUserShortcutTypesCache |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mUserShortcutTypesCache |= UserShortcutType.HARDWARE;
        }
        if (mTripleTapTypeCheckBox.isChecked()) {
            mUserShortcutTypesCache |= UserShortcutType.TRIPLETAP;
        }

        if (saveChanges) {
            final boolean isChanged = (mUserShortcutTypesCache != UserShortcutType.EMPTY);
            if (isChanged) {
                setUserShortcutType(getPrefContext(), mUserShortcutTypesCache);
            }
            mUserShortcutType = mUserShortcutTypesCache;
        }
    }

    private void setUserShortcutType(Context context, int type) {
        Set<String> info = SharedPreferenceUtils.getUserShortcutTypes(context);
        if (info.isEmpty()) {
            info = new HashSet<>();
        } else {
            final Set<String> filtered = info.stream().filter(
                    str -> str.contains(MAGNIFICATION_CONTROLLER_NAME)).collect(
                    Collectors.toSet());
            info.removeAll(filtered);
        }
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                MAGNIFICATION_CONTROLLER_NAME, type);
        info.add(shortcut.flattenToString());
        SharedPreferenceUtils.setUserShortcutType(context, info);
    }

    @Override
    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.switch_off_text);
        }

        final int shortcutType = getUserShortcutTypes(context, UserShortcutType.EMPTY);
        int resId = R.string.accessibility_shortcut_edit_summary_software;
        if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = AccessibilityUtil.isTouchExploreEnabled(context)
                    ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture_talkback
                    : R.string.accessibility_shortcut_edit_dialog_title_software_gesture;
        }
        final CharSequence softwareTitle = context.getText(resId);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_hardware_keyword);
            list.add(hardwareTitle);
        }

        if ((shortcutType & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP) {
            final CharSequence tripleTapTitle = context.getText(
                    R.string.accessibility_shortcut_triple_tap_keyword);
            list.add(tripleTapTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }
        final String joinStrings = TextUtils.join(/* delimiter= */", ", list);

        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, joinStrings);
    }

    @Override
    protected int getUserShortcutTypes(Context context, @UserShortcutType int defaultValue) {
        final Set<String> info = SharedPreferenceUtils.getUserShortcutTypes(context);
        final Set<String> filtered = info.stream().filter(
                str -> str.contains(MAGNIFICATION_CONTROLLER_NAME)).collect(
                Collectors.toSet());
        if (filtered.isEmpty()) {
            return defaultValue;
        }

        final String str = (String) filtered.toArray()[0];
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(str);
        return shortcut.getType();
    }

    @Override
    protected void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateUserShortcutType(/* saveChanges= */ true);
        optInAllMagnificationValuesToSettings(getPrefContext(), mUserShortcutType);
        optOutAllMagnificationValuesFromSettings(getPrefContext(), ~mUserShortcutType);
        mShortcutPreference.setChecked(mUserShortcutType != UserShortcutType.EMPTY);
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public int getMetricsCategory() {
        // TODO: Distinguish between magnification modes
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
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
        super.onInstallSwitchPreferenceToggleSwitch();
        mToggleServiceDividerSwitchPreference.setVisible(false);
    }

    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        final int shortcutTypes = getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE);
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
        // Do not restore shortcut in shortcut chooser dialog when shortcutPreference is turned off.
        mUserShortcutTypesCache = mShortcutPreference.isChecked()
                ? getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE)
                : UserShortcutType.EMPTY;
        showDialog(DialogEnums.MAGNIFICATION_EDIT_SHORTCUT);
    }

    @Override
    protected void updateShortcutPreferenceData() {
        // Get the user shortcut type from settings provider.
        mUserShortcutType = getUserShortcutTypeFromSettings(getPrefContext());
        if (mUserShortcutType != UserShortcutType.EMPTY) {
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        } else {
            //  Get the user shortcut type from shared_prefs if cannot get from settings provider.
            mUserShortcutType = getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE);
        }
    }

    private void initShortcutPreference() {
        mShortcutPreference = new ShortcutPreference(getPrefContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(KEY_SHORTCUT_PREFERENCE);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        mShortcutPreference.setOnClickCallback(this);

        final CharSequence title = getString(R.string.accessibility_shortcut_title, mPackageName);
        mShortcutPreference.setTitle(title);
    }

    @Override
    protected void updateShortcutPreference() {
        final int shortcutTypes = getUserShortcutTypes(getPrefContext(), UserShortcutType.SOFTWARE);
        mShortcutPreference.setChecked(
                hasMagnificationValuesInSettings(getPrefContext(), shortcutTypes));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
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
