/*
 * Copyright (C) 2017 Google Inc.
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

package com.android.settings.password;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {ShadowUtils.class, ShadowAlertDialog.class})
public class SetupSkipDialogTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;
    private FragmentActivity mActivity;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        ShadowUtils.setFaceManager(mFaceManager);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mActivity = Robolectric.setupActivity(FragmentActivity.class);

        when(mFakeFeatureFactory.mFaceFeatureProvider.isSetupWizardSupported(any())).thenReturn(
                true);
    }

    private ShadowAlertDialog getShadowAlertDialog() {
        ShadowApplication shadowApplication = Shadow.extract(
                ApplicationProvider.getApplicationContext());
        ShadowAlertDialog shadowAlertDialog = shadowApplication.getLatestAlertDialog();
        assertThat(shadowAlertDialog).isNotNull();
        return shadowAlertDialog;
    }

    private String getSkipSetupTitle(int credentialType, boolean hasFingerprint,
            boolean hasFace) {
        final int screenLockResId;
        switch (credentialType) {
            case CREDENTIAL_TYPE_PATTERN:
                screenLockResId = R.string.unlock_set_unlock_pattern_title;
                break;
            case CREDENTIAL_TYPE_PASSWORD:
                screenLockResId = R.string.unlock_set_unlock_password_title;
                break;
            case CREDENTIAL_TYPE_PIN:
            default:
                screenLockResId = R.string.unlock_set_unlock_pin_title;
                break;
        }
        return mActivity.getString(R.string.lock_screen_skip_setup_title,
                BiometricUtils.getCombinedScreenLockOptions(mActivity,
                        mActivity.getString(screenLockResId), hasFingerprint, hasFace));
    }

    @Test
    public void frpMessages_areShownCorrectly_whenNotSupported() {
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PIN, false, false, false, false, true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_intro_skip_title));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_intro_skip_dialog_text));
    }

    @Test
    public void frpMessages_areShownCorrectly_whenSupported() {
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PIN, true, false, false, false, true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_intro_skip_title));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_intro_skip_dialog_text_frp));
    }

    @Test
    public void dialogMessage_whenSkipPinSetupForFace_shouldShownCorrectly() {
        final boolean hasFace = true;
        final boolean hasFingerprint = false;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog = SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PIN, false,
                false, true, false, true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PIN, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_pin_skip_face_message));
    }

    @Test
    public void dialogMessage_whenSkipPasswordSetupForFace_shouldShownCorrectly() {
        final boolean hasFace = true;
        final boolean hasFingerprint = false;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog = SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PASSWORD,
                false, hasFingerprint, hasFace, false, true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PASSWORD, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_password_skip_face_message));
    }

    @Test
    public void dialogMessage_whenSkipPatternSetupForFace_shouldShownCorrectly() {
        final boolean hasFace = true;
        final boolean hasFingerprint = false;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PATTERN, true, false, true, false,
                        true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PATTERN, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_pattern_skip_face_message));
    }

    @Test
    public void dialogMessage_whenSkipPinSetupForFingerprint_shouldShownCorrectly() {
        final boolean hasFace = false;
        final boolean hasFingerprint = true;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PIN, true, true, false, false, true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PIN, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_pin_skip_fingerprint_message));
    }

    @Test
    public void dialogMessage_whenSkipPasswordSetupForFingerprint_shouldShownCorrectly() {
        final boolean hasFace = false;
        final boolean hasFingerprint = true;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PASSWORD, true, true, false, false,
                        true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PASSWORD, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_password_skip_fingerprint_message));
    }

    @Test
    public void dialogMessage_whenSkipPatternSetupForFingerprint_shouldShownCorrectly() {
        final boolean hasFace = false;
        final boolean hasFingerprint = true;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog = SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PATTERN, true,
                true, false, false, true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PATTERN, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_pattern_skip_fingerprint_message));
    }

    @Test
    public void dialogMessage_whenSkipPinSetupForBiometrics_shouldShownCorrectly() {
        final boolean hasFace = true;
        final boolean hasFingerprint = true;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PIN, true, false, false, true, true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PIN, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_pin_skip_biometrics_message));
    }

    @Test
    public void dialogMessage_whenSkipPasswordSetupForBiometrics_shouldShownCorrectly() {
        final boolean hasFace = true;
        final boolean hasFingerprint = true;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PASSWORD, true, false, false, true,
                        true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PASSWORD, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_password_skip_biometrics_message));
    }

    @Test
    public void dialogMessage_whenSkipPatternSetupForBiometrics_shouldShownCorrectly() {
        final boolean hasFace = true;
        final boolean hasFingerprint = true;

        when(mFaceManager.isHardwareDetected()).thenReturn(hasFace);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(hasFingerprint);

        SetupSkipDialog setupSkipDialog =
                SetupSkipDialog.newInstance(CREDENTIAL_TYPE_PATTERN, true, false, false, true,
                        true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                getSkipSetupTitle(CREDENTIAL_TYPE_PATTERN, hasFingerprint, hasFace));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                mActivity.getString(R.string.lock_screen_pattern_skip_biometrics_message));
    }
}
