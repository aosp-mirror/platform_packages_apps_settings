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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.Flags;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;
import com.android.settings.testutils.AccessibilityTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;

/**
 * Tests for {@link TripleTapShortcutOptionController}
 */
@RunWith(RobolectricTestRunner.class)
public class TripleTapShortcutOptionControllerTest {
    private static final String PREF_KEY = "prefKey";
    private static final String TARGET_MAGNIFICATION =
            "com.android.server.accessibility.MagnificationController";
    private static final String TARGET_FAKE =
            new ComponentName("FakePackage", "FakeClass").flattenToString();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    private TripleTapShortcutOptionController mController;
    private ShortcutOptionPreference mShortcutOptionPreference;
    private AccessibilityManager mAccessibilityManager;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mAccessibilityManager = AccessibilityTestUtils.setupMockAccessibilityManager(mContext);
        mController = new TripleTapShortcutOptionController(mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        mShortcutOptionPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mShortcutOptionPreference);
    }

    @Test
    public void displayPreference_verifyTitleSummaryText() {
        String expectedTitle = mContext.getString(
                R.string.accessibility_shortcut_edit_screen_title_triple_tap);
        String expectedSummary = mContext.getString(
                R.string.accessibility_shortcut_edit_screen_summary_triple_tap, 3);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(expectedTitle);
        assertThat(mShortcutOptionPreference.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getAvailabilityStatus_targetIsMagnificationAndIsExpanded_returnsAvailableUnsearchable() {
        mController.setExpanded(true);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_targetIsMagnificationAndIsNotExpanded_returnsConditionallyUnavailable() {
        mController.setExpanded(false);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_targetIsNotMagnificationAndIsNotExpanded_returnsConditionallyUnavailable() {
        mController.setExpanded(false);
        mController.setShortcutTargets(Set.of(TARGET_FAKE));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_targetIsNotMagnificationAndIsExpanded_returnsConditionallyUnavailable() {
        mController.setExpanded(true);
        mController.setShortcutTargets(Set.of(TARGET_FAKE));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void setExpanded_expand_updateExpandedValue() {
        mController.setExpanded(true);

        assertThat(mController.isExpanded()).isTrue();
    }

    @Test
    public void setExpanded_collapse_updateExpandedValue() {
        mController.setExpanded(false);

        assertThat(mController.isExpanded()).isFalse();
    }

    @Test
    public void isShortcutAvailable_multipleTargets_returnFalse() {
        mController.setShortcutTargets(Set.of(TARGET_FAKE, TARGET_MAGNIFICATION));

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    public void isShortcutAvailable_magnificationTargetOnly_returnTrue() {
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @Test
    public void isShortcutAvailable_nonMagnificationTarget_returnFalse() {
        mController.setShortcutTargets(Set.of(TARGET_FAKE));

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    public void isChecked_tripleTapConfigured_returnTrue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.ON);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_tripleTapNotConfigured_returnFalse() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.OFF);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableShortcut_settingUpdated() {
        mController.enableShortcutForTargets(true);

        assertThat(
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                        AccessibilityUtil.State.OFF)
        ).isEqualTo(AccessibilityUtil.State.ON);
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableShortcut_callA11yManager() {
        mController.enableShortcutForTargets(true);

        verify(mAccessibilityManager).enableShortcutsForTargets(
                /* enable= */ true,
                ShortcutConstants.UserShortcutType.TRIPLETAP,
                Set.of(TARGET_MAGNIFICATION),
                UserHandle.myUserId()
        );
        verifyNoMoreInteractions(mAccessibilityManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableShortcut_settingUpdated() {
        mController.enableShortcutForTargets(false);

        assertThat(
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                        AccessibilityUtil.State.OFF)
        ).isEqualTo(AccessibilityUtil.State.OFF);
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableShortcut_callA11yManager() {
        mController.enableShortcutForTargets(false);

        verify(mAccessibilityManager).enableShortcutsForTargets(
                /* enable= */ false,
                ShortcutConstants.UserShortcutType.TRIPLETAP,
                Set.of(TARGET_MAGNIFICATION),
                UserHandle.myUserId()
        );
        verifyNoMoreInteractions(mAccessibilityManager);
    }
}
