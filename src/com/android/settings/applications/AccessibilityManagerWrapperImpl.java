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

package com.android.settings.applications;

import android.view.accessibility.AccessibilityManager;

/**
 * This class replicates a subset of the {@link android.view.accessibility.AccessibilityManager}.
 * The interface exists so that we can use a thin wrapper around the AccessibilityManager in
 * production code and a mock in tests.
 */
public class AccessibilityManagerWrapperImpl {

    /**
     * Determines if the accessibility button within the system navigation area is supported.
     *
     * @return {@code true} if the accessibility button is supported on this device,
     * {@code false} otherwise
     * @hide
     */
    public static boolean isAccessibilityButtonSupported() {
        return AccessibilityManager.isAccessibilityButtonSupported();
    }
}
