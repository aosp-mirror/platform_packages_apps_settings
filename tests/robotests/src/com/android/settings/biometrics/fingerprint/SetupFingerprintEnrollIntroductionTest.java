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
import static com.google.common.truth.Truth.assertWithMessage;

import static org.robolectric.RuntimeEnvironment.application;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.password.SetupSkipDialog;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowFingerprintManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowStorageManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    ShadowFingerprintManager.class,
    ShadowLockPatternUtils.class,
    ShadowStorageManager.class,
    ShadowUserManager.class,
    ShadowAlertDialogCompat.class
})
public class SetupFingerprintEnrollIntroductionTest {

    private ActivityController<SetupFingerprintEnrollIntroduction> mController;

    public static class TestSetupFingerprintEnrollIntroductionInMultiWindowMode
            extends SetupFingerprintEnrollIntroduction {
        public boolean mIsMultiWindowMode = true;

        @Override
        public boolean isInMultiWindowMode() {
            return mIsMultiWindowMode;
        }
    }

    @Before
    public void setUp() {
        Shadows.shadowOf(application.getPackageManager())
            .setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true);

        final List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        componentInfo.add(new ComponentInfoInternal("faceSensor" /* componentId */,
                "vendor/model/revision" /* hardwareVersion */, "1.01" /* firmwareVersion */,
                "00000001" /* serialNumber */, "" /* softwareVersion */));
        componentInfo.add(new ComponentInfoInternal("matchingAlgorithm" /* componentId */,
                "" /* hardwareVersion */, "" /* firmwareVersion */, "" /* serialNumber */,
                "vendor/version/revision" /* softwareVersion */));

        final FingerprintSensorPropertiesInternal prop = new FingerprintSensorPropertiesInternal(
                0 /* sensorId */,
                SensorProperties.STRENGTH_STRONG,
                5 /* maxEnrollmentsPerUser */,
                componentInfo,
                FingerprintSensorProperties.TYPE_REAR,
                true /* resetLockoutRequiresHardwareAuthToken */);
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(prop);
        ShadowFingerprintManager.setSensorProperties(props);

        FakeFeatureFactory.setupForTest();

        final Intent intent = new Intent();
        mController = Robolectric.buildActivity(SetupFingerprintEnrollIntroduction.class, intent);
    }

    @After
    public void tearDown() {
        ShadowStorageManager.reset();
        ShadowAlertDialogCompat.reset();
    }

    @Test
    public void multiWindow_showsDialog() {
        Activity activity = Robolectric.buildActivity(
                TestSetupFingerprintEnrollIntroductionInMultiWindowMode.class).setup().get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();

        final ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                activity.getString(
                        R.string.biometric_settings_add_fingerprint_in_split_mode_title));
        assertThat(shadowAlertDialog.getMessage().toString()).isEqualTo(
                activity.getString(
                        R.string.biometric_settings_add_fingerprint_in_split_mode_message));

        // TODO(b/299573056): Make WizardManagerHelper.isAnySetupWizard(getIntent()) correct and
        //  test button click not finishing the activity.
    }

    @Test
    public void singleWindow_noDialog() {
        Robolectric.buildActivity(SetupFingerprintEnrollIntroduction.class).setup().get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    @Ignore
    public void testKeyguardNotSecure_shouldFinishWithSetupSkipDialogResultSkip() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);

        mController.create().resume();

        PartnerCustomizationLayout layout =
                mController.get().findViewById(R.id.setup_wizard_layout);
        final Button skipButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        assertWithMessage("Skip visible").that(skipButton.getVisibility()).isEqualTo(View.VISIBLE);
        skipButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mController.get());
        assertWithMessage("Is finishing").that(mController.get().isFinishing()).isTrue();
        assertWithMessage("Result code").that(shadowActivity.getResultCode())
            .isEqualTo(SetupSkipDialog.RESULT_SKIP);
    }

    @Test
    @Ignore
    public void testKeyguardSecure_shouldFinishWithFingerprintResultSkip() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);

        mController.create().resume();

        PartnerCustomizationLayout layout =
                mController.get().findViewById(R.id.setup_wizard_layout);
        final Button skipButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        assertWithMessage("Skip visible").that(skipButton.getVisibility()).isEqualTo(View.VISIBLE);
        skipButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mController.get());
        assertWithMessage("Is finishing").that(mController.get().isFinishing()).isTrue();
        assertWithMessage("Result code").that(shadowActivity.getResultCode())
            .isEqualTo(BiometricEnrollBase.RESULT_SKIP);
    }

    @Test
    @Ignore
    public void testBackKeyPress_shouldSetIntentDataIfLockScreenAdded() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);

        mController.create().resume();
        getShadowKeyguardManager().setIsKeyguardSecure(true);
        SetupFingerprintEnrollIntroduction activity = mController.get();
        activity.onBackPressed();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNotNull();
    }

    @Test
    @Ignore
    public void testBackKeyPress_shouldNotSetIntentDataIfLockScreenPresentBeforeLaunch() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);

        mController.create().resume();
        SetupFingerprintEnrollIntroduction activity = mController.get();
        activity.onBackPressed();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNull();
    }

    @Test
    @Ignore
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
    }

    @Test
    @Ignore
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
    @Ignore
    public void testOnResultFromFindSensor_shouldNotSetIntentDataIfLockScreenPresentBeforeLaunch() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);
        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        activity.onActivityResult(BiometricEnrollIntroduction.BIOMETRIC_FIND_SENSOR_REQUEST,
            BiometricEnrollBase.RESULT_FINISHED, null);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNotNull();
    }

    @Test
    @Ignore
    public void testOnResultFromFindSensor_shouldSetIntentDataIfLockScreenAdded() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);
        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        getShadowKeyguardManager().setIsKeyguardSecure(true);
        activity.onActivityResult(BiometricEnrollIntroduction.BIOMETRIC_FIND_SENSOR_REQUEST,
            BiometricEnrollBase.RESULT_FINISHED, null);
        assertThat(Shadows.shadowOf(activity).getResultIntent()).isNotNull();
    }

    @Test
    @Ignore
    public void testOnResultFromFindSensor_shouldNotSetIntentDataIfLockScreenNotAdded() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);
        SetupFingerprintEnrollIntroduction activity = mController.create().resume().get();
        activity.onActivityResult(BiometricEnrollIntroduction.BIOMETRIC_FIND_SENSOR_REQUEST,
            BiometricEnrollBase.RESULT_FINISHED, null);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getResultIntent()).isNull();
    }

    @Test
    @Ignore
    public void testLockPattern() {
        ShadowStorageManager.setIsFileEncrypted(false);

        mController.create().postCreate(null).resume();

        SetupFingerprintEnrollIntroduction activity = mController.get();

        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();


        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        IntentForResult startedActivity = shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity).isNotNull();
    }

    private ShadowKeyguardManager getShadowKeyguardManager() {
        return Shadows.shadowOf(application.getSystemService(KeyguardManager.class));
    }
}
