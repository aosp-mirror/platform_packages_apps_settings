/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility.shortcuts;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment.SHORTCUT_SETTINGS;

import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_FIRST_RUN;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;
import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.util.Pair;
import android.view.accessibility.AccessibilityManager;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.dialog.AccessibilityTarget;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.accessibility.AccessibilityUtil;
import com.android.settings.accessibility.PreferredShortcuts;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link EditShortcutsPreferenceFragment}
 */
@Config(shadows = SettingsShadowResources.class)
@RunWith(RobolectricTestRunner.class)
public class EditShortcutsPreferenceFragmentTest {
    private static final int METRICS_CATEGORY = 123;
    private static final CharSequence SCREEN_TITLE = "Fake shortcut title";
    private static final ComponentName TARGET_FAKE_COMPONENT =
            new ComponentName("FakePackage", "FakeClass");
    private static final String TARGET = MAGNIFICATION_CONTROLLER_NAME;
    private static final Set<String> TARGETS = Set.of(TARGET);

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FragmentActivity mActivity;
    private FragmentScenario<EditShortcutsPreferenceFragment> mFragmentScenario;

    @Before
    public void setUp() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_GESTURAL);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE_GESTURE);

        mActivity = Robolectric.buildActivity(FragmentActivity.class).get();
    }

    @After
    public void cleanUp() {
        if (mFragmentScenario != null) {
            mFragmentScenario.close();
        }
    }

    @Test
    public void showEditShortcutScreen_targetIsMagnification_launchSubSetting() {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
                mActivity, METRICS_CATEGORY, SCREEN_TITLE,
                MAGNIFICATION_COMPONENT_NAME, /* fromIntent= */ null);

        assertLaunchSubSettingWithCurrentTargetComponents(
                MAGNIFICATION_CONTROLLER_NAME, /* isInSuw= */ false);
    }

    @Test
    public void showEditShortcutScreen_launchSubSetting() {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
                mActivity, METRICS_CATEGORY, SCREEN_TITLE,
                TARGET_FAKE_COMPONENT, /* fromIntent= */ null);

        assertLaunchSubSettingWithCurrentTargetComponents(
                TARGET_FAKE_COMPONENT.flattenToString(), /* isInSuw= */ false);
    }

    @Test
    public void showEditShortcutScreen_inSuw_launchSubSettingWithSuw() {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
                mActivity, METRICS_CATEGORY, SCREEN_TITLE,
                TARGET_FAKE_COMPONENT, createSuwIntent(new Intent(), /* isInSuw= */ true));

        assertLaunchSubSettingWithCurrentTargetComponents(
                TARGET_FAKE_COMPONENT.flattenToString(), /* isInSuw= */ true);
    }

    @Test
    public void fragmentCreated_inSuw_controllersTargetsSet() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ true);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        mFragmentScenario.onFragment(fragment -> {
            List<ShortcutOptionPreferenceController> controllers =
                    getShortcutOptionPreferenceControllers(fragment);

            for (ShortcutOptionPreferenceController controller : controllers) {
                assertThat(controller.getShortcutTargets()).containsExactlyElementsIn(TARGETS);
                assertThat(controller.isInSetupWizard()).isTrue();
            }
        });
    }

    @Test
    public void fragmentCreated_notInSuw_controllersTargetsSet() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        mFragmentScenario.onFragment(fragment -> {
            List<ShortcutOptionPreferenceController> controllers =
                    getShortcutOptionPreferenceControllers(fragment);

            for (ShortcutOptionPreferenceController controller : controllers) {
                assertThat(controller.getShortcutTargets()).containsExactlyElementsIn(TARGETS);
                assertThat(controller.isInSetupWizard()).isFalse();
            }
        });
    }

    @Test
    public void fragmentCreated_settingsObserversAreRegistered() {
        ShadowContentResolver contentResolver = shadowOf(mContext.getContentResolver());
        for (Uri uri : SHORTCUT_SETTINGS) {
            assertThat(contentResolver.getContentObservers(uri)).isEmpty();
        }

        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        for (Uri uri : SHORTCUT_SETTINGS) {
            assertThat(contentResolver.getContentObservers(uri)).isNotEmpty();
        }
    }

    @Test
    public void fragmentDestroyed_unregisterSettingsObserver() {
        ShadowContentResolver contentResolver = shadowOf(mContext.getContentResolver());

        mFragmentScenario = createFragScenario(/* isInSuw= */ false)
                .moveToState(Lifecycle.State.CREATED);
        mFragmentScenario.onFragment(EditShortcutsPreferenceFragment::onDestroy);

        for (Uri uri : SHORTCUT_SETTINGS) {
            assertThat(contentResolver.getContentObservers(uri)).isEmpty();
        }
    }

    @Test
    public void onVolumeKeysShortcutSettingChanged_volumeKeyControllerUpdated() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.HARDWARE, TARGET);

        mFragmentScenario.onFragment(fragment -> {
            TwoStatePreference preference = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcut_volume_keys_pref));
            assertThat(preference.isChecked()).isTrue();
        });
    }

    @Test
    public void onSoftwareShortcutSettingChanged_softwareControllersUpdated() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, TARGET);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        mFragmentScenario.onFragment(fragment -> {
            TwoStatePreference preference = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcut_gesture_pref));
            assertThat(preference.isChecked()).isTrue();
        });
    }

    @Test
    public void onSoftwareShortcutModeChanged_softwareControllersUpdated() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, TARGET);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        mFragmentScenario.onFragment(fragment -> {
            TwoStatePreference preference = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcut_gesture_pref));
            assertThat(preference.isChecked()).isTrue();
        });
    }

    @Test
    public void onTripleTapShortcutSettingChanged_tripleTapShortcutControllerUpdated() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.ON);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        mFragmentScenario.onFragment(fragment -> {
            TwoStatePreference preference = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcut_triple_tap_pref));
            assertThat(preference.isChecked()).isTrue();
        });
    }

    @Test
    public void onTwoFingersShortcutSettingChanged_twoFingersDoubleTapShortcutControllerUpdated() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                AccessibilityUtil.State.ON);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        mFragmentScenario.onFragment(fragment -> {
            TwoStatePreference preference = fragment.findPreference(
                    mContext.getString(
                            R.string.accessibility_shortcut_two_fingers_double_tap_pref));
            assertThat(preference.isChecked()).isTrue();
        });
    }

    @Test
    public void fragmentResumed_enableTouchExploration_gestureShortcutOptionSummaryUpdated() {
        String expectedSummary = mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_gesture_talkback)
                + "\n\n"
                + mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_summary_software_floating);
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);

        ShadowAccessibilityManager am = shadowOf(
                mContext.getSystemService(AccessibilityManager.class));
        am.setTouchExplorationEnabled(true);

        mFragmentScenario.onFragment(fragment -> {
            Preference preference = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcut_gesture_pref));
            assertThat(preference.getSummary().toString()).isEqualTo(expectedSummary);
        });
    }

    @Test
    public void fragmentPaused_enableTouchExploration_gestureShortcutOptionSummaryNotUpdated() {
        String expectedSummary = mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_gesture)
                + "\n\n"
                + mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_summary_software_floating);
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.RESUMED).moveToState(Lifecycle.State.STARTED);

        ShadowAccessibilityManager am = shadowOf(
                mContext.getSystemService(AccessibilityManager.class));
        am.setTouchExplorationEnabled(true);

        mFragmentScenario.onFragment(fragment -> {
            Preference preference = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcut_gesture_pref));
            assertThat(preference.getSummary().toString()).isEqualTo(expectedSummary);
        });
    }

    @Test
    public void onAdvancedPreferenceClicked_advancedShouldBecomeInvisible() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);
        mFragmentScenario.onFragment(fragment -> {
            Preference advanced = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcuts_advanced_collapsed));
            assertThat(advanced.isVisible()).isTrue();

            fragment.onPreferenceTreeClick(advanced);

            assertThat(advanced.isVisible()).isFalse();
        });
    }

    @Test
    public void fragmentRecreated_expanded_advancedRemainInvisible() {
        onAdvancedPreferenceClicked_advancedShouldBecomeInvisible();

        mFragmentScenario.recreate();

        mFragmentScenario.onFragment(fragment -> {
            Preference advanced = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcuts_advanced_collapsed));
            assertThat(advanced.isVisible()).isFalse();
        });
    }

    @Test
    public void fragmentRecreated_collapsed_advancedRemainVisible() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);

        mFragmentScenario.recreate();

        mFragmentScenario.onFragment(fragment -> {
            Preference advanced = fragment.findPreference(
                    mContext.getString(R.string.accessibility_shortcuts_advanced_collapsed));
            assertThat(advanced.isVisible()).isTrue();
        });
    }

    @Test
    public void fragmentResumed_preferredShortcutsUpdated() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);
        // Move the fragment to the background
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);
        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, TARGET)
        ).isEqualTo(ShortcutConstants.UserShortcutType.SOFTWARE);
        // Update the chosen shortcut type to Volume keys while the fragment is in the background
        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.HARDWARE, TARGET);

        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, TARGET)
        ).isEqualTo(ShortcutConstants.UserShortcutType.HARDWARE);
    }

    @Test
    public void onVolumeKeysShortcutSettingChanged_preferredShortcutsUpdated() {
        mFragmentScenario = createFragScenario(/* isInSuw= */ false);
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);
        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, TARGET)
        ).isEqualTo(ShortcutConstants.UserShortcutType.SOFTWARE);

        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.HARDWARE, TARGET);

        // Calls onFragment so that the change to Setting is notified to its observer
        mFragmentScenario.onFragment(fragment ->
                assertThat(
                        PreferredShortcuts.retrieveUserShortcutType(
                                mContext, TARGET)
                ).isEqualTo(ShortcutConstants.UserShortcutType.HARDWARE)
        );

    }

    @Test
    public void findTitles_withSingleTarget_hasNullSubtitle() {
        final String fake_label = "FAKE";
        List<AccessibilityTarget> accessibilityTargets = List.of(
                generateAccessibilityTargetMock(TARGET_FAKE_COMPONENT, fake_label));

        Pair<String, String> titles = EditShortcutsPreferenceFragment
                .getTitlesFromAccessibilityTargetList(
                        Set.of(TARGET_FAKE_COMPONENT.flattenToString()),
                        accessibilityTargets, mActivity.getResources()
                );

        assertThat(titles.first).isNotNull();
        assertThat(titles.first).contains(fake_label);
        assertThat(titles.second).isNull();
    }

    @Test
    public void findTitles_withMoreTargets_hasSubtitle() {
        final String fake_label = "FAKE";
        final String magnification_label = "MAGNIFICATION";
        List<AccessibilityTarget> accessibilityTargets = List.of(
                generateAccessibilityTargetMock(TARGET_FAKE_COMPONENT, fake_label),
                generateAccessibilityTargetMock(MAGNIFICATION_COMPONENT_NAME, magnification_label));

        Pair<String, String> titles = EditShortcutsPreferenceFragment
                .getTitlesFromAccessibilityTargetList(
                        Set.of(TARGET_FAKE_COMPONENT.flattenToString(),
                                MAGNIFICATION_COMPONENT_NAME.flattenToString()),
                        accessibilityTargets, mActivity.getResources()
                );

        assertThat(titles.first).isNotNull();
        assertThat(titles.second).isNotNull();
        assertThat(titles.second).contains(fake_label);
        assertThat(titles.second).contains(magnification_label);
    }

    @Test
    public void findTitles_targetMissing_labelNotInTitles() {
        final String fake_label = "FAKE";
        List<AccessibilityTarget> accessibilityTargets = List.of(
                generateAccessibilityTargetMock(TARGET_FAKE_COMPONENT, fake_label));

        assertThrows(IllegalStateException.class,
                () -> EditShortcutsPreferenceFragment
                        .getTitlesFromAccessibilityTargetList(
                                Set.of(MAGNIFICATION_COMPONENT_NAME.flattenToString()),
                                accessibilityTargets, mActivity.getResources()
                        ));
    }



    private void assertLaunchSubSettingWithCurrentTargetComponents(
            String componentName, boolean isInSuw) {
        Intent intent = shadowOf(mActivity.getApplication()).getNextStartedActivity();

        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent()).isEqualTo(
                new ComponentName(mActivity, SubSettings.class));
        assertThat(intent.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(EditShortcutsPreferenceFragment.class.getName());
        assertThat(intent.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE))
                .isEqualTo(SCREEN_TITLE.toString());
        assertThat(intent.getExtra(
                MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY)).isEqualTo(METRICS_CATEGORY);
        Bundle args = (Bundle) intent.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(args).isNotNull();
        assertThat(Arrays.stream(args.getStringArray(
                EditShortcutsPreferenceFragment.ARG_KEY_SHORTCUT_TARGETS)).toList())
                .containsExactly(componentName);
        assertThat(WizardManagerHelper.isAnySetupWizard(intent)).isEqualTo(isInSuw);
    }

    private List<ShortcutOptionPreferenceController> getShortcutOptionPreferenceControllers(
            EditShortcutsPreferenceFragment fragment) {

        Collection<List<AbstractPreferenceController>> controllers =
                ReflectionHelpers.callInstanceMethod(fragment, "getPreferenceControllers");
        List<ShortcutOptionPreferenceController> retControllers = new ArrayList<>();
        controllers.stream().flatMap(Collection::stream)
                .filter(controller -> controller instanceof ShortcutOptionPreferenceController)
                .forEach(controller ->
                        retControllers.add((ShortcutOptionPreferenceController) controller));

        return retControllers;
    }

    private FragmentScenario<EditShortcutsPreferenceFragment> createFragScenario(boolean isInSuw) {
        Bundle args = new Bundle();
        args.putStringArray(
                EditShortcutsPreferenceFragment.ARG_KEY_SHORTCUT_TARGETS, new String[]{TARGET});
        FragmentScenario<EditShortcutsPreferenceFragment> scenario =
                FragmentScenario.launch(
                        EditShortcutsPreferenceFragment.class, args,
                        /* themeResId= */ 0, Lifecycle.State.INITIALIZED);
        scenario.onFragment(fragment -> {
            Intent intent = fragment.requireActivity().getIntent();
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE, SCREEN_TITLE);
            fragment.requireActivity().setIntent(createSuwIntent(intent, isInSuw));
            // Since the fragment is attached before we have a chance
            // to modify the activity's intent; initialize controllers again
            fragment.initializePreferenceControllerArguments();
        });
        return scenario;
    }

    private Intent createSuwIntent(Intent intent, boolean isInSuw) {

        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra(EXTRA_IS_SETUP_FLOW, isInSuw);
        intent.putExtra(EXTRA_IS_FIRST_RUN, isInSuw);
        intent.putExtra(EXTRA_IS_PRE_DEFERRED_SETUP, isInSuw);
        intent.putExtra(EXTRA_IS_DEFERRED_SETUP, isInSuw);
        return intent;
    }

    private AccessibilityTarget generateAccessibilityTargetMock(
            ComponentName componentName, String label) {
        AccessibilityTarget target = mock(AccessibilityTarget.class);
        when(target.getComponentName()).thenReturn(componentName);
        when(target.getId()).thenReturn(componentName.flattenToString());
        when(target.getLabel()).thenReturn(label);
        return target;
    }
}
