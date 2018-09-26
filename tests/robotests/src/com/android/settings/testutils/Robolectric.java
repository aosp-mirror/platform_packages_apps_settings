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

package com.android.settings.testutils;


import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

// TODO(b/111195450) - Duplicated from org.robolectric.Robolectric.
@Deprecated
public class Robolectric {

    /**
     * This method is internal and shouldn't be called by developers.
     */
    @Deprecated
    public static void reset() {
        // No-op- is now handled in the test runner. Users should not be calling this method anyway.
    }

    public static <T extends FragmentActivity> ActivityController<T> buildActivity(
            Class<T> activityClass) {
        return buildActivity(activityClass, null);
    }

    public static <T extends FragmentActivity> ActivityController<T> buildActivity(
            Class<T> activityClass, Intent intent) {
        return ActivityController.of(ReflectionHelpers.callConstructor(activityClass), intent);
    }

    public static <T extends FragmentActivity> T setupActivity(Class<T> activityClass) {
        return buildActivity(activityClass).setup().get();
    }
}

