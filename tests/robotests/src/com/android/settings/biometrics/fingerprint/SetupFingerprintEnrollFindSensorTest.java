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

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.content.Intent;
import android.content.res.Resources.Theme;
import android.hardware.fingerprint.FingerprintManager;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {ShadowUtils.class, ShadowAlertDialogCompat.class})
public class SetupFingerprintEnrollFindSensorTest {

    @Mock private FingerprintManager mFingerprintManager;

    private Theme mTheme;

    private SetupFingerprintEnrollFindSensor mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        FakeFeatureFactory.setupForTest();

        final Intent intent = new Intent()
                // Set the challenge token so the confirm screen will not be shown
                .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);
        mActivity = Robolectric.buildActivity(SetupFingerprintEnrollFindSensor.class,
                intent).setup().get();
        mTheme = mActivity.getTheme();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void fingerprintEnroll_showsAlert_whenClickingSkip() {
        final AlertDialog alertDialog = setupAlertDialog();
        final ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(
                alertDialog);
        final int titleRes = R.string.setup_fingerprint_enroll_skip_title;

        assertThat(application.getString(titleRes)).isEqualTo(shadowAlertDialog.getTitle());
    }

    @Test
    public void fingerprintEnroll_activityApplyDarkLightStyle() {
        mActivity.onApplyThemeResource(mTheme, R.style.GlifTheme, true /* first */);

        final String appliedThemes = mTheme.toString();
        assertThat(appliedThemes.contains("SetupWizardPartnerResource")).isTrue();
    }

    @Test
    public void fingerprintEnroll_showsAlert_setAlertDialogTheme() {
        final AlertDialog alertDialog = setupAlertDialog();

        assertThat(alertDialog.getContext().getThemeResId()).isEqualTo(
                R.style.Theme_AlertDialog);
    }

    private AlertDialog setupAlertDialog() {
        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).getSecondaryButtonView().performClick();

        final AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();

        return alertDialog;
    }
}
