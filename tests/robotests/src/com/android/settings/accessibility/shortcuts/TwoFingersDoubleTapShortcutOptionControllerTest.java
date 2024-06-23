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

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.icu.text.MessageFormat;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;

/**
 * Tests for {@link TwoFingersDoubleTapShortcutOptionController}
 */
@RunWith(RobolectricTestRunner.class)
public class TwoFingersDoubleTapShortcutOptionControllerTest {
    private static final String PREF_KEY = "prefKey";
    private static final String TARGET_MAGNIFICATION =
            "com.android.server.accessibility.MagnificationController";
    private static final String TARGET_FAKE =
            new ComponentName("FakePackage", "FakeClass").flattenToString();
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TwoFingersDoubleTapShortcutOptionController mController;
    private ShortcutOptionPreference mShortcutOptionPreference;

    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mController = new TwoFingersDoubleTapShortcutOptionController(mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        mShortcutOptionPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mShortcutOptionPreference);
    }

    @Test
    public void displayPreference_verifyScreenTextSet() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_title_two_finger_double_tap));
        assertThat(mShortcutOptionPreference.getSummary().toString()).isEqualTo(
                MessageFormat.format(mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_summary_two_finger_double_tap),
                        2));
    }

    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    public void isShortcutAvailable_featureFlagTurnedOff_returnFalse() {
        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    public void isShortcutAvailable_multipleTargets_returnFalse() {
        mController.setShortcutTargets(Set.of(TARGET_FAKE, TARGET_MAGNIFICATION));

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    public void isShortcutAvailable_magnificationTargetOnly_returnTrue() {
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    public void isShortcutAvailable_nonMagnificationTarget_returnFalse() {
        mController.setShortcutTargets(Set.of(TARGET_FAKE));

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    public void isChecked_twoFingersDoubleTapConfigured_returnTrue() {
        mController.enableShortcutForTargets(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_twoFingersDoubleTapNotConfigured_returnFalse() {
        mController.enableShortcutForTargets(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void enableShortcutForTargets_enableShortcut_settingUpdated() {
        mController.enableShortcutForTargets(true);

        assertThat(
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                        AccessibilityUtil.State.OFF)
        ).isEqualTo(AccessibilityUtil.State.ON);
    }

    @Test
    public void enableShortcutForTargets_disableShortcut_settingUpdated() {
        mController.enableShortcutForTargets(false);

        assertThat(
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                        AccessibilityUtil.State.OFF)
        ).isEqualTo(AccessibilityUtil.State.OFF);
    }
}
