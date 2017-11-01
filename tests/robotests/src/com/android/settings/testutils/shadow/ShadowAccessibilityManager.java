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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.view.accessibility.AccessibilityManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@Implements(AccessibilityManager.class)
public class ShadowAccessibilityManager {
    private static final List<AccessibilityServiceInfo> mInstalledAccessibilityList =
            new ArrayList<>();

    public static void addAccessibilityService(String serviceName) {
        AccessibilityServiceInfo serviceInfo = new AccessibilityServiceInfo();
        serviceInfo.setComponentName(ComponentName.unflattenFromString(serviceName));
        mInstalledAccessibilityList.add(serviceInfo);
    }

    @Implementation
    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList() {
        return mInstalledAccessibilityList;
    }
}
