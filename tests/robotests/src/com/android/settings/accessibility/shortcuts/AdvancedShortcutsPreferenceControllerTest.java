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

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;

/**
 * Tests for {@link AdvancedShortcutsPreferenceController}
 */
@RunWith(RobolectricTestRunner.class)
public class AdvancedShortcutsPreferenceControllerTest {
    private static final String PREF_KEY = "prefKey";
    private static final String TARGET_MAGNIFICATION =
            "com.android.server.accessibility.MagnificationController";
    private static final String TARGET_FAKE =
            new ComponentName("FakePackage", "FakeClass").flattenToString();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AdvancedShortcutsPreferenceController mController;

    @Before
    public void setUp() {
        mController = new AdvancedShortcutsPreferenceController(mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY);
        PreferenceScreen preferenceScreen =
                new PreferenceManager(mContext).createPreferenceScreen(mContext);
        preferenceScreen.addPreference(preference);
    }

    @Test
    public void getAvailabilityStatus_targetIsMagnificationAndIsExpanded_returnsConditionallyUnavailable() {
        mController.setExpanded(true);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_targetIsMagnificationAndIsNotExpanded_returnsAvailableUnsearchable() {
        mController.setExpanded(false);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
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
    public void isChecked_returnFalse() {
        assertThat(mController.isChecked()).isFalse();
    }
}
