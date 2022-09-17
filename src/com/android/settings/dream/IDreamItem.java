/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/**
 * Interface representing a dream item to be displayed.
 */
public interface IDreamItem {
    /**
     * Gets the title of this dream.
     */
    CharSequence getTitle();

    /**
     * Gets the summary of this dream, or null if the dream doesn't provide one.
     */
    @Nullable
    CharSequence getSummary();

    /**
     * Gets the icon for the dream.
     */
    Drawable getIcon();

    /**
     * Callback which can be implemented to handle clicks on this dream.
     */
    void onItemClicked();

    /**
     * Callback which can be implemented to handle the customization of this dream.
     */
    default void onCustomizeClicked() {
    }

    /**
     * Gets the preview image of this dream.
     */
    Drawable getPreviewImage();

    /**
     * Returns whether or not this dream is currently active.
     */
    boolean isActive();

    /**
     * Returns whether to allow customization of this dream or not.
     */
    default boolean allowCustomization() {
        return false;
    }

    /**
     * Returns whether or not this item is the no screensaver item.
     */
    default @DreamItemViewTypes.ViewType int viewType() {
        return DreamItemViewTypes.DREAM_ITEM;
    }
}
