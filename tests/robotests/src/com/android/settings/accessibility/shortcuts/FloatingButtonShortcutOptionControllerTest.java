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

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_GESTURE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;

/**
 * Tests for {@link FloatingButtonShortcutOptionController}
 */
@RunWith(RobolectricTestRunner.class)
public class FloatingButtonShortcutOptionControllerTest {
    private static final String PREF_KEY = "prefKey";
    private static final String TARGET =
            new ComponentName("FakePackage", "FakeClass").flattenToString();
    private final Context mContext = ApplicationProvider.getApplicationContext();
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
        setFloatingButtonEnabled(true);
    }

    @Test
    public void displayPreference_verifyTitle() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mShortcutOptionPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_shortcut_edit_dialog_title_software));
    }

    @Test
    public void getSummary_inSuw_verifySummaryEmpty() {
        mController.setInSetupWizard(true);

        assertThat(TextUtils.isEmpty(mController.getSummary())).isTrue();
    }

    @Test
    public void getSummary_notInSuw_verifySummary() {
        mController.setInSetupWizard(false);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_summary_software_floating));
    }

    @Test
    public void isShortcutAvailable_floatingMenuEnabled_returnTrue() {
        setFloatingButtonEnabled(true);

        assertThat(mController.isShortcutAvailable()).isTrue();
    }

    @Test
    public void isShortcutAvailable_floatingMenuDisabled_returnFalse() {
        setFloatingButtonEnabled(false);

        assertThat(mController.isShortcutAvailable()).isFalse();
    }

    private void setFloatingButtonEnabled(boolean enable) {
        int mode = enable
                ? ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU : ACCESSIBILITY_BUTTON_MODE_GESTURE;

        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, mode);
    }
}
