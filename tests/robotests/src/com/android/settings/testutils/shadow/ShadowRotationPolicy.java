/*
 *  Copyright (C) 2018 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.android.settings.testutils.shadow;

import android.content.Context;

import com.android.internal.view.RotationPolicy;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(RotationPolicy.class)
public class ShadowRotationPolicy {

    private static boolean rotationLockEnabled = false;
    private static boolean rotationSupported = true;

    @Implementation
    protected static void setRotationLock(Context context, final boolean enabled, String caller) {
        rotationLockEnabled = enabled;
    }

    @Implementation
    protected static void setRotationLockForAccessibility(
            Context context, final boolean enabled, String caller) {
        rotationLockEnabled = enabled;
    }

    @Implementation
    protected static boolean isRotationLocked(Context context) {
        return rotationLockEnabled;
    }

    @Implementation
    protected static boolean isRotationSupported(Context context) {
        return rotationSupported;
    }

    public static void setRotationSupported(boolean supported) {
        rotationSupported = supported;
    }
}
