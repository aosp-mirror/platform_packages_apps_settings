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

import android.content.ComponentName;
import android.content.Context;
import android.icu.text.MessageFormat;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;

import org.junit.Before;
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
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TripleTapShortcutOptionController mController;
    private ShortcutOptionPreference mShortcutOptionPreference;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mController = new TripleTapShortcutOptionController(mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        mShortcutOptionPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mShortcutOptionPreference);
    }

    @Test
    public void displayPreference_verifyScreenTestSet() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_shortcut_edit_dialog_title_triple_tap));
        assertThat(mShortcutOptionPreference.getSummary().toString()).isEqualTo(
                MessageFormat.format(
                        mContext.getString(
                                R.string.accessibility_shortcut_edit_dialog_summary_triple_tap),
                        3));
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
        mController.enableShortcutForTargets(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_tripleTapNotConfigured_returnFalse() {
        mController.enableShortcutForTargets(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
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
    public void enableShortcutForTargets_disableShortcut_settingUpdated() {
        mController.enableShortcutForTargets(false);

        assertThat(
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                        AccessibilityUtil.State.OFF)
        ).isEqualTo(AccessibilityUtil.State.OFF);
    }
}
