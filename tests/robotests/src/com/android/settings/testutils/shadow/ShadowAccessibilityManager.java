/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.UserIdInt;
import android.content.ComponentName;
import android.view.accessibility.AccessibilityManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Collections;
import java.util.Map;

/**
 * Shadow of {@link AccessibilityManager} with the hidden methods
 */
@Implements(AccessibilityManager.class)
public class ShadowAccessibilityManager extends org.robolectric.shadows.ShadowAccessibilityManager {
    /**
     * Implements a hidden method {@link AccessibilityManager.getA11yFeatureToTileMap} and returns
     * an empty map.
     */
    @Implementation
    public Map<ComponentName, ComponentName> getA11yFeatureToTileMap(@UserIdInt int userId) {
        return Collections.emptyMap();
    }
}
