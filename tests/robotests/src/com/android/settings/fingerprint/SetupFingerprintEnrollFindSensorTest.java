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

package com.android.settings.fingerprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.IFingerprintManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowEventLogWriter.class,
                ShadowUtils.class
        })
public class SetupFingerprintEnrollFindSensorTest {

    @Mock
    private IFingerprintManager mFingerprintManager;

    private SetupFingerprintEnrollFindSensor mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        RuntimeEnvironment.getAppResourceLoader().getResourceIndex();

    }

    private void createActivity(Intent intent) {
        mActivity = Robolectric.buildActivity(
                SetupFingerprintEnrollFindSensor.class, intent)
                .setup().get();
    }

    private Intent createIntent() {
        return new Intent()
                // Set the challenge token so the confirm screen will not be shown
                .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void fingerprintEnroll_showsAlert_whenClickingSkip() {
        createActivity(createIntent());
        Button skipButton = mActivity.findViewById(R.id.skip_button);
        skipButton.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        ShadowAlertDialog shadowAlertDialog = Shadows.shadowOf(alertDialog);
        int titleRes = R.string.setup_fingerprint_enroll_skip_title;
        assertEquals(application.getString(titleRes), shadowAlertDialog.getTitle());
    }

}
