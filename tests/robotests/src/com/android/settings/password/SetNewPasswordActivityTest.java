/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SetNewPasswordActivityTest {

    private int mProvisioned;

    @Before
    public void setUp() {
        mProvisioned = Settings.Global.getInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, mProvisioned);
    }

    @Test
    public void testChooseLockGeneric() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class).get();
        activity.launchChooseLock(new Bundle());
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent intent = shadowActivity.getNextStartedActivityForResult().intent;

        assertThat(intent.getComponent())
                .isEqualTo(new ComponentName(activity, ChooseLockGeneric.class));
    }

    @Test
    public void testSetupChooseLockGeneric() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class).get();
        activity.launchChooseLock(new Bundle());
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent intent = shadowActivity.getNextStartedActivityForResult().intent;

        assertThat(intent.getComponent())
                .isEqualTo(new ComponentName(activity, SetupChooseLockGeneric.class));

    }
}
