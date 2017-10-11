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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.ComponentName;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.os.CancellationSignal;
import android.widget.Button;

import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.password.IFingerprintManager;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowDynamicIndexableContentMonitor.class,
                ShadowEventLogWriter.class,
                ShadowUtils.class
        })
public class FingerprintEnrollFindSensorTest {

    @Mock
    private IFingerprintManager mFingerprintManager;

    private FingerprintEnrollFindSensor mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFingerprintManager(mFingerprintManager);

        RuntimeEnvironment.getAppResourceLoader().getResourceIndex();

        mActivity = Robolectric.buildActivity(
                FingerprintEnrollFindSensor.class,
                new Intent()
                        // Set the challenge token so the confirm screen will not be shown
                        .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]))
                .setup().get();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void clickNextAndFingerprint_shouldNotCrash() {
        ArgumentCaptor<EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(EnrollmentCallback.class);
        verify(mFingerprintManager).enroll(
                any(byte[].class),
                any(CancellationSignal.class),
                anyInt(),
                anyInt(),
                callbackCaptor.capture());

        Button nextButton = mActivity.findViewById(R.id.next_button);
        nextButton.performClick();
        callbackCaptor.getValue().onEnrollmentProgress(123);
        nextButton.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity).named("Next activity 1").isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(new ComponentName(application, FingerprintEnrollEnrolling.class));

        // Should only start one next activity
        assertThat(shadowActivity.getNextStartedActivityForResult()).named("Next activity 2")
                .isNull();
    }
}
