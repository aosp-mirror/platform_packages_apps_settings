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

package com.android.settings.display;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings.NightDisplaySettingsActivity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NightDisplaySettingsActivityTest {

    private Context mTargetContext;

    @Before
    public void setUp() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = instrumentation.getTargetContext();
    }

    @Test
    public void nightDisplaySettingsIntent_resolvesCorrectly() {
        final boolean nightDisplayAvailable = mTargetContext.getResources().getBoolean(
                com.android.internal.R.bool.config_nightDisplayAvailable);
        final PackageManager pm = mTargetContext.getPackageManager();
        final Intent intent = new Intent(Settings.ACTION_NIGHT_DISPLAY_SETTINGS);
        final ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (nightDisplayAvailable) {
            Assert.assertNotNull("No activity for " + Settings.ACTION_NIGHT_DISPLAY_SETTINGS, ri);
            Assert.assertEquals(mTargetContext.getPackageName(), ri.activityInfo.packageName);
            Assert.assertEquals(NightDisplaySettingsActivity.class.getName(),
                    ri.activityInfo.name);
        } else {
            Assert.assertNull("Should have no activity for "
                    + Settings.ACTION_NIGHT_DISPLAY_SETTINGS, ri);
        }
    }

}
