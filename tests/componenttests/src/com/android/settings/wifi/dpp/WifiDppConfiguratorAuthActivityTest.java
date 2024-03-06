/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import static android.app.Activity.RESULT_OK;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.equalTo;

import android.app.KeyguardManager;

import androidx.activity.result.ActivityResult;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WifiDppConfiguratorAuthActivityTest {

    @Before
    public void setup() {
        Intents.init();
    }

    @After
    public void teardown() throws Exception {
        Intents.release();
    }

    @Test
    public void launchActivity_sendAuthIntent() {
        ActivityScenario<WifiDppConfiguratorAuthActivity> activityScenario =
                ActivityScenario.launch(WifiDppConfiguratorAuthActivity.class);
        assertThat(activityScenario).isNotNull();
        intended(hasAction(equalTo(KeyguardManager.ACTION_CONFIRM_DEVICE_CREDENTIAL_WITH_USER)));
    }

    @Test
    public void launchActivity_sendQrCodeIntent() {
        ActivityScenario.launch(WifiDppConfiguratorAuthActivity.class).onActivity(activity ->
                activity.onAuthResult(new ActivityResult(RESULT_OK, /* data= */ null))
        );
        intended(hasAction(
                equalTo(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR)));
    }

    @Test
    public void launchActivity_shouldFinish() {
        ActivityScenario.launch(WifiDppConfiguratorAuthActivity.class).onActivity(activity -> {
            activity.onAuthResult(new ActivityResult(RESULT_OK, /* data= */ null));
            assertThat(activity.isFinishing()).isTrue();
        });
    }
}
