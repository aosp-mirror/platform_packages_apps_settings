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

package com.android.settings.dashboard;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.Matchers.allOf;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PreferenceThemeTest {

    private Instrumentation mInstrumentation;
    private Context mTargetContext;
    private String mTargetPackage;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = mInstrumentation.getTargetContext();
        mTargetPackage = mTargetContext.getPackageName();
    }

    @Test
    public void startSetupWizardLockScreen_preferenceIconSpaceNotReserved() {
        launchSetupWizardLockScreen();
        // Icons should not be shown, and the frame should not occupy extra space.
        onView(allOf(withId(R.id.icon_frame), withEffectiveVisibility(Visibility.VISIBLE)))
                .check(doesNotExist());
        onView(withId(R.id.icon_container)).check(doesNotExist());
    }

    private void launchSetupWizardLockScreen() {
        final Intent settingsIntent = new Intent("com.android.settings.SETUP_LOCK_SCREEN")
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(mTargetPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InstrumentationRegistry.getInstrumentation().startActivitySync(settingsIntent);
    }
}
