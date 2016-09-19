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
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import org.junit.runner.RunWith;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import com.android.settings.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DrawOverlayDetailsTest {
    private final static String PACKAGE_SYSTEM_UI = "com.android.systemui";

    @Test
    public void testSystemUiDrawOverlayDetails_Disabled() throws Exception{
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(new Intent(android.provider.Settings
                .ACTION_MANAGE_OVERLAY_PERMISSION));

        final Context targetContext = instrumentation.getTargetContext();

        final PackageManager packageManager = targetContext.getPackageManager();
        final String appName = (String) packageManager.getApplicationLabel(packageManager
                .getApplicationInfo(PACKAGE_SYSTEM_UI, PackageManager.GET_META_DATA));

        openActionBarOverflowOrOptionsMenu(targetContext);
        onView(withText(targetContext.getString(R.string.menu_show_system))).perform(click());
        onView(withText(appName)).perform(click());
        onView(withText(targetContext.getString(R.string.permit_draw_overlay))).check(matches
                (not(isEnabled())));
    }

}
