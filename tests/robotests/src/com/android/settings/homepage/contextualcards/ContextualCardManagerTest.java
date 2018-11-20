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
import android.util.ArrayMap;

import com.android.settings.homepage.contextualcards.conditional.ConditionalContextualCard;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class ContextualCardManagerTest {

    private static final String TEST_SLICE_URI = "context://test/test";

    @Mock
    ContextualCardUpdateListener mListener;

    private Context mContext;
    private ContextualCardManager mManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final ContextualCardsFragment fragment = new ContextualCardsFragment();
        mManager = new ContextualCardManager(mContext, fragment.getSettingsLifecycle());
    }

    @Test
    public void sortCards_hasConditionalAndSliceCards_conditionalShouldAlwaysBeTheLast() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ConditionalContextualCard.Builder().build());
        cards.add(buildContextualCard(TEST_SLICE_URI));

        final List<ContextualCard> sortedCards = mManager.sortCards(cards);

        assertThat(sortedCards.get(cards.size() - 1).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL);
    }

    @Test
    public void onContextualCardUpdated_emtpyMapWithExistingCards_shouldOnlyKeepConditionalCard() {
        mManager.mContextualCards.add(new ConditionalContextualCard.Builder().build());
        mManager.mContextualCards.add(
                buildContextualCard(TEST_SLICE_URI));
        mManager.setListener(mListener);

        //Simulate database returns no contents.
        mManager.onContextualCardUpdated(new ArrayMap<>());

        assertThat(mManager.mContextualCards).hasSize(1);
        assertThat(mManager.mContextualCards.get(0).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL);
    }

    private ContextualCard buildContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_name")
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }
}
