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
 * limitations under the License
 */
package com.android.settings.connecteddevice.dock;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Update the dock devices. It notifies the upper level whether to add/remove the preference
 * through {@link DevicePreferenceCallback}
 */
public interface DockUpdater {

    /**
     * Register the dock event callback and update the list
     */
    default void registerCallback() {
    }

    /**
     * Unregister the dock event callback
     */
    default void unregisterCallback() {
    }

    /**
     * Force to update the list of dock devices
     */
    default void forceUpdate() {
    }

    /**
     * Set the context to generate the {@link Preference}, so it could get the correct theme.
     */
    default void setPreferenceContext(@NonNull Context preferenceContext) {
    }
}
