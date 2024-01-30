/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;
import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_SAVED_USER_SHORTCUT_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogType;
import com.android.settings.testutils.shadow.ShadowStorageManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import com.google.common.truth.Correspondence;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowSettings;
import org.robolectric.shadows.androidx.fragment.FragmentController;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collection;
import java.util.List;

/** Tests for {@link ToggleScreenMagnificationPreferenceFragment}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowStorageManager.class,
        ShadowSettings.ShadowSecure.class,
})
public class ToggleScreenMagnificationPreferenceFragmentTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.mock.example";
    private static final String PLACEHOLDER_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + ".mock_a11y_service";
    private static final ComponentName PLACEHOLDER_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_CLASS_NAME);
    private static final String PLACEHOLDER_DIALOG_TITLE = "title";

    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
    private static final String TRIPLETAP_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED;
    private static final String TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED;

    private static final String MAGNIFICATION_CONTROLLER_NAME =
            "com.android.server.accessibility.MagnificationController";

    private static final String KEY_FOLLOW_TYPING =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED;
    private FragmentController<ToggleScreenMagnificationPreferenceFragment> mFragController;
    private Context mContext;
    private Resources mSpyResources;
    private ShadowPackageManager mShadowPackageManager;

    @Before
    public void setUpTestFragment() {

        mContext = ApplicationProvider.getApplicationContext();

        // Set up the fragment that support window magnification feature
        mSpyResources = spy(mContext.getResources());
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        Context spyContext = spy(mContext);
        when(spyContext.getResources()).thenReturn(mSpyResources);

        setWindowMagnificationSupported(
                /* magnificationAreaSupported= */ true,
                /* windowMagnificationSupported= */ true);

        TestToggleScreenMagnificationPreferenceFragment fragment =
                new TestToggleScreenMagnificationPreferenceFragment();
        fragment.setArguments(new Bundle());
        fragment.setContext(spyContext);

        mFragController = FragmentController.of(fragment, SettingsActivity.class);
    }

    @Test
    public void onResume_defaultStateForFollowingTyping_switchPreferenceShouldReturnTrue() {
        setKeyFollowTypingEnabled(true);

        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        final TwoStatePreference switchPreference =
                mFragController.get().findPreference(
                        MagnificationFollowTypingPreferenceController.PREF_KEY);
        assertThat(switchPreference).isNotNull();
        assertThat(switchPreference.isChecked()).isTrue();
    }

    @Test
    public void onResume_disableFollowingTyping_switchPreferenceShouldReturnFalse() {
        setKeyFollowTypingEnabled(false);

        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        final TwoStatePreference switchPreference =
                mFragController.get().findPreference(
                        MagnificationFollowTypingPreferenceController.PREF_KEY);
        assertThat(switchPreference).isNotNull();
        assertThat(switchPreference.isChecked()).isFalse();
    }

    @Test
    public void onResume_haveRegisterToSpecificUris() {
        ShadowContentResolver shadowContentResolver = Shadows.shadowOf(
                mContext.getContentResolver());
        Uri[] observedUri = new Uri[]{
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS),
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE),
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED),
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED)
        };
        for (Uri uri : observedUri) {
            // verify no observer registered before launching the fragment
            assertThat(shadowContentResolver.getContentObservers(uri)).isEmpty();
        }

        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        for (Uri uri : observedUri) {
            Collection<ContentObserver> observers = shadowContentResolver.getContentObservers(uri);
            assertThat(observers.size()).isEqualTo(1);
            assertThat(observers.stream().findFirst().get()).isInstanceOf(
                    AccessibilitySettingsContentObserver.class);
        }
    }

    @Test
    public void hasValueInSettings_putValue_hasValue() {
        setMagnificationTripleTapEnabled(/* enabled= */ true);

        assertThat(ToggleScreenMagnificationPreferenceFragment.hasMagnificationValuesInSettings(
                mContext, UserShortcutType.TRIPLETAP)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void hasMagnificationValuesInSettings_twoFingerTripleTapIsOn_isTrue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, ON);

        assertThat(ToggleScreenMagnificationPreferenceFragment.hasMagnificationValuesInSettings(
                mContext, UserShortcutType.TWOFINGERTRIPLETAP)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void hasMagnificationValuesInSettings_twoFingerTripleTapIsOff_isFalse() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, OFF);

        assertThat(ToggleScreenMagnificationPreferenceFragment.hasMagnificationValuesInSettings(
                mContext, UserShortcutType.TWOFINGERTRIPLETAP)).isFalse();
    }

    @Test
    public void optInAllValuesToSettings_optInValue_haveMatchString() {
        int shortcutTypes = UserShortcutType.SOFTWARE | UserShortcutType.TRIPLETAP;

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(getMagnificationTripleTapStatus()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void optInAllValuesToSettings_twoFingerTripleTap_haveMatchString() {
        int shortcutTypes = UserShortcutType.TWOFINGERTRIPLETAP;

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                shortcutTypes);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, OFF)).isEqualTo(ON);
    }

    @Test
    public void optInAllValuesToSettings_existOtherValue_optInValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, PLACEHOLDER_COMPONENT_NAME.flattenToString());

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                UserShortcutType.SOFTWARE);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                PLACEHOLDER_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
    }

    @Test
    public void optInAllValuesToSettings_software_sizeValueIsNull_putLargeSizeValue() {
        ShadowSettings.ShadowSecure.reset();

        ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(mContext,
                UserShortcutType.SOFTWARE);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                FloatingMenuSizePreferenceController.Size.UNKNOWN)).isEqualTo(
                FloatingMenuSizePreferenceController.Size.LARGE);
    }

    @Test
    public void optInAllValuesToSettings_software_sizeValueIsNotNull_sizeValueIsNotChanged() {
        for (int size : new int[] {FloatingMenuSizePreferenceController.Size.LARGE,
                FloatingMenuSizePreferenceController.Size.SMALL}) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, size);

            ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(
                    mContext,
                    UserShortcutType.SOFTWARE);

            assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                    FloatingMenuSizePreferenceController.Size.UNKNOWN)).isEqualTo(
                    size);
        }
    }

    @Test
    public void optInAllValuesToSettings_hardware_sizeValueIsNotChanged() {
        for (int size : new int[] {FloatingMenuSizePreferenceController.Size.UNKNOWN,
                FloatingMenuSizePreferenceController.Size.LARGE,
                FloatingMenuSizePreferenceController.Size.SMALL}) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, size);

            ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(
                    mContext,
                    UserShortcutType.HARDWARE);

            assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, size + 1)).isEqualTo(
                    size);
        }
    }

    @Test
    public void optInAllValuesToSettings_tripletap_sizeValueIsNotChanged() {
        for (int size : new int[] {FloatingMenuSizePreferenceController.Size.UNKNOWN,
                FloatingMenuSizePreferenceController.Size.LARGE,
                FloatingMenuSizePreferenceController.Size.SMALL}) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, size);

            ToggleScreenMagnificationPreferenceFragment.optInAllMagnificationValuesToSettings(
                    mContext,
                    UserShortcutType.TRIPLETAP);

            assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, size + 1)).isEqualTo(
                    size);
        }
    }

    @Test
    public void optOutAllValuesToSettings_optOutValue_emptyString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        setMagnificationTripleTapEnabled(/* enabled= */ true);
        int shortcutTypes =
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE | UserShortcutType.TRIPLETAP;

        ToggleScreenMagnificationPreferenceFragment.optOutAllMagnificationValuesFromSettings(
                mContext, shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getMagnificationTripleTapStatus()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void optOutAllValuesToSettings_twoFingerTripleTap_settingsValueIsOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, ON);

        ToggleScreenMagnificationPreferenceFragment.optOutAllMagnificationValuesFromSettings(
                mContext, UserShortcutType.TWOFINGERTRIPLETAP);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, ON)).isEqualTo(OFF);
    }

    @Test
    public void optOutValueFromSettings_existOtherValue_optOutValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY,
                PLACEHOLDER_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY,
                PLACEHOLDER_COMPONENT_NAME.flattenToString() + ":" + MAGNIFICATION_CONTROLLER_NAME);
        int shortcutTypes = UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE;

        ToggleScreenMagnificationPreferenceFragment.optOutAllMagnificationValuesFromSettings(
                mContext, shortcutTypes);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                PLACEHOLDER_COMPONENT_NAME.flattenToString());
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEqualTo(
                PLACEHOLDER_COMPONENT_NAME.flattenToString());
    }

    @Test
    public void updateShortcutPreferenceData_assignDefaultValueToVariable() {
        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        mFragController.get().updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                MAGNIFICATION_CONTROLLER_NAME);
        // Compare to default UserShortcutType
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSettings_assignToVariable() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, MAGNIFICATION_CONTROLLER_NAME);
        setMagnificationTripleTapEnabled(/* enabled= */ true);
        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        mFragController.get().updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(expectedType).isEqualTo(UserShortcutType.SOFTWARE | UserShortcutType.TRIPLETAP);
    }

    @Test
    public void updateShortcutPreferenceData_hasValueInSharedPreference_assignToVariable() {
        final PreferredShortcut tripleTapShortcut = new PreferredShortcut(
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.TRIPLETAP);
        putUserShortcutTypeIntoSharedPreference(mContext, tripleTapShortcut);
        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        mFragController.get().updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(expectedType).isEqualTo(UserShortcutType.TRIPLETAP);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void updateShortcutPreferenceData_hasTwoFingerTripleTapInSettings_assignToVariable() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, ON);
        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        mFragController.get().updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(expectedType).isEqualTo(UserShortcutType.TWOFINGERTRIPLETAP);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void updateShortcutPreferenceData_hasTwoFingerTripleTapInSharedPref_assignToVariable() {
        final PreferredShortcut tripleTapShortcut = new PreferredShortcut(
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.TWOFINGERTRIPLETAP);
        putUserShortcutTypeIntoSharedPreference(mContext, tripleTapShortcut);
        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        mFragController.get().updateShortcutPreferenceData();

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(expectedType).isEqualTo(UserShortcutType.TWOFINGERTRIPLETAP);
    }

    @Test
    public void setupMagnificationEditShortcutDialog_shortcutPreferenceOff_checkboxIsEmptyValue() {
        ToggleScreenMagnificationPreferenceFragment fragment =
                mFragController.create(R.id.main_content, /* bundle= */
                        null).start().resume().get();
        fragment.mShortcutPreference = new ShortcutPreference(mContext, /* attrs= */ null);

        fragment.mShortcutPreference.setChecked(false);
        fragment.setupMagnificationEditShortcutDialog(
                createEditShortcutDialog(fragment.getActivity()));

        final int checkboxValue = fragment.getShortcutTypeCheckBoxValue();
        assertThat(checkboxValue).isEqualTo(UserShortcutType.EMPTY);
    }

    @Test
    public void setupMagnificationEditShortcutDialog_shortcutPreferenceOn_checkboxIsSavedValue() {
        ToggleScreenMagnificationPreferenceFragment fragment =
                mFragController.create(R.id.main_content, /* bundle= */
                        null).start().resume().get();
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        final PreferredShortcut tripletapShortcut = new PreferredShortcut(
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.TRIPLETAP);
        fragment.mShortcutPreference = shortcutPreference;

        PreferredShortcuts.saveUserShortcutType(mContext, tripletapShortcut);
        fragment.mShortcutPreference.setChecked(true);
        fragment.setupMagnificationEditShortcutDialog(
                createEditShortcutDialog(fragment.getActivity()));

        final int checkboxValue = fragment.getShortcutTypeCheckBoxValue();
        assertThat(checkboxValue).isEqualTo(UserShortcutType.TRIPLETAP);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void setupMagnificationEditShortcutDialog_twoFingerTripleTapOn_checkboxIsSavedValue() {
        ToggleScreenMagnificationPreferenceFragment fragment =
                mFragController.create(R.id.main_content, /* bundle= */
                        null).start().resume().get();
        final ShortcutPreference shortcutPreference = new ShortcutPreference(mContext, /* attrs= */
                null);
        final PreferredShortcut twoFingerTripleTapShortcut = new PreferredShortcut(
                MAGNIFICATION_CONTROLLER_NAME, UserShortcutType.TWOFINGERTRIPLETAP);
        fragment.mShortcutPreference = shortcutPreference;

        PreferredShortcuts.saveUserShortcutType(mContext, twoFingerTripleTapShortcut);
        fragment.mShortcutPreference.setChecked(true);
        fragment.setupMagnificationEditShortcutDialog(
                createEditShortcutDialog(fragment.getActivity()));

        final int checkboxValue = fragment.getShortcutTypeCheckBoxValue();
        assertThat(checkboxValue).isEqualTo(UserShortcutType.TWOFINGERTRIPLETAP);
    }

    @Test
    public void restoreValueFromSavedInstanceState_assignToVariable() {
        final Bundle fragmentState = createFragmentSavedInstanceState(
                UserShortcutType.HARDWARE | UserShortcutType.TRIPLETAP);
        ToggleScreenMagnificationPreferenceFragment fragment = mFragController.get();
        // Had to use reflection to pass the savedInstanceState when launching the fragment
        ReflectionHelpers.setField(fragment, "mSavedFragmentState", fragmentState);

        FragmentController.of(fragment, SettingsActivity.class).create(
                R.id.main_content, /* bundle= */ null).start().resume().get();
        fragment.setupMagnificationEditShortcutDialog(
                createEditShortcutDialog(fragment.getActivity()));
        final int value = fragment.getShortcutTypeCheckBoxValue();
        fragment.saveNonEmptyUserShortcutType(value);

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(value).isEqualTo(6);
        assertThat(expectedType).isEqualTo(UserShortcutType.HARDWARE | UserShortcutType.TRIPLETAP);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void restoreValueFromSavedInstanceState_twoFingerTripleTap_assignToVariable() {
        final Bundle fragmentState =
                createFragmentSavedInstanceState(UserShortcutType.TWOFINGERTRIPLETAP);
        ToggleScreenMagnificationPreferenceFragment fragment = mFragController.get();
        // Had to use reflection to pass the savedInstanceState when launching the fragment
        ReflectionHelpers.setField(fragment, "mSavedFragmentState", fragmentState);

        FragmentController.of(fragment, SettingsActivity.class).create(
                R.id.main_content, /* bundle= */ null).start().resume().get();
        fragment.setupMagnificationEditShortcutDialog(
                createEditShortcutDialog(fragment.getActivity()));
        final int value = fragment.getShortcutTypeCheckBoxValue();
        fragment.saveNonEmptyUserShortcutType(value);

        final int expectedType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                MAGNIFICATION_CONTROLLER_NAME);
        assertThat(value).isEqualTo(UserShortcutType.TWOFINGERTRIPLETAP);
        assertThat(expectedType).isEqualTo(UserShortcutType.TWOFINGERTRIPLETAP);
    }

    @Test
    public void onCreateView_magnificationAreaNotSupported_settingsPreferenceIsNull() {
        setWindowMagnificationSupported(
                /* magnificationAreaSupported= */ false,
                /* windowMagnificationSupported= */ true);

        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        assertThat(mFragController.get().mSettingsPreference).isNull();
    }

    @Test
    public void onCreateView_windowMagnificationNotSupported_settingsPreferenceIsNull() {
        setWindowMagnificationSupported(
                /* magnificationAreaSupported= */ true,
                /* windowMagnificationSupported= */ false);

        mFragController.create(R.id.main_content, /* bundle= */ null).start().resume();

        assertThat(mFragController.get().mSettingsPreference).isNull();
    }

    @Test
    public void onCreateView_setDialogDelegateAndAddTheControllerToLifeCycleObserver() {
        Correspondence instanceOf = Correspondence.transforming(
                observer -> (observer instanceof MagnificationModePreferenceController),
                "contains MagnificationModePreferenceController");

        ToggleScreenMagnificationPreferenceFragment fragment = mFragController.create(
                R.id.main_content, /* bundle= */ null).start().resume().get();

        DialogCreatable dialogDelegate = ReflectionHelpers.getField(fragment, "mDialogDelegate");
        List<LifecycleObserver> lifecycleObservers = ReflectionHelpers.getField(
                fragment.getSettingsLifecycle(), "mObservers");
        assertThat(dialogDelegate).isInstanceOf(MagnificationModePreferenceController.class);
        assertThat(lifecycleObservers).isNotNull();
        assertThat(lifecycleObservers).comparingElementsUsing(instanceOf).contains(true);
    }

    @Test
    public void onCreateDialog_setDialogDelegate_invokeDialogDelegate() {
        ToggleScreenMagnificationPreferenceFragment fragment =
                mFragController.create(
                        R.id.main_content, /* bundle= */ null).start().resume().get();
        final DialogCreatable dialogDelegate = mock(DialogCreatable.class, RETURNS_DEEP_STUBS);
        when(dialogDelegate.getDialogMetricsCategory(anyInt())).thenReturn(1);
        fragment.setDialogDelegate(dialogDelegate);

        fragment.onCreateDialog(1);
        fragment.getDialogMetricsCategory(1);

        verify(dialogDelegate).onCreateDialog(1);
        verify(dialogDelegate).getDialogMetricsCategory(1);
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        ToggleScreenMagnificationPreferenceFragment fragment =
                mFragController.create(
                        R.id.main_content, /* bundle= */ null).start().resume().get();

        assertThat(fragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        ToggleScreenMagnificationPreferenceFragment fragment =
                mFragController.create(
                        R.id.main_content, /* bundle= */ null).start().resume().get();

        assertThat(fragment.getHelpResource()).isEqualTo(R.string.help_url_magnification);
    }

    @Test
    public void onProcessArguments_defaultArgumentUnavailable_shouldSetDefaultArguments() {
        ToggleScreenMagnificationPreferenceFragment fragment =
                mFragController.create(
                        R.id.main_content, /* bundle= */ null).start().resume().get();
        Bundle arguments = new Bundle();

        fragment.onProcessArguments(arguments);

        assertTrue(arguments.containsKey(AccessibilitySettings.EXTRA_PREFERENCE_KEY));
        assertTrue(arguments.containsKey(AccessibilitySettings.EXTRA_INTRO));
        assertTrue(arguments.containsKey(AccessibilitySettings.EXTRA_HTML_DESCRIPTION));
    }

    @Test
    public void getSummary_magnificationEnabled_returnShortcutOnWithSummary() {
        setMagnificationTripleTapEnabled(true);

        assertThat(
                ToggleScreenMagnificationPreferenceFragment.getServiceSummary(mContext).toString())
                .isEqualTo(
                        mContext.getString(R.string.preference_summary_default_combination,
                                mContext.getText(R.string.accessibility_summary_shortcut_enabled),
                                mContext.getText(R.string.magnification_feature_summary)));
    }

    @Test
    public void getSummary_magnificationDisabled_returnShortcutOffWithSummary() {
        setMagnificationTripleTapEnabled(false);

        assertThat(
                ToggleScreenMagnificationPreferenceFragment.getServiceSummary(mContext).toString())
                .isEqualTo(
                        mContext.getString(R.string.preference_summary_default_combination,
                                mContext.getText(
                                        R.string.generic_accessibility_feature_shortcut_off),
                                mContext.getText(R.string.magnification_feature_summary)));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void getSummary_magnificationGestureEnabled_returnShortcutOnWithSummary() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, ON);

        assertThat(
                ToggleScreenMagnificationPreferenceFragment.getServiceSummary(mContext).toString())
                .isEqualTo(
                        mContext.getString(R.string.preference_summary_default_combination,
                                mContext.getText(R.string.accessibility_summary_shortcut_enabled),
                                mContext.getText(R.string.magnification_feature_summary)));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void getSummary_magnificationGestureDisabled_returnShortcutOffWithSummary() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), TWO_FINGER_TRIPLE_TAP_SHORTCUT_KEY, OFF);

        assertThat(
                ToggleScreenMagnificationPreferenceFragment.getServiceSummary(mContext).toString())
                .isEqualTo(
                        mContext.getString(R.string.preference_summary_default_combination,
                                mContext.getText(
                                        R.string.generic_accessibility_feature_shortcut_off),
                                mContext.getText(R.string.magnification_feature_summary)));
    }

    private void putStringIntoSettings(String key, String componentName) {
        Settings.Secure.putString(mContext.getContentResolver(), key, componentName);
    }

    private void putUserShortcutTypeIntoSharedPreference(Context context,
            PreferredShortcut shortcut) {
        PreferredShortcuts.saveUserShortcutType(context, shortcut);
    }

    private void setMagnificationTripleTapEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(), TRIPLETAP_SHORTCUT_KEY,
                enabled ? ON : OFF);
    }

    private void setKeyFollowTypingEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_FOLLOW_TYPING,
                enabled ? ON : OFF);
    }

    private String getStringFromSettings(String key) {
        return Settings.Secure.getString(mContext.getContentResolver(), key);
    }

    private boolean getMagnificationTripleTapStatus() {
        return Settings.Secure.getInt(mContext.getContentResolver(), TRIPLETAP_SHORTCUT_KEY, OFF)
                == ON;
    }

    private void callEmptyOnClicked(DialogInterface dialog, int which) {
    }

    private void setWindowMagnificationSupported(boolean magnificationAreaSupported,
            boolean windowMagnificationSupported) {
        when(mSpyResources.getBoolean(
                com.android.internal.R.bool.config_magnification_area))
                .thenReturn(magnificationAreaSupported);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_WINDOW_MAGNIFICATION,
                windowMagnificationSupported);
    }

    private AlertDialog createEditShortcutDialog(Context context) {
        context.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        return AccessibilityDialogUtils.showEditShortcutDialog(
                context,
                DialogType.EDIT_SHORTCUT_MAGNIFICATION, PLACEHOLDER_DIALOG_TITLE,
                this::callEmptyOnClicked);
    }

    private Bundle createFragmentSavedInstanceState(int userShortcutType) {
        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putInt(KEY_SAVED_USER_SHORTCUT_TYPE, userShortcutType);
        final Bundle fragmentState = new Bundle();
        fragmentState.putBundle(
                /* FragmentStateManager.SAVED_INSTANCE_STATE_KEY */ "savedInstanceState",
                savedInstanceState);
        return fragmentState;
    }

    /**
     * A test fragment that provides a way to change the context
     */
    public static class TestToggleScreenMagnificationPreferenceFragment
            extends ToggleScreenMagnificationPreferenceFragment {
        private Context mContext;

        @Override
        public Context getContext() {
            return this.mContext != null ? this.mContext : super.getContext();
        }

        /**
         * Sets the spy context used for RoboTest in order to change the value of
         * com.android.internal.R.bool.config_magnification_area
         */
        public void setContext(Context context) {
            this.mContext = context;
        }
    }
}
