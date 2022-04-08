/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.InstrumentationRegistry.getTargetContext;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiSelector;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

/**
 * Tests for {@link SetupChooseLockGenericTest}
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SetupChooseLockGenericTest {

    private UiDevice mDevice;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(getInstrumentation());
        mContext = getInstrumentation().getTargetContext();
        Settings.Global.putInt(
            mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(
            mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
    }

    @Test
    public void clickSkipFigerprintPreference_deviceNotProvisioned_shouldBeAbleToProceed()
            throws Throwable {
        final Intent newPasswordIntent =
            new Intent(getTargetContext(), SetupChooseLockGeneric.class)
            .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true)
            .setAction(ACTION_SET_NEW_PASSWORD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        getInstrumentation().getContext().startActivity(newPasswordIntent);
        mDevice.waitForIdle();
        mDevice.findObject(new UiSelector().textContains("Continue without ")).click();

        final Activity activity = getCurrentActivity();
        assertThat(activity).isInstanceOf(SetupChooseLockGeneric.InternalActivity.class);
    }

    private Activity getCurrentActivity() throws Throwable {
        getInstrumentation().waitForIdleSync();
        final Activity[] activity = new Activity[1];
        getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED);
            activity[0] = activities.iterator().next();
        });
        return activity[0];
    }

}
