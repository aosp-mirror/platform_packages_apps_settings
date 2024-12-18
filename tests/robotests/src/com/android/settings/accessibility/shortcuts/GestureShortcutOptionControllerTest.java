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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.settings.testutils.AccessibilityTestUtils.setupMockAccessibilityManager;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Flags;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.AccessibilityTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.utils.StringUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Set;

/**
 * Tests for {@link GestureShortcutOptionController}
 */
@Config(shadows = SettingsShadowResources.class)
@RunWith(RobolectricTestRunner.class)
public class GestureShortcutOptionControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String PREF_KEY = "prefKey";
    private static final String TARGET =
            new ComponentName("FakePackage", "FakeClass").flattenToString();
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    private GestureShortcutOptionController mController;
    private ShortcutOptionPreference mShortcutOptionPreference;

    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mController = new GestureShortcutOptionController(
                mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET));
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        mShortcutOptionPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mShortcutOptionPreference);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);
        enableTouchExploration(false);
    }

    @Test
    public void displayPreference_verifyTitle() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_title_software_by_gesture));
    }

    @Test
    public void getSummary_touchExplorationDisabled_notInSuw_verifySummary() {
        enableTouchExploration(false);
        mController.setInSetupWizard(false);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 2,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture)
                + "\n\n"
                + mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_floating);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void getSummary_touchExplorationDisabled_inSuw_verifySummary() {
        enableTouchExploration(false);
        mController.setInSetupWizard(true);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 2,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getSummary_touchExplorationEnabled_notInSuw_verifySummary() {
        enableTouchExploration(true);
        mController.setInSetupWizard(false);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 3,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture)
                + "\n\n"
                + mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_floating);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getSummary_touchExplorationEnabled_notInSuw_gestureFlag_verifySummary() {
        enableTouchExploration(true);
        mController.setInSetupWizard(false);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 3,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void getSummary_touchExplorationEnabled_inSuw_verifySummary() {
        enableTouchExploration(true);
        mController.setInSetupWizard(true);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 3,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getSummary_standaloneGestureFlagOn_verifyNoCustomizeA11yButtonTest() {
        enableTouchExploration(true);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 3,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void isShortcutAvailable_inSuw_returnFalse() {
        mController.setInSetupWizard(true);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void isShortcutAvailable_notInSuwUseGestureNavSystemUseFab_returnFalse() {
        mController.setInSetupWizard(false);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ true);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    public void isShortcutAvailable_notInSuwUseGestureNavSystemNotUseFab_returnTrue() {
        mController.setInSetupWizard(false);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);

        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @Test
    public void isShortcutAvailable_notInSuwUseButtonNavSystemUseFab_returnFalse() {
        mController.setInSetupWizard(false);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ true);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    public void isShortcutAvailable_notInSuwUseButtonNavSystemNotUseFab_returnFalse() {
        mController.setInSetupWizard(false);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ false);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    @Test
    public void isShortcutAvailable_floatingMenuEnabled_gestureNavEnabled_returnsTrue() {
        mController.setInSetupWizard(false);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ true);

        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    @Test
    public void getShortcutType_gesture() {
        assertThat(mController.getShortcutType()).isEqualTo(GESTURE);
    }

    @DisableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    @Test
    public void getShortcutType_software() {
        assertThat(mController.getShortcutType()).isEqualTo(SOFTWARE);
    }

    private void enableTouchExploration(boolean enable) {
        AccessibilityManager am = setupMockAccessibilityManager(mContext);
        when(am.isTouchExplorationEnabled()).thenReturn(enable);
    }
}
