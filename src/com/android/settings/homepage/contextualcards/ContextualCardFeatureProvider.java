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

package com.android.settings.homepage.contextualcards;

import android.content.Context;
import android.database.Cursor;

/** Feature provider for the contextual card feature. */
public interface ContextualCardFeatureProvider {
    /** Get contextual cards from the card provider */
    Cursor getContextualCards();

    /** Get the default contextual card to display */
    ContextualCard getDefaultContextualCard();

    /**
     * Mark a specific {@link ContextualCard} as dismissed with dismissal signal in the database
     * to indicate that the card has been dismissed.
     *
     * @param context  Context
     * @param cardName The card name of the ContextualCard which is dismissed by user.
     * @return The number of rows updated
     */
    int markCardAsDismissed(Context context, String cardName);
}
