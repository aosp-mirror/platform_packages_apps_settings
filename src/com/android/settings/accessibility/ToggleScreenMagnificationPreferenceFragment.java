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
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.icu.text.CaseMap;
import android.icu.text.MessageFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.widget.CheckBox;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.accessibility.Flags;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogType;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import com.android.settings.utils.LocaleUtils;
import com.android.settingslib.core.AbstractPreferenceController;

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

    private static final String TAG = "ToggleScreenMagnificationPreferenceFragment";
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    protected TwoStatePreference mFollowingTypingSwitchPreference;

    // TODO(b/147021230): Move duplicated functions with android/internal/accessibility into util.
    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;
    private CheckBox mTripleTapTypeCheckBox;
    @Nullable private CheckBox mTwoFingerTripleTapTypeCheckBox;
    private DialogCreatable mDialogDelegate;
    private MagnificationFollowTypingPreferenceController mFollowTypingPreferenceController;

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
                .appendPath(String.valueOf(R.raw.a11y_magnification_banner))
                .build();
        mTouchExplorationStateChangeListener = isTouchExplorationEnabled -> {
            removeDialog(DialogEnums.EDIT_SHORTCUT);
            mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        };

        final View view = super.onCreateView(inflater, container, savedInstanceState);
        updateFooterPreference();
        return view;
    }

    private void updateFooterPreference() {
        final String title = getPrefContext().getString(
                R.string.accessibility_screen_magnification_about_title);
        final String learnMoreText = getPrefContext().getString(
                R.string.accessibility_screen_magnification_footer_learn_more_content_description);
        mFooterPreferenceController.setIntroductionTitle(title);
        mFooterPreferenceController.setupHelpLink(getHelpResource(), learnMoreText);
        mFooterPreferenceController.displayPreference(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();

        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
    }

    @Override
    protected int getPreferenceScreenResId() {
        // TODO(b/171272809): Add back when controllers move to static type
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
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
            mDialog = mDialogDelegate.onCreateDialog(dialogId);
            if (mDialog != null) {
                return mDialog;
            }
        }
        switch (dialogId) {
            case DialogEnums.GESTURE_NAVIGATION_TUTORIAL:
                return AccessibilityGestureNavigationTutorial
                        .showAccessibilityGestureTutorialDialog(getPrefContext());
            case DialogEnums.MAGNIFICATION_EDIT_SHORTCUT:
                final CharSequence dialogTitle = getShortcutTitle();
                final int dialogType = WizardManagerHelper.isAnySetupWizard(getIntent())
                        ? DialogType.EDIT_SHORTCUT_MAGNIFICATION_SUW
                        : DialogType.EDIT_SHORTCUT_MAGNIFICATION;
                mDialog = AccessibilityDialogUtils.showEditShortcutDialog(getPrefContext(),
                        dialogType, dialogTitle, this::callOnAlertDialogCheckboxClicked);
                setupMagnificationEditShortcutDialog(mDialog);
                return mDialog;
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    @Override
    protected void initSettingsPreference() {
        // If the device doesn't support window magnification feature, it should hide the
        // settings preference.
        final boolean supportWindowMagnification =
                getContext().getResources().getBoolean(
                        com.android.internal.R.bool.config_magnification_area)
                        && getContext().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_WINDOW_MAGNIFICATION);
        if (!supportWindowMagnification) {
            return;
        }
        mSettingsPreference = new Preference(getPrefContext());
        mSettingsPreference.setTitle(R.string.accessibility_magnification_mode_title);
        mSettingsPreference.setKey(MagnificationModePreferenceController.PREF_KEY);
        mSettingsPreference.setPersistent(false);

        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        generalCategory.addPreference(mSettingsPreference);

        final MagnificationModePreferenceController magnificationModePreferenceController =
                new MagnificationModePreferenceController(getContext(),
                        MagnificationModePreferenceController.PREF_KEY);
        magnificationModePreferenceController.setDialogHelper(this);
        getSettingsLifecycle().addObserver(magnificationModePreferenceController);
        magnificationModePreferenceController.displayPreference(getPreferenceScreen());

        mFollowingTypingSwitchPreference = new SwitchPreferenceCompat(getPrefContext());
        mFollowingTypingSwitchPreference.setTitle(
                R.string.accessibility_screen_magnification_follow_typing_title);
        mFollowingTypingSwitchPreference.setSummary(
                R.string.accessibility_screen_magnification_follow_typing_summary);
        mFollowingTypingSwitchPreference.setKey(
                MagnificationFollowTypingPreferenceController.PREF_KEY);
        generalCategory.addPreference(mFollowingTypingSwitchPreference);

        mFollowTypingPreferenceController = new MagnificationFollowTypingPreferenceController(
                getContext(), MagnificationFollowTypingPreferenceController.PREF_KEY);
        getSettingsLifecycle().addObserver(mFollowTypingPreferenceController);
        mFollowTypingPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(mFollowTypingPreferenceController);

        addAlwaysOnSetting(generalCategory);
        addJoystickSetting(generalCategory);
    }

    @Override
    protected void onProcessArguments(Bundle arguments) {
        Context context = getContext();

        if (!arguments.containsKey(AccessibilitySettings.EXTRA_PREFERENCE_KEY)) {
            arguments.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
        }

        if (!arguments.containsKey(AccessibilitySettings.EXTRA_INTRO)) {
            arguments.putCharSequence(AccessibilitySettings.EXTRA_INTRO,
                    context.getString(R.string.accessibility_screen_magnification_intro_text));
        }

        if (!arguments.containsKey(AccessibilitySettings.EXTRA_HTML_DESCRIPTION)) {
            String summary = MessageFormat.format(
                    context.getString(R.string.accessibility_screen_magnification_summary),
                            new Object[]{1, 2, 3, 4, 5});
            arguments.putCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, summary);
        }

        super.onProcessArguments(arguments);
    }

    private boolean isAlwaysOnSettingEnabled() {
        final boolean defaultValue = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_magnification_always_on_enabled);

        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                "AlwaysOnMagnifier__enable_always_on_magnifier",
                defaultValue
        );
    }
    private void addAlwaysOnSetting(PreferenceCategory generalCategory) {
        if (!isAlwaysOnSettingEnabled()) {
            return;
        }

        var alwaysOnPreference = new SwitchPreferenceCompat(getPrefContext());
        alwaysOnPreference.setTitle(
                R.string.accessibility_screen_magnification_always_on_title);
        alwaysOnPreference.setSummary(
                R.string.accessibility_screen_magnification_always_on_summary);
        alwaysOnPreference.setKey(
                MagnificationAlwaysOnPreferenceController.PREF_KEY);
        generalCategory.addPreference(alwaysOnPreference);

        var alwaysOnPreferenceController = new MagnificationAlwaysOnPreferenceController(
                getContext(), MagnificationAlwaysOnPreferenceController.PREF_KEY);
        getSettingsLifecycle().addObserver(alwaysOnPreferenceController);
        alwaysOnPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(alwaysOnPreferenceController);
    }

    private void addJoystickSetting(PreferenceCategory generalCategory) {
        if (!DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                "MagnificationJoystick__enable_magnification_joystick",
                false
        )) {
            return;
        }

        TwoStatePreference joystickPreference = new SwitchPreferenceCompat(getPrefContext());
        joystickPreference.setTitle(
                R.string.accessibility_screen_magnification_joystick_title);
        joystickPreference.setSummary(
                R.string.accessibility_screen_magnification_joystick_summary);
        joystickPreference.setKey(
                MagnificationJoystickPreferenceController.PREF_KEY);
        generalCategory.addPreference(joystickPreference);

        MagnificationJoystickPreferenceController joystickPreferenceController =
                new MagnificationJoystickPreferenceController(
                        getContext(),
                        MagnificationJoystickPreferenceController.PREF_KEY
                );
        getSettingsLifecycle().addObserver(joystickPreferenceController);
        joystickPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(joystickPreferenceController);
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
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (mTwoFingerTripleTapTypeCheckBox.isChecked()) {
                value |= UserShortcutType.TWOFINGERTRIPLETAP;
            }
        }
        return value;
    }

    @VisibleForTesting
    void setupMagnificationEditShortcutDialog(Dialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogSoftwareView, mSoftwareTypeCheckBox);

        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        setDialogTextAreaClickListener(dialogHardwareView, mHardwareTypeCheckBox);

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            final View dialogTwoFignerTripleTapView =
                    dialog.findViewById(R.id.two_finger_triple_tap_shortcut);
            mTwoFingerTripleTapTypeCheckBox = dialogTwoFignerTripleTapView.findViewById(
                    R.id.checkbox);
            setDialogTextAreaClickListener(
                    dialogTwoFignerTripleTapView, mTwoFingerTripleTapTypeCheckBox);
        }

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
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            mTwoFingerTripleTapTypeCheckBox.setChecked(
                    hasShortcutType(value, UserShortcutType.TWOFINGERTRIPLETAP));
        }
    }

    private int restoreOnConfigChangedValue() {
        final int savedValue = mSavedCheckBoxValue;
        mSavedCheckBoxValue = NOT_SET;
        return savedValue;
    }

    private boolean hasShortcutType(int value, @UserShortcutType int type) {
        return (value & type) == type;
    }

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

    @Override
    protected void registerKeysToObserverCallback(
            AccessibilitySettingsContentObserver contentObserver) {
        super.registerKeysToObserverCallback(contentObserver);

        var keysToObserve = List.of(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED,
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED,
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_JOYSTICK_ENABLED
        );
        contentObserver.registerKeysToObserverCallback(keysToObserve,
                key -> updatePreferencesState());
    }

    private void updatePreferencesState() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        getPreferenceControllers().forEach(controllers::addAll);
        controllers.forEach(controller -> controller.updateState(
                findPreference(controller.getPreferenceKey())));
    }

    @Override
    protected List<String> getShortcutFeatureSettingsKeys() {
        final List<String> shortcutKeys = super.getShortcutFeatureSettingsKeys();
        shortcutKeys.add(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
        return shortcutKeys;
    }

    @Override
    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.switch_off_text);
        }

        final int shortcutTypes = PreferredShortcuts.retrieveUserShortcutType(context,
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.SOFTWARE);

        final List<CharSequence> list = new ArrayList<>();
        if (hasShortcutType(shortcutTypes, UserShortcutType.SOFTWARE)) {
            list.add(getSoftwareShortcutTypeSummary(context));
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
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (hasShortcutType(shortcutTypes, UserShortcutType.TWOFINGERTRIPLETAP)) {
                final CharSequence twoFingerTripleTapTitle = context.getText(
                        R.string.accessibility_shortcut_two_finger_double_tap_keyword);
                list.add(twoFingerTripleTapTitle);
            }
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(getSoftwareShortcutTypeSummary(context));
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

        if (mHardwareTypeCheckBox.isChecked()) {
            AccessibilityUtil.skipVolumeShortcutDialogTimeoutRestriction(getPrefContext());
        }
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
    ComponentName getTileComponentName() {
        return null;
    }

    @Override
    CharSequence getTileTooltipContent(@QuickSettingsTooltipType int type) {
        return null;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        if (enabled && TextUtils.equals(
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED,
                preferenceKey)) {
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        }
        Settings.Secure.putInt(getContentResolver(), preferenceKey, enabled ? ON : OFF);
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
        mShortcutPreference.setTitle(getShortcutTitle());

        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        generalCategory.addPreference(mShortcutPreference);
    }

    @Override
    protected CharSequence getShortcutTitle() {
        return getText(R.string.accessibility_screen_magnification_shortcut_title);
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
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (((shortcutTypes & UserShortcutType.TWOFINGERTRIPLETAP)
                    == UserShortcutType.TWOFINGERTRIPLETAP)) {
                optInMagnificationValueToSettings(context, UserShortcutType.TWOFINGERTRIPLETAP);
            }
        }
    }

    private static void optInMagnificationValueToSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (shortcutType == UserShortcutType.TRIPLETAP) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, ON);
            return;
        }

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (shortcutType == UserShortcutType.TWOFINGERTRIPLETAP) {
                Settings.Secure.putInt(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                        ON);
                return;
            }
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
        // The size setting defaults to unknown. If the user has ever manually changed the size
        // before, we do not automatically change it.
        if (shortcutType == UserShortcutType.SOFTWARE
                && Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                FloatingMenuSizePreferenceController.Size.UNKNOWN)
                == FloatingMenuSizePreferenceController.Size.UNKNOWN) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                    FloatingMenuSizePreferenceController.Size.LARGE);
        }
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
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (((shortcutTypes & UserShortcutType.TWOFINGERTRIPLETAP)
                    == UserShortcutType.TWOFINGERTRIPLETAP)) {
                optOutMagnificationValueFromSettings(context, UserShortcutType.TWOFINGERTRIPLETAP);
            }
        }
    }

    private static void optOutMagnificationValueFromSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (shortcutType == UserShortcutType.TRIPLETAP) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF);
            return;
        }

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (shortcutType == UserShortcutType.TWOFINGERTRIPLETAP) {
                Settings.Secure.putInt(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                        OFF);
                return;
            }
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
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (((shortcutTypes & UserShortcutType.TWOFINGERTRIPLETAP)
                    == UserShortcutType.TWOFINGERTRIPLETAP)) {
                exist |= hasMagnificationValueInSettings(context,
                        UserShortcutType.TWOFINGERTRIPLETAP);
            }
        }
        return exist;
    }

    private static boolean hasMagnificationValueInSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (shortcutType == UserShortcutType.TRIPLETAP) {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
        }

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (shortcutType == UserShortcutType.TWOFINGERTRIPLETAP) {
                return Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                        OFF) == ON;
            }
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
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (hasMagnificationValuesInSettings(context, UserShortcutType.TWOFINGERTRIPLETAP)) {
                shortcutTypes |= UserShortcutType.TWOFINGERTRIPLETAP;
            }
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
        final int userShortcutType = getUserShortcutTypeFromSettings(context);
        final CharSequence featureState =
                (userShortcutType != AccessibilityUtil.UserShortcutType.EMPTY)
                ? context.getText(R.string.accessibility_summary_shortcut_enabled)
                : context.getText(R.string.generic_accessibility_feature_shortcut_off);
        final CharSequence featureSummary = context.getText(R.string.magnification_feature_summary);
        return context.getString(R.string.preference_summary_default_combination,
                featureState, featureSummary);
    }
}
