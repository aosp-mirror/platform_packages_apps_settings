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
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards.conditional;

import android.graphics.drawable.Drawable;

/**
 * UI Model for a conditional card displayed on homepage.
 */
public interface ConditionalCard {

    /**
     * A stable ID for this card.
     *
     * @see {@link ConditionalCardController#getId()}
     */
    long getId();

    /**
     * The text display on the card for click action.
     */
    CharSequence getActionText();

    /**
     * Metrics constant used for logging user interaction.
     */
    int getMetricsConstant();

    Drawable getIcon();

    CharSequence getTitle();

    CharSequence getSummary();
}
