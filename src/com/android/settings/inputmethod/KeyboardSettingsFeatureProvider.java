/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

/**
 * Provider for Keyboard settings related features.
 */
public interface KeyboardSettingsFeatureProvider {

    /**
     * Checks whether the connected device supports firmware update.
     *
     * @return true if the connected device supports firmware update.
     */
    boolean supportsFirmwareUpdate();

    /**
     * Add firmware update preference category .
     *
     * @param context The context to initialize the application with.
     * @param screen  The {@link PreferenceScreen} to add the firmware update preference category.
     *
     * @return true if the category is added successfully.
     */
    boolean addFirmwareUpdateCategory(Context context, PreferenceScreen screen);

    /**
     * Get custom action key icon.
     *
     * @param context Context for accessing resources.
     *
     * @return Returns the image of the icon, or null if there is no any custom icon.
     */
    @Nullable
    Drawable getActionKeyIcon(Context context);
}
