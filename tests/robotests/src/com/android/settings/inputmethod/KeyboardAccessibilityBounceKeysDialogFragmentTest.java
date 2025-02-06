/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.android.settings.inputmethod.KeyboardAccessibilityKeysDialogFragment.EXTRA_SUBTITLE_RES;
import static com.android.settings.inputmethod.KeyboardAccessibilityKeysDialogFragment.EXTRA_TITLE_RES;

import static com.google.common.truth.Truth.assertThat;

import android.app.AlertDialog;
import android.hardware.input.InputSettings;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class KeyboardAccessibilityBounceKeysDialogFragmentTest {
    private AlertDialog mAlertDialog;

    @Before
    public void setUp() {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_TITLE_RES, R.string.bounce_keys_dialog_title);
        bundle.putInt(EXTRA_SUBTITLE_RES, R.string.bounce_keys_dialog_subtitle);

        FragmentScenario<KeyboardAccessibilityBounceKeysDialogFragment> mFragmentScenario =
                FragmentScenario.launch(
                        KeyboardAccessibilityBounceKeysDialogFragment.class,
                        bundle,
                        R.style.Theme_AlertDialog_SettingsLib,
                        Lifecycle.State.INITIALIZED);
        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);

        mFragmentScenario.onFragment(fragment -> {
            assertThat(fragment.getDialog()).isNotNull();
            assertThat(fragment.requireDialog().isShowing()).isTrue();
            assertThat(fragment.requireDialog()).isInstanceOf(AlertDialog.class);
            mAlertDialog = (AlertDialog) fragment.requireDialog();
        });
    }

    @Test
    public void handlePreferenceTreeClick_performClickOn200_updatesBounceKeysThreshold() {
        assertThat(mAlertDialog.isShowing()).isTrue();
        RadioGroup radioGroup = mAlertDialog.findViewById(R.id.input_setting_keys_value_group);
        radioGroup.check(R.id.input_setting_keys_value_200);

        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertThat(mAlertDialog.isShowing()).isFalse();
        int threshold = InputSettings.getAccessibilityBounceKeysThreshold(
                ApplicationProvider.getApplicationContext());
        assertThat(threshold).isEqualTo(200);
    }
}
