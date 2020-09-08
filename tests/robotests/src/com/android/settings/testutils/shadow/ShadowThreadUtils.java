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

package com.android.settings.testutils.shadow;

import com.android.settingslib.utils.ThreadUtils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(ThreadUtils.class)
public class ShadowThreadUtils {

    private static boolean sIsMainThread = true;
    private static final String TAG = "ShadowThreadUtils";

    @Resetter
    public static void reset() {
        sIsMainThread = true;
    }

    @Implementation
    protected static void postOnBackgroundThread(Runnable runnable) {
        runnable.run();
    }

    @Implementation
    protected static void postOnMainThread(Runnable runnable) {
        runnable.run();
    }

    @Implementation
    protected static boolean isMainThread() {
        return sIsMainThread;
    }

    public static void setIsMainThread(boolean isMainThread) {
        sIsMainThread = isMainThread;
    }
}
