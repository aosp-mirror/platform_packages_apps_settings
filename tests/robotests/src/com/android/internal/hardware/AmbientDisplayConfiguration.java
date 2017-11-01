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

package com.android.internal.hardware;

import android.content.Context;

/**
 * Fake controller to make robolectric test compile. Should be removed when Robolectric supports
 * API 25.
 */
public class AmbientDisplayConfiguration {

    public AmbientDisplayConfiguration(Context context) {}

    public boolean pulseOnPickupAvailable() {
        return false;
    }

    public boolean pulseOnPickupEnabled(int user) {
        return true;
    }

    public boolean pulseOnPickupCanBeModified(int user) {
        return true;
    }

    public boolean pulseOnDoubleTapAvailable() {
        return true;
    }

    public boolean pulseOnDoubleTapEnabled(int user) {
        return true;
    }

    public boolean pulseOnNotificationEnabled(int user) {
        return true;
    }

    public boolean pulseOnNotificationAvailable() {
        return true;
    }

    public boolean alwaysOnEnabled(int user) {
        return true;
    }

    public boolean alwaysOnAvailable() {
        return true;
    }

    public boolean alwaysOnAvailableForUser(int user) {
        return true;
    }

    public boolean available() {
        return true;
    }

    public boolean enabled(int user) {
        return true;
    }
}
