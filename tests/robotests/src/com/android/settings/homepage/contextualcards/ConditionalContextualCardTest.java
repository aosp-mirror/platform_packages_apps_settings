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

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.homepage.contextualcards.conditional.ConditionalContextualCard;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SettingsRobolectricTestRunner.class)
public class ConditionalContextualCardTest {

    @Test(expected = IllegalArgumentException.class)
    public void newInstance_changeCardType_shouldCrash() {
        new ConditionalContextualCard.Builder()
                .setCardType(ContextualCard.CardType.SUGGESTION)
                .build();
    }

    @Test
    public void getCardType_shouldAlwaysBeConditional() {
        assertThat(new ConditionalContextualCard.Builder().build().getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL);
    }
}
