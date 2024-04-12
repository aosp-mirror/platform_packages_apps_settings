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

package com.android.settings.notification.syncacrossdevices;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Updates the sync across devices feature state. It notifies the upper level whether to add/remove
 * the preference through {@link SyncAcrossDevicesFeatureCallback}
 */
public interface SyncAcrossDevicesFeatureUpdater {

    /** Forces to update the list of the Sync Across Devices feature. */
    default void forceUpdate() {}

    /** Sets the context to generate the {@link Preference}, so it could get the correct theme. */
    default void setPreferenceContext(@Nullable Context preferenceContext) {}
}
