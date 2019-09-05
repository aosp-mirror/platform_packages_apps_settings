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

package com.android.settings;

import static junit.framework.Assert.fail;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RegulatoryInfoDisplayActivityTest {
    private static final String TAG = "RegulatoryInfoTest";

    private Instrumentation mInstrumentation;
    private Intent mRegulatoryInfoIntent;
    private UiAutomation mUiAutomation;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mUiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mRegulatoryInfoIntent = new Intent("android.settings.SHOW_REGULATORY_INFO")
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(mInstrumentation.getTargetContext().getPackageName());
    }

    @Test
    public void resolveRegulatoryInfoIntent_intentShouldMatchConfig() {
        // Load intent from PackageManager and load config from Settings app
        final Context context = mInstrumentation.getTargetContext();

        final boolean hasRegulatoryInfo = context.getResources()
                .getBoolean(R.bool.config_show_regulatory_info);
        final ResolveInfo resolveInfo = mInstrumentation.getTargetContext().getPackageManager()
                .resolveActivity(mRegulatoryInfoIntent, 0 /* flags */);

        // Check config and intent both enable or both disabled.
        if (hasRegulatoryInfo && resolveInfo == null) {
            fail("Config enables regulatory info but there is no handling intent");
            return;
        }
        if (!hasRegulatoryInfo && resolveInfo != null) {
            fail("Config disables regulatory info but there is at least one handling intent");
            return;
        }
    }
}
