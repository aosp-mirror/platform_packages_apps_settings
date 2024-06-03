/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.testutils.AccessibilityTestUtils.setupMockAccessibilityManager;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.Flags;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.util.ShortcutUtils;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link QuickSettingsShortcutOptionController}
 */
@Config(shadows = SettingsShadowResources.class)
@RunWith(RobolectricTestRunner.class)
public class QuickSettingsShortcutOptionControllerTest {
    private static final String PREF_KEY = "prefKey";
    private static final ComponentName TARGET = new ComponentName("FakePackage", "FakeClass");
    private static final String TARGET_FLATTEN = TARGET.flattenToString();
    private static final ComponentName TARGET_TILE =
            new ComponentName("FakePackage", "FakeTileClass");
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    private QuickSettingsShortcutOptionController mController;
    private ShortcutOptionPreference mShortcutOptionPreference;
    private AccessibilityManager mAccessibilityManager;

    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_quickSettingsSupported, true);
        mAccessibilityManager = AccessibilityTestUtils.setupMockAccessibilityManager(mContext);
        mController = new QuickSettingsShortcutOptionController(
                mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET_FLATTEN));
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        mShortcutOptionPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mShortcutOptionPreference);
    }

    @Test
    public void displayPreference_verifyScreenTitleSet() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_title_quick_settings));
    }

    @Test
    public void getSummary_touchExplorationDisabled_inSuw_verifySummary() {
        enableTouchExploration(false);
        mController.setInSetupWizard(true);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 1,
                R.string.accessibility_shortcut_edit_dialog_summary_quick_settings_suw);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void getSummary_touchExplorationDisabled_notInSuw_verifySummary() {
        enableTouchExploration(false);
        mController.setInSetupWizard(false);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 1,
                R.string.accessibility_shortcut_edit_dialog_summary_quick_settings);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void getSummary_touchExplorationEnabled_inSuw_verifySummary() {
        enableTouchExploration(true);
        mController.setInSetupWizard(true);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 2,
                R.string.accessibility_shortcut_edit_dialog_summary_quick_settings_suw);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void getSummary_touchExplorationEnabled_notInSuw_verifySummary() {
        enableTouchExploration(true);
        mController.setInSetupWizard(false);
        String expected = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 2,
                R.string.accessibility_shortcut_edit_dialog_summary_quick_settings);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void isShortcutAvailable_a11yQsShortcutFlagDisabled_returnsFalse() {
        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void isShortcutAvailable_qsNotSupported_returnsFalse() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_quickSettingsSupported, false);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void isShortcutAvailable_qsTileProvided_returnsTrue() {
        when(mAccessibilityManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
                .thenReturn(Map.of(TARGET, TARGET_TILE));

        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void isShortcutAvailable_qsTileNotProvided_returnsFalse() {
        when(mAccessibilityManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
                .thenReturn(Collections.emptyMap());

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void isShortcutAvailable_qsTileProvided_invalidUseCase_returnFalse() {
        AccessibilityServiceInfo mockStandardA11yService =
                AccessibilityTestUtils.createAccessibilityServiceInfo(
                        mContext, TARGET, /* isAlwaysOnService= */ false);
        when(mAccessibilityManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
                .thenReturn(Map.of(TARGET, TARGET_TILE));
        // setup target as a standard a11y service
        when(mAccessibilityManager.getInstalledAccessibilityServiceList())
                .thenReturn(List.of(mockStandardA11yService));

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void isShortcutAvailable_qsTileProvided_validUseCase_returnTrue() {
        AccessibilityServiceInfo mockAlwaysOnA11yService =
                AccessibilityTestUtils.createAccessibilityServiceInfo(
                        mContext, TARGET, /* isAlwaysOnService= */ true);
        when(mAccessibilityManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
                .thenReturn(Map.of(TARGET, TARGET_TILE));
        // setup target as a always-on a11y service
        when(mAccessibilityManager.getInstalledAccessibilityServiceList())
                .thenReturn(List.of(mockAlwaysOnA11yService));


        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @Test
    public void isChecked_targetUseQsShortcut_returnTrue() {
        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.QUICK_SETTINGS, TARGET_FLATTEN);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_targetNotUseQsShortcut_returnFalse() {
        ShortcutUtils.optOutValueFromSettings(
                mContext, ShortcutConstants.UserShortcutType.QUICK_SETTINGS, TARGET_FLATTEN);

        assertThat(mController.isChecked()).isFalse();
    }

    private void enableTouchExploration(boolean enable) {
        AccessibilityManager am = setupMockAccessibilityManager(mContext);
        when(am.isTouchExplorationEnabled()).thenReturn(enable);
    }
}
