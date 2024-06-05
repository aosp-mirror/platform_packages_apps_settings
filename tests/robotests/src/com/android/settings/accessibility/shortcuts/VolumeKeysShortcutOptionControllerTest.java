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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;

/**
 * Tests for {@link VolumeKeysShortcutOptionController}
 */
@RunWith(RobolectricTestRunner.class)
public class VolumeKeysShortcutOptionControllerTest {

    private static final String PREF_KEY = "prefKey";
    private static final String TARGET =
            new ComponentName("FakePackage", "FakeClass").flattenToString();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    private AccessibilityManager mAccessibilityManager;
    private VolumeKeysShortcutOptionController mController;
    private ShortcutOptionPreference mShortcutOptionPreference;

    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mAccessibilityManager = AccessibilityTestUtils.setupMockAccessibilityManager(mContext);
        mController = new VolumeKeysShortcutOptionController(
                mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET));
        mShortcutOptionPreference = new ShortcutOptionPreference(mContext);
        mShortcutOptionPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mShortcutOptionPreference);
    }

    @Test
    public void displayPreference_verifyScreenTextSet() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_shortcut_edit_dialog_title_hardware));
        assertThat(mShortcutOptionPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_shortcut_edit_dialog_summary_hardware));
    }

    @Test
    public void isShortcutAvailable_returnsTrue() {
        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @Test
    public void isChecked_targetUseVolumeKeyShortcut_returnTrue() {
        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.HARDWARE, TARGET);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_targetNotUseVolumeKeyShortcut_returnFalse() {
        ShortcutUtils.optOutValueFromSettings(
                mContext, ShortcutConstants.UserShortcutType.HARDWARE, TARGET);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableVolumeKeysShortcut_shortcutSet() {
        mController.enableShortcutForTargets(true);

        assertThat(
                ShortcutUtils.isComponentIdExistingInSettings(
                        mContext, ShortcutConstants.UserShortcutType.HARDWARE, TARGET)).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableVolumeKeysShortcut_callA11yManager() {
        mController.enableShortcutForTargets(true);

        verify(mAccessibilityManager).enableShortcutsForTargets(
                /* enable= */ true,
                ShortcutConstants.UserShortcutType.HARDWARE,
                Set.of(TARGET),
                UserHandle.myUserId()
        );
        verifyNoMoreInteractions(mAccessibilityManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableVolumeKeysShortcut_shortcutNotSet() {
        mController.enableShortcutForTargets(false);

        assertThat(
                ShortcutUtils.isComponentIdExistingInSettings(
                        mContext, ShortcutConstants.UserShortcutType.HARDWARE, TARGET)).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableVolumeKeysShortcut_callA11yManager() {
        mController.enableShortcutForTargets(false);

        verify(mAccessibilityManager).enableShortcutsForTargets(
                /* enable= */ false,
                ShortcutConstants.UserShortcutType.HARDWARE,
                Set.of(TARGET),
                UserHandle.myUserId()
        );
        verifyNoMoreInteractions(mAccessibilityManager);
    }
}
