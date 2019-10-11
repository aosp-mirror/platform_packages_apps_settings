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

package com.android.settings.homepage.contextualcards.legacysuggestion;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.app.PendingIntent;
import android.service.settings.suggestions.Suggestion;

import com.android.settings.homepage.contextualcards.ContextualCard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LegacySuggestionContextualCardTest {

    @Test(expected = IllegalArgumentException.class)
    public void newInstance_changeCardType_shouldCrash() {
        new LegacySuggestionContextualCard.Builder()
                .setCardType(ContextualCard.CardType.CONDITIONAL)
                .build();
    }

    @Test
    public void getCardType_shouldAlwaysBeSuggestionType() {
        assertThat(new LegacySuggestionContextualCard.Builder().build().getCardType())
                .isEqualTo(ContextualCard.CardType.LEGACY_SUGGESTION);
    }

    @Test
    public void build_shouldSetPendingIntent() {
        assertThat(new LegacySuggestionContextualCard.Builder()
                .setPendingIntent(mock(PendingIntent.class))
                .build()
                .getPendingIntent()).isNotNull();
    }

    @Test
    public void build_shouldSetSuggestion() {
        assertThat(new LegacySuggestionContextualCard.Builder()
                .setSuggestion(mock(Suggestion.class))
                .build()
                .getSuggestion()).isNotNull();
    }
}
