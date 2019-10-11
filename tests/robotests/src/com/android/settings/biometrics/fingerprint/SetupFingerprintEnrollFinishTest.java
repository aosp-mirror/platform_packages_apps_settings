/*
 * Copyright (C) 2019 Google Inc.
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

import static com.android.settings.biometrics.fingerprint.FingerprintEnrollFinish.FINGERPRINT_SUGGESTION_ACTIVITY;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowFingerprintManager;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowFingerprintManager.class})
public class SetupFingerprintEnrollFinishTest {

    private FingerprintEnrollFinish mActivity;
    private ComponentName mComponentName;
    private PartnerCustomizationLayout mLayout;
    private FingerprintManager mFingerprintManager;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(FingerprintEnrollFinish.class).setup().get();
        mLayout = mActivity.findViewById(R.id.setup_wizard_layout);
        Shadows.shadowOf(application.getPackageManager())
                .setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true);

        mFingerprintManager = (FingerprintManager) application.getSystemService(
                Context.FINGERPRINT_SERVICE);
        Shadows.shadowOf(mFingerprintManager).setIsHardwareDetected(true);

        mComponentName = new ComponentName(
                application, FINGERPRINT_SUGGESTION_ACTIVITY);
    }

    @Test
    public void clickAddAnother_shouldLaunchEnrolling() {
        final ComponentName enrollingComponent = new ComponentName(
                application,
                FingerprintEnrollEnrolling.class);

        mLayout.getMixin(FooterBarMixin.class).getSecondaryButtonView().performClick();

        ShadowActivity.IntentForResult startedActivity =
                Shadows.shadowOf(mActivity).getNextStartedActivityForResult();
        assertThat(startedActivity).named("Next activity").isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(enrollingComponent);
    }

    @Test
    public void clickAddAnother_shouldPropagateResults() {
        final ComponentName enrollingComponent = new ComponentName(
                application,
                FingerprintEnrollEnrolling.class);

        mLayout.getMixin(FooterBarMixin.class).getSecondaryButtonView().performClick();

        ShadowActivity.IntentForResult startedActivity =
                Shadows.shadowOf(mActivity).getNextStartedActivityForResult();
        assertThat(startedActivity).named("Next activity").isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(enrollingComponent);
    }

    @Test
    public void clickNext_shouldFinish() {
        mLayout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_fingerprintCountIsNotOne_fingerprintSuggestionActivityDisabled() {
        Shadows.shadowOf((FingerprintManager) mFingerprintManager).setDefaultFingerprints(0);

        mActivity.onActivityResult(0, 0, null);

        assertThat(application.getPackageManager().getComponentEnabledSetting(
                mComponentName)).isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void onActivityResult_fingerprintCountIsOne_fingerprintSuggestionActivityEnabled() {
        Shadows.shadowOf((FingerprintManager) mFingerprintManager).setDefaultFingerprints(1);

        mActivity.onActivityResult(0, 0, null);

        assertThat(application.getPackageManager().getComponentEnabledSetting(
                mComponentName)).isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    @Test
    public void clickNext_fingerprintCountIsNotOne_fingerprintSuggestionActivityDisabled() {
        Shadows.shadowOf((FingerprintManager) mFingerprintManager).setDefaultFingerprints(2);

        mLayout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();

        assertThat(application.getPackageManager().getComponentEnabledSetting(
                mComponentName)).isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void clickNext_fingerprintCountIsOne_fngerprintSuggestionActivityEnabled() {
        Shadows.shadowOf((FingerprintManager) mFingerprintManager).setDefaultFingerprints(1);

        mLayout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();

        assertThat(application.getPackageManager().getComponentEnabledSetting(
                mComponentName)).isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    @Test
    public void onBackPressed_fingerprintCountIsNotOne_fingerprintSuggestionActivityDisabled() {
        Shadows.shadowOf((FingerprintManager) mFingerprintManager).setDefaultFingerprints(2);

        mActivity.onBackPressed();

        assertThat(application.getPackageManager().getComponentEnabledSetting(
                mComponentName)).isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void onBackPressed_fingerprintCountIsOne_fngerprintSuggestionActivityEnabled() {
        Shadows.shadowOf((FingerprintManager) mFingerprintManager).setDefaultFingerprints(1);

        mActivity.onBackPressed();

        assertThat(application.getPackageManager().getComponentEnabledSetting(
                mComponentName)).isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }
}