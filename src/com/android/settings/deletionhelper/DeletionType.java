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

package com.android.settings.deletionhelper;

import android.content.Context;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.Preference;

/**
 * Helper for the Deletion Helper which can query, clear out, and visualize deletable data.
 * This could represent a helper for deleting photos, downloads, movies, etc.
 */
public interface DeletionType {
    /**
     * Registers a callback to call when the amount of freeable space is updated.
     * @param listener A callback.
     */
    void registerFreeableChangedListener(FreeableChangedListener listener);

    /**
     * Resumes an operation, intended to be called when the deletion fragment resumes.
     */
    void onResume();

    /**
     * Pauses the feature's operations, intended to be called when the deletion fragment is paused.
     */
    void onPause();

    /**
     * Asynchronously free up the freeable information for the feature.
     */
    void clearFreeableData();

    /**
     * Callback interface to listen for when a deletion feature's amount of freeable space updates.
     */
    interface FreeableChangedListener {
        void onFreeableChanged(int numItems, long bytesFreeable);
    }
}
