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

package com.android.settings.wrapper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;

/**
 * This class replicates a subset of the
 * {@link android.accessibilityservice.AccessibilityServiceInfo}. The class
 * exists so that we can use a thin wrapper around it in production code and a mock in tests.
 * We cannot directly mock or shadow it, because some of the methods we rely on are newer than
 * the API version supported by Robolectric.
 */
public class AccessibilityServiceInfoWrapper {

    private final AccessibilityServiceInfo mServiceInfo;

    public AccessibilityServiceInfoWrapper(AccessibilityServiceInfo serviceInfo) {
        mServiceInfo = serviceInfo;
    }

    /**
     * Returns the real {@code AccessibilityServiceInfo} object.
     */
    public AccessibilityServiceInfo getAccessibilityServiceInfo() {
        return mServiceInfo;
    }

    public ComponentName getComponentName() {
        return mServiceInfo.getComponentName();
    }
}
