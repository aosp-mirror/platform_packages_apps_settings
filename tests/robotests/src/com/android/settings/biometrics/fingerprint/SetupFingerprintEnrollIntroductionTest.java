/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.password.SetupChooseLockGeneric.SetupChooseLockGenericFragment;
import com.android.settings.password.SetupSkipDialog;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFingerprintManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowStorageManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;
import org.robolectric.shadows.ShadowKeyguardManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    ShadowFingerprintManager.class,
    ShadowLockPatternUtils.class,
    ShadowStorageManager.class,
    ShadowUserManager.class
})
public class SetupFingerprintEnrollIntroductionTest {

    private ActivityController<SetupFingerprintEnrollIntroduction> mController;

    @Before
    public void setUp() {
        Shadows.shadowOf(application.getPackageManager())
            .setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true);

        FakeFeatureFactory.setupForTest();

        final Intent intent = new Intent();
        mController = Robolectric.buildActivity(SetupFingerprintEnrollIntroduction.class, intent);
    }

    @After
    public void tearDown() {
        ShadowStorageManager.reset();
    }

    @Test
    public void testKeyguardNotSecure_shouldFinishWithSetupSkipDialogResultSkip() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);

        mController.create().resume();

        PartnerCustomizationLayout layout =
                mController.get().findViewById(R.id.setup_wizard_layout);
        final Button skipButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        assertThat(skipButton.getVisibility()).named("Skip visible").isEqualTo(View.VISIBLE);
        skipButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mController.get());
        assertThat(mController.get().isFinishing()).named("Is finishing").isTrue();
        assertThat(shadowActivity.getResultCode()).named("Result code")
            .isEqualTo(SetupSkipDialog.RESULT_SKIP);
    }

    @Test
    public void testKeyguardSecure_shouldFinishWithFingerprintResultSkip() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);

        mController.create().resume();

        PartnerCustomizationLayout layout =
                mController.get().findViewById(R.id.setup_wizard_layout);
        final Button skipButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        assertThat(skipButton.getVisibility()).named("Skip visible").isEqualTo(View.VISIBLE);
        skipButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mController.get());
        assertThat(mController.get().isFinishing()).named("Is finishing").isTrue();
        assertThat(shadowActivity.getResultCode()).named("Result code")
            .isEqualTo(BiometricEnrollBase.RESULT_SKIP);
    }

    @Test
    public void testBackKeyPress_shouldSetIntentDataIfLockScreenAdded() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);

        mController.create().resume();
        getShadowKeyguardManager().setIsKeyguardSecure(true);
        SetupFingerprintEnrollIntroduction activity = mController.get();
        activity.onBackPressed();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNotNull();
        assertThat(shadowActivity.getResultIntent().hasExtra(
            SetupChooseLockGenericFragment.EXTRA_PASSWORD_QUALITY)).isTrue();
    }

    @Test
    public void testBackKeyPress_shouldNotSetIntentDataIfLockScreenPresentBeforeLaunch() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);

        mController.create().resume();
        SetupFingerprintEnrollIntroduction activity = mController.get();
        activity.onBackPressed();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNull();
    }

    @Test
    public void testCancelClicked_shouldSetIntentDataIfLockScreenAdded() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);

        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        final Button skipButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        getShadowKeyguardManager().setIsKeyguardSecure(true);
        skipButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNotNull();
        assertThat(shadowActivity.getResultIntent().hasExtra(
            SetupChooseLockGenericFragment.EXTRA_PASSWORD_QUALITY)).isTrue();
    }

    @Test
    public void testCancelClicked_shouldNotSetIntentDataIfLockScreenPresentBeforeLaunch() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);

        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        final Button skipButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        skipButton.performClick();

        assertThat(Shadows.shadowOf(activity).getResultIntent()).isNull();
    }

    @Test
    public void testOnResultFromFindSensor_shouldNotSetIntentDataIfLockScreenPresentBeforeLaunch() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);
        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        activity.onActivityResult(BiometricEnrollIntroduction.BIOMETRIC_FIND_SENSOR_REQUEST,
            BiometricEnrollBase.RESULT_FINISHED, null);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNotNull();
        assertThat(shadowActivity.getResultIntent().hasExtra(
                SetupChooseLockGenericFragment.EXTRA_PASSWORD_QUALITY)).isFalse();
    }

    @Test
    public void testOnResultFromFindSensor_shouldSetIntentDataIfLockScreenAdded() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);
        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        getShadowKeyguardManager().setIsKeyguardSecure(true);
        activity.onActivityResult(BiometricEnrollIntroduction.BIOMETRIC_FIND_SENSOR_REQUEST,
            BiometricEnrollBase.RESULT_FINISHED, null);
        assertThat(Shadows.shadowOf(activity).getResultIntent()).isNotNull();
    }

    @Test
    public void testOnResultFromFindSensor_shouldNotSetIntentDataIfLockScreenNotAdded() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);
        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        activity.onActivityResult(BiometricEnrollIntroduction.BIOMETRIC_FIND_SENSOR_REQUEST,
            BiometricEnrollBase.RESULT_FINISHED, null);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNull();
    }

    @Test
    public void testLockPattern() {
        ShadowStorageManager.setIsFileEncryptedNativeOrEmulated(false);

        mController.create().postCreate(null).resume();

        SetupFingerprintEnrollIntroduction activity = mController.get();

        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();


        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        IntentForResult startedActivity = shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity).isNotNull();
        assertThat(startedActivity.intent.hasExtra(
            SetupChooseLockGenericFragment.EXTRA_PASSWORD_QUALITY)).isFalse();
    }

    private ShadowKeyguardManager getShadowKeyguardManager() {
        return Shadows.shadowOf(application.getSystemService(KeyguardManager.class));
    }
}
