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

package com.android.settings.ui.inputmethods;

import static com.android.settings.ui.testutils.SettingsTestUtils.TIMEOUT;
import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SpellCheckerSettingsUITest {

    private Instrumentation mInstrumentation;
    private Intent mIntent;
    private UiDevice mUiDevice;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mUiDevice = UiDevice.getInstance(mInstrumentation);
        mIntent = new Intent().setClassName("com.android.settings",
                "com.android.settings.Settings$SpellCheckersSettingsActivity")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void launchSettings_hasSwitchBar() {
        mInstrumentation.getContext().startActivity(mIntent);
        final UiObject2 switchBar =
                mUiDevice.wait(Until.findObject(By.text("Use spell checker")), TIMEOUT);

        assertThat(switchBar).isNotNull();
    }
}
