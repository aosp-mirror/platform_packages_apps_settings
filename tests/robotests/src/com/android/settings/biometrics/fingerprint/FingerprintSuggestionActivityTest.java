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
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFingerprintManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowStorageManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

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
import org.robolectric.shadows.ShadowKeyguardManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowFingerprintManager.class,
        ShadowLockPatternUtils.class,
        ShadowStorageManager.class,
        ShadowUserManager.class
})
public class FingerprintSuggestionActivityTest {
    private FingerprintSuggestionActivity mActivity;
    private ActivityController<FingerprintSuggestionActivity> mController;

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
        mController = Robolectric.buildActivity(FingerprintSuggestionActivity.class, intent);
        mActivity = mController.get();
    }

    @Test
    @Ignore
    public void testKeyguardSecure_shouldFinishWithFingerprintResultSkip() {
        getShadowKeyguardManager().setIsKeyguardSecure(true);

        mController.create().resume();

        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final Button cancelButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        assertThat(cancelButton.getText().toString()).isEqualTo("Cancel");
        assertWithMessage("Cancel visible").that(cancelButton.getVisibility())
                .isEqualTo(View.VISIBLE);
        cancelButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        assertWithMessage("Is finishing").that(mActivity.isFinishing()).isTrue();
        assertWithMessage("Result code").that(shadowActivity.getResultCode())
            .isEqualTo(Activity.RESULT_CANCELED);
    }

    private ShadowKeyguardManager getShadowKeyguardManager() {
        return Shadows.shadowOf(application.getSystemService(KeyguardManager.class));
    }
}
