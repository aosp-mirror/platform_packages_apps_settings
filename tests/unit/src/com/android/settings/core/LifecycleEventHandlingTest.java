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

package com.android.settings.core;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.Presubmit;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.development.featureflags.FeatureFlagsDashboard;
import com.android.settingslib.core.instrumentation.Instrumentable;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LifecycleEventHandlingTest {

    private static final long TIMEOUT = 2000;

    private Context mContext;
    private String mTargetPackage;
    private UiDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.wakeUp();
        mDevice.executeShellCommand("wm dismiss-keyguard");
        mContext = InstrumentationRegistry.getTargetContext();
        mTargetPackage = mContext.getPackageName();
    }

    @Test
    @Presubmit
    @Ignore("b/133334887")
    public void launchDashboard_shouldSeeFooter() {
        new SubSettingLauncher(mContext)
                .setDestination(FeatureFlagsDashboard.class.getName())
                .setSourceMetricsCategory(Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
                .launch();

        final String footerText = "Experimental";
        // Scroll to bottom
        final UiObject2 view = mDevice.wait(
                Until.findObject(By.res(mTargetPackage, "main_content")),
                TIMEOUT);
        view.scroll(Direction.DOWN, 100f);

        assertThat(mDevice.wait(Until.findObject(By.text(footerText)), TIMEOUT))
                .isNotNull();
    }
}
