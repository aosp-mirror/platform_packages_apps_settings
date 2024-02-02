/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.biometrics;

import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FACE;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FINGERPRINT;

import static com.google.common.truth.Truth.assertThat;

import android.content.DialogInterface;
import android.hardware.biometrics.BiometricAuthenticator;
import android.os.Looper;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class BiometricsSplitScreenDialogTest {
    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    private FragmentActivity mActivity;
    private BiometricsSplitScreenDialog mFragment;

    @Before
    public void setUp() {
        ShadowAlertDialogCompat.reset();
        mActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
    }

    @After
    public void tearDown() {
        ShadowAlertDialogCompat.reset();
    }

    @Test
    public void testTexts_face() {
        final AlertDialog dialog = setUpFragment(TYPE_FACE, false /*destroyActivity*/);

        final ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                mActivity.getString(R.string.biometric_settings_add_face_in_split_mode_title));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.biometric_settings_add_face_in_split_mode_message));
    }

    @Test
    public void testTexts_fingerprint() {
        final AlertDialog dialog = setUpFragment(TYPE_FINGERPRINT, false /*destroyActivity*/);

        final ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                mActivity.getString(
                        R.string.biometric_settings_add_fingerprint_in_split_mode_title));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(
                        R.string.biometric_settings_add_fingerprint_in_split_mode_message));
    }

    @Test
    public void testButton_destroyActivity() {
        final AlertDialog dialog = setUpFragment(TYPE_FACE, true /*destroyActivity*/);
        final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

        assertThat(button).isNotNull();
        button.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertThat(dialog.isShowing()).isFalse();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void testButton_notDestroyActivity() {
        final AlertDialog dialog = setUpFragment(TYPE_FACE, false /*destroyActivity*/);
        final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

        assertThat(button).isNotNull();
        button.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertThat(dialog.isShowing()).isFalse();
        assertThat(mActivity.isFinishing()).isFalse();
    }

    private AlertDialog setUpFragment(
            @BiometricAuthenticator.Modality int biometricsModality, boolean destroyActivity) {
        mFragment = BiometricsSplitScreenDialog.newInstance(biometricsModality, destroyActivity);
        mFragment.show(mActivity.getSupportFragmentManager(), null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        return dialog;
    }
}
