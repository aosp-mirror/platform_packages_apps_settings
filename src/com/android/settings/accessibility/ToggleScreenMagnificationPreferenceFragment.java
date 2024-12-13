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

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TWOFINGER_DOUBLETAP;
import static com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.icu.text.CaseMap;
import android.icu.text.MessageFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.accessibility.Flags;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.utils.LocaleUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.IllustrationPreference;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    // TODO(b/147021230): Move duplicated functions with android/internal/accessibility into util.
    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;
    private DialogCreatable mDialogDelegate;

    @Nullable
    MagnificationOneFingerPanningPreferenceController mOneFingerPanningPreferenceController;

    private boolean mInSetupWizard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.accessibility_screen_magnification_title);
        mInSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
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
        final IllustrationPreference illustrationPreference =
                getPreferenceScreen().findPreference(KEY_ANIMATED_IMAGE);
        if (illustrationPreference != null) {
            illustrationPreference.applyDynamicColor();
        }

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
                return AccessibilityShortcutsTutorial
                        .showAccessibilityGestureTutorialDialog(getPrefContext());
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
        addPreferenceController(magnificationModePreferenceController);

        addFollowTypingSetting(generalCategory);
        addOneFingerPanningSetting(generalCategory);
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

        if (!arguments.containsKey(AccessibilitySettings.EXTRA_HTML_DESCRIPTION)
                && !Flags.enableMagnificationOneFingerPanningGesture()) {
            String summary = MessageFormat.format(
                    context.getString(R.string.accessibility_screen_magnification_summary),
                            new Object[]{1, 2, 3, 4, 5});
            arguments.putCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, summary);
        }

        super.onProcessArguments(arguments);
    }

    private void addFollowTypingSetting(PreferenceCategory generalCategory) {
        var followingTypingSwitchPreference = new SwitchPreferenceCompat(getPrefContext());
        followingTypingSwitchPreference.setTitle(
                R.string.accessibility_screen_magnification_follow_typing_title);
        followingTypingSwitchPreference.setSummary(
                R.string.accessibility_screen_magnification_follow_typing_summary);
        followingTypingSwitchPreference.setKey(
                MagnificationFollowTypingPreferenceController.PREF_KEY);
        generalCategory.addPreference(followingTypingSwitchPreference);

        var followTypingPreferenceController = new MagnificationFollowTypingPreferenceController(
                getContext(), MagnificationFollowTypingPreferenceController.PREF_KEY);
        followTypingPreferenceController.setInSetupWizard(mInSetupWizard);
        followTypingPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(followTypingPreferenceController);
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
        alwaysOnPreferenceController.setInSetupWizard(mInSetupWizard);
        getSettingsLifecycle().addObserver(alwaysOnPreferenceController);
        alwaysOnPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(alwaysOnPreferenceController);
    }

    private void addOneFingerPanningSetting(PreferenceCategory generalCategory) {
        if (!Flags.enableMagnificationOneFingerPanningGesture()) {
            return;
        }

        var oneFingerPanningPreference = new SwitchPreferenceCompat(getPrefContext());
        oneFingerPanningPreference.setTitle(
                R.string.accessibility_magnification_one_finger_panning_title);
        oneFingerPanningPreference.setKey(
                MagnificationOneFingerPanningPreferenceController.PREF_KEY);
        generalCategory.addPreference(oneFingerPanningPreference);

        mOneFingerPanningPreferenceController =
                new MagnificationOneFingerPanningPreferenceController(getContext());
        mOneFingerPanningPreferenceController.setInSetupWizard(mInSetupWizard);
        getSettingsLifecycle().addObserver(mOneFingerPanningPreferenceController);
        mOneFingerPanningPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(mOneFingerPanningPreferenceController);
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
        joystickPreferenceController.setInSetupWizard(mInSetupWizard);
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

        if (Flags.enableMagnificationOneFingerPanningGesture()) {
            contentObserver.registerKeysToObserverCallback(
                    List.of(Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED),
                    key -> updateHtmlTextPreference());
        }
    }

    private void updatePreferencesState() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        getPreferenceControllers().forEach(controllers::addAll);
        controllers.forEach(controller -> controller.updateState(
                findPreference(controller.getPreferenceKey())));
    }

    @Override
    CharSequence getCurrentHtmlDescription() {
        CharSequence origin = super.getCurrentHtmlDescription();
        if (!TextUtils.isEmpty(origin)) {
            // If in ToggleFeaturePreferenceFragment we already have a fixed html description, we
            // should use the fixed one, otherwise we'll dynamically decide the description.
            return origin;
        }

        Context context = getContext();
        if (mOneFingerPanningPreferenceController != null && context != null) {
            @StringRes int resId = mOneFingerPanningPreferenceController.isChecked()
                    ? R.string.accessibility_screen_magnification_summary_one_finger_panning_on
                    : R.string.accessibility_screen_magnification_summary_one_finger_panning_off;
            return MessageFormat.format(context.getString(resId), new Object[]{1, 2, 3, 4, 5});
        }
        return "";
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
                MAGNIFICATION_CONTROLLER_NAME);

        // LINT.IfChange(shortcut_type_ui_order)
        final List<CharSequence> list = new ArrayList<>();
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            if (hasShortcutType(shortcutTypes, QUICK_SETTINGS)) {
                final CharSequence qsTitle = context.getText(
                        R.string.accessibility_feature_shortcut_setting_summary_quick_settings);
                list.add(qsTitle);
            }
        }
        if (hasShortcutType(shortcutTypes, SOFTWARE)) {
            list.add(getSoftwareShortcutTypeSummary(context));
        }
        if (hasShortcutType(shortcutTypes, HARDWARE)) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_hardware_keyword);
            list.add(hardwareTitle);
        }
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (hasShortcutType(shortcutTypes, TWOFINGER_DOUBLETAP)) {
                final CharSequence twoFingerDoubleTapTitle = context.getString(
                        R.string.accessibility_shortcut_two_finger_double_tap_keyword, 2);
                list.add(twoFingerDoubleTapTitle);
            }
        }
        if (hasShortcutType(shortcutTypes, TRIPLETAP)) {
            final CharSequence tripleTapTitle = context.getText(
                    R.string.accessibility_shortcut_triple_tap_keyword);
            list.add(tripleTapTitle);
        }
        // LINT.ThenChange(/res/xml/accessibility_edit_shortcuts.xml:shortcut_type_ui_order)

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(getSoftwareShortcutTypeSummary(context));
        }

        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, LocaleUtils.getConcatenatedString(list));
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
        final int shortcutTypes = getUserPreferredShortcutTypes();
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
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
                requireContext(),
                getMetricsCategory(),
                getShortcutTitle(),
                MAGNIFICATION_COMPONENT_NAME,
                getIntent());
    }

    @Override
    protected void updateShortcutPreferenceData() {
        final int shortcutTypes = getUserShortcutTypeFromSettings(getPrefContext());
        if (shortcutTypes != DEFAULT) {
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
        final int shortcutTypes = getUserPreferredShortcutTypes();
        mShortcutPreference.setChecked(
                hasMagnificationValuesInSettings(getPrefContext(), shortcutTypes));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    @VisibleForTesting
    static void optInAllMagnificationValuesToSettings(Context context, int shortcutTypes) {
        if ((shortcutTypes & SOFTWARE) == SOFTWARE) {
            optInMagnificationValueToSettings(context, SOFTWARE);
        }
        if (((shortcutTypes & HARDWARE) == HARDWARE)) {
            optInMagnificationValueToSettings(context, HARDWARE);
        }
        if (((shortcutTypes & TRIPLETAP) == TRIPLETAP)) {
            optInMagnificationValueToSettings(context, TRIPLETAP);
        }
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (((shortcutTypes & TWOFINGER_DOUBLETAP)
                    == TWOFINGER_DOUBLETAP)) {
                optInMagnificationValueToSettings(context, TWOFINGER_DOUBLETAP);
            }
        }
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            if (((shortcutTypes & QUICK_SETTINGS)
                    == QUICK_SETTINGS)) {
                optInMagnificationValueToSettings(context, QUICK_SETTINGS);
            }
        }
    }

    private static void optInMagnificationValueToSettings(
            Context context, @UserShortcutType int shortcutType) {
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            AccessibilityManager a11yManager = context.getSystemService(AccessibilityManager.class);
            if (a11yManager != null) {
                a11yManager.enableShortcutsForTargets(
                        /* enable= */ true,
                        shortcutType,
                        Set.of(MAGNIFICATION_CONTROLLER_NAME),
                        UserHandle.myUserId()
                );
            }
            return;
        }

        if (shortcutType == TRIPLETAP) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, ON);
            return;
        }

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (shortcutType == TWOFINGER_DOUBLETAP) {
                Settings.Secure.putInt(
                        context.getContentResolver(),
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
        if (shortcutType == SOFTWARE
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
        if ((shortcutTypes & SOFTWARE) == SOFTWARE) {
            optOutMagnificationValueFromSettings(context, SOFTWARE);
        }
        if (((shortcutTypes & HARDWARE) == HARDWARE)) {
            optOutMagnificationValueFromSettings(context, HARDWARE);
        }
        if (((shortcutTypes & TRIPLETAP) == TRIPLETAP)) {
            optOutMagnificationValueFromSettings(context, TRIPLETAP);
        }
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (((shortcutTypes & TWOFINGER_DOUBLETAP)
                    == TWOFINGER_DOUBLETAP)) {
                optOutMagnificationValueFromSettings(context, TWOFINGER_DOUBLETAP);
            }
        }
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            if (((shortcutTypes & QUICK_SETTINGS)
                    == QUICK_SETTINGS)) {
                optOutMagnificationValueFromSettings(context, QUICK_SETTINGS);
            }
        }
    }

    private static void optOutMagnificationValueFromSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (android.view.accessibility.Flags.a11yQsShortcut()) {
            AccessibilityManager a11yManager = context.getSystemService(AccessibilityManager.class);
            if (a11yManager != null) {
                a11yManager.enableShortcutsForTargets(
                        /* enable= */ false,
                        shortcutType,
                        Set.of(MAGNIFICATION_CONTROLLER_NAME),
                        UserHandle.myUserId()
                );
            }
            return;
        }

        if (shortcutType == TRIPLETAP) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF);
            return;
        }

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (shortcutType == TWOFINGER_DOUBLETAP) {
                Settings.Secure.putInt(
                        context.getContentResolver(),
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

        if ((shortcutTypes & SOFTWARE) == SOFTWARE) {
            exist = hasMagnificationValueInSettings(context, SOFTWARE);
        }
        if (((shortcutTypes & HARDWARE) == HARDWARE)) {
            exist |= hasMagnificationValueInSettings(context, HARDWARE);
        }
        if (((shortcutTypes & TRIPLETAP) == TRIPLETAP)) {
            exist |= hasMagnificationValueInSettings(context, TRIPLETAP);
        }
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (((shortcutTypes & TWOFINGER_DOUBLETAP)
                    == TWOFINGER_DOUBLETAP)) {
                exist |= hasMagnificationValueInSettings(context,
                        TWOFINGER_DOUBLETAP);
            }
        }
        return exist;
    }

    private static boolean hasMagnificationValueInSettings(Context context,
            @UserShortcutType int shortcutType) {
        if (shortcutType == TRIPLETAP) {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
        }

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (shortcutType == TWOFINGER_DOUBLETAP) {
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
        int shortcutTypes = DEFAULT;
        if (hasMagnificationValuesInSettings(context, SOFTWARE)) {
            shortcutTypes |= SOFTWARE;
        }
        if (hasMagnificationValuesInSettings(context, HARDWARE)) {
            shortcutTypes |= HARDWARE;
        }
        if (hasMagnificationValuesInSettings(context, TRIPLETAP)) {
            shortcutTypes |= TRIPLETAP;
        }
        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if (hasMagnificationValuesInSettings(context, TWOFINGER_DOUBLETAP)) {
                shortcutTypes |= TWOFINGER_DOUBLETAP;
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
                (userShortcutType != DEFAULT)
                ? context.getText(R.string.accessibility_summary_shortcut_enabled)
                : context.getText(R.string.generic_accessibility_feature_shortcut_off);
        final CharSequence featureSummary = context.getText(R.string.magnification_feature_summary);
        return context.getString(
                com.android.settingslib.R.string.preference_summary_default_combination,
                featureState, featureSummary);
    }

    @Override
    protected int getUserPreferredShortcutTypes() {
        return PreferredShortcuts.retrieveUserShortcutType(
                getPrefContext(), MAGNIFICATION_CONTROLLER_NAME);
    }
}
