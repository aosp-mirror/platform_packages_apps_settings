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

package com.android.settings.core;

/**
 * @deprecated This interface allows a {@link android.support.v7.preference.PreferenceGroup}'s
 * controller to observe the availability of the {@link android.support.v7.preference.Preference}s
 * inside it, hiding the group when all preferences become unavailable. In the future,
 * {@link android.support.v7.preference.PreferenceGroup} will have native support for that
 * functionality, removing the need for this interface.
 */
public interface PreferenceAvailabilityObserver {

    /**
     * Notifies the observer that the availability of the preference identified by {@code key} has
     * been updated.
     */
    void onPreferenceAvailabilityUpdated(String key, boolean available);
}
