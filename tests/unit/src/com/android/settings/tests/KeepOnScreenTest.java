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

package com.android.settings.tests;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import com.android.settings.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class KeepOnScreenTest {
    private static int EXPECTED_FLAG = BatteryManager.BATTERY_PLUGGED_AC
            | BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_WIRELESS;

    @Test
    public void testStayAwake_turnOn_StayAwakeWhileWirelessCharging() throws Exception{
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(new Intent(android.provider.Settings
                .ACTION_APPLICATION_DEVELOPMENT_SETTINGS));

        final Context targetContext = instrumentation.getTargetContext();
        final int prevFlag = Settings.Global.getInt(targetContext.getContentResolver(), Settings
                .Global.STAY_ON_WHILE_PLUGGED_IN);

        // Turn on "Stay Awake" if needed
        if (prevFlag == 0) {
            onView(withText(R.string.keep_screen_on)).perform(click());
        }

        final int currentFlag = Settings.Global.getInt(targetContext.getContentResolver(), Settings
                .Global.STAY_ON_WHILE_PLUGGED_IN);

        assertEquals(EXPECTED_FLAG, currentFlag);

        // Since this app doesn't have permission(and shouldn't have) to change global setting, we
        // can only tearDown in this way
        if (prevFlag != currentFlag) {
            onView(withText(R.string.keep_screen_on)).perform(click());
        }
    }
}
