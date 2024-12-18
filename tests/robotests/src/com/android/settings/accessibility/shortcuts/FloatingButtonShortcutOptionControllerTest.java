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

import static org.mockito.Mockito.spy;

import android.content.ComponentName;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Flags;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.AccessibilityTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Set;

/**
 * Tests for {@link FloatingButtonShortcutOptionController}
 */
@Config(shadows = SettingsShadowResources.class)
@RunWith(RobolectricTestRunner.class)
public class FloatingButtonShortcutOptionControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREF_KEY = "prefKey";
    private static final String TARGET =
            new ComponentName("FakePackage", "FakeClass").flattenToString();
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    private FloatingButtonShortcutOptionController mController;
    private ShortcutOptionPreference mShortcutOptionPreference;

    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mController = new FloatingButtonShortcutOptionController(
                mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET));
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        mShortcutOptionPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mShortcutOptionPreference);
    }

    @Test
    public void displayPreference_verifyTitle() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_shortcut_edit_dialog_title_software));
    }

    @Test
    public void getSummary_inSuw_verifySummary() {
        String expectedSummary = mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_floating_button);
        mController.setInSetupWizard(true);

        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_notInSuw_verifySummary() {
        String expectedSummary = mContext.getText(
                R.string.accessibility_shortcut_edit_dialog_summary_floating_button)
                + "\n\n"
                + mContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_floating);
        mController.setInSetupWizard(false);

        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void isShortcutAvailable_floatingMenuEnabled_returnTrue() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ true);

        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @Test
    public void isShortcutAvailable_floatingMenuDisabled_returnFalse() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ false);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void isShortcutAvailable_gestureNavigationMode_returnsTrue() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);

        assertThat(mController.isShortcutAvailable()).isTrue();
    }
}
