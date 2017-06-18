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

package com.android.settings.fingerprint;


import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.password.SetupSkipDialog;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowKeyguardManager;
import org.robolectric.util.ActivityController;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowEventLogWriter.class,
                ShadowLockPatternUtils.class,
                ShadowUserManager.class
        })
public class SetupFingerprintEnrollIntroductionTest {

    @Mock
    private UserInfo mUserInfo;

    private ActivityController<SetupFingerprintEnrollIntroduction> mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final Intent intent = new Intent();
        mController = Robolectric.buildActivity(SetupFingerprintEnrollIntroduction.class, intent);

        ShadowUserManager.getShadow().setUserInfo(0, mUserInfo);
    }

    @Test
    public void testKeyguardNotSecure_shouldFinishWithSetupSkipDialogResultSkip() {
        getShadowKeyguardManager().setIsKeyguardSecure(false);

        mController.create().resume();

        final Button skipButton = mController.get().findViewById(R.id.fingerprint_cancel_button);
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

        final Button skipButton = mController.get().findViewById(R.id.fingerprint_cancel_button);
        assertThat(skipButton.getVisibility()).named("Skip visible").isEqualTo(View.VISIBLE);
        skipButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mController.get());
        assertThat(mController.get().isFinishing()).named("Is finishing").isTrue();
        assertThat(shadowActivity.getResultCode()).named("Result code")
                .isEqualTo(FingerprintEnrollBase.RESULT_SKIP);
    }

    private ShadowKeyguardManager getShadowKeyguardManager() {
        return Shadows.shadowOf(application.getSystemService(KeyguardManager.class));
    }
}
