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
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;

/**
 * Tests for {@link ShortcutOptionPreferenceController}
 */
@RunWith(RobolectricTestRunner.class)
public class ShortcutOptionPreferenceControllerTest {

    private static final String PREF_KEY = "prefKey";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ShortcutOptionPreference mShortcutOptionPreference;
    private ShortcutOptionPreferenceController mController;

    @Before
    public void setUp() {
        mShortcutOptionPreference = spy(new ShortcutOptionPreference(mContext));
        mShortcutOptionPreference.setKey(PREF_KEY);
        mController = spy(new ShortcutOptionPreferenceController(mContext, PREF_KEY) {
            @Override
            protected boolean isShortcutAvailable() {
                return false;
            }

            @Override
            protected boolean isChecked() {
                return false;
            }

            @Override
            protected void enableShortcutForTargets(boolean enable) {
                // do nothing
            }
        });
    }

    @Test
    public void updateState_shortcutControllerIsChecked_shouldSetPreferenceChecked() {
        when(mController.isChecked()).thenReturn(true);

        mController.updateState(mShortcutOptionPreference);

        assertThat(mShortcutOptionPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_shortcutControllerIsNotChecked_shouldSetPreferenceUnchecked() {
        when(mController.isChecked()).thenReturn(false);

        mController.updateState(mShortcutOptionPreference);

        assertThat(mShortcutOptionPreference.isChecked()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_shortcutAvailable_returnAvailableUnsearchable() {
        when(mController.isShortcutAvailable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_shortcutUnavailable_returnConditionallyUnavailable() {
        when(mController.isShortcutAvailable()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void onPreferenceChanged_callEnableShortcutForTargets() {
        mController.onPreferenceChange(mShortcutOptionPreference, true);
        mController.onPreferenceChange(mShortcutOptionPreference, false);

        InOrder inOrder = Mockito.inOrder(mController);
        inOrder.verify(mController).enableShortcutForTargets(true);
        inOrder.verify(mController).enableShortcutForTargets(false);
    }

    @Test
    public void getShortcutTargets() {
        Set<String> targets = Set.of("target1", "target2");
        mController.setShortcutTargets(targets);

        assertThat(mController.getShortcutTargets())
                .containsExactlyElementsIn(targets);
    }

    @Test
    public void isInSetupWizard() {
        mController.setInSetupWizard(true);

        assertThat(mController.isInSetupWizard()).isTrue();
    }
}
