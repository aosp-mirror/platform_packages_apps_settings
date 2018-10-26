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

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ContextualCardLoaderTest {

    private Context mContext;
    private ContextualCardLoader mContextualCardLoader;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mContextualCardLoader = new ContextualCardLoader(mContext);
    }

    @Test
    public void filter_twoInvalidCards_shouldReturnOneCard() {
        final String sliceUri1 = "content://com.android.settings.slices/action/flashlight"; //valid
        final String sliceUri2 = "content://com.android.settings.test.slices/action/flashlight";
        final String sliceUri3 = "cotent://com.android.settings.slices/action/flashlight";

        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(getContextualCard(sliceUri1));
        cards.add(getContextualCard(sliceUri2));
        cards.add(getContextualCard(sliceUri3));

        final List<ContextualCard> result = mContextualCardLoader.filterEligibleCards(cards);

        assertThat(result).hasSize(1);
    }

    private ContextualCard getContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_card")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }
}
