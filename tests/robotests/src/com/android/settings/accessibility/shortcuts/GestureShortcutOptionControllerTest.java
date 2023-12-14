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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.AccessibilityTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
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
    public void getSummary_touchExplorationDisabled_verifySummary() {
        enableTouchExploration(false);
        String expected = mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_gesture)
                + "\n\n"
                + mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_floating);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void getSummary_touchExplorationEnabled_verifySummary() {
        enableTouchExploration(true);
        String expected = mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_gesture_talkback)
                + "\n\n"
                + mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_floating);

        assertThat(mController.getSummary().toString()).isEqualTo(expected);
    }

    @Test
    public void isShortcutAvailable_inSuw_returnFalse() {
        mController.setInSetupWizard(true);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
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

    private void enableTouchExploration(boolean enable) {
        AccessibilityManager am = mock(AccessibilityManager.class);
        when(mContext.getSystemService(AccessibilityManager.class)).thenReturn(am);
        when(am.isTouchExplorationEnabled()).thenReturn(enable);
    }
}
