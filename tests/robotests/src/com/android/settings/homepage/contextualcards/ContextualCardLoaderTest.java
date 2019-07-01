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

import static com.android.settings.homepage.contextualcards.ContextualCardLoader.DEFAULT_CARD_COUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardLoaderTest {

    private Context mContext;
    private ContextualCardLoader mContextualCardLoader;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContextualCardLoader = spy(new ContextualCardLoader(mContext));
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void loadInBackground_legacyMode_shouldReturnNothing() {
        assertThat(mContext.getResources().getBoolean(R.bool.config_use_legacy_suggestion))
                .isTrue();

        assertThat(mContextualCardLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void getDisplayableCards_twoEligibleCards_shouldShowAll() {
        final List<ContextualCard> cards = getContextualCardList().stream().limit(2)
                .collect(Collectors.toList());
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(cards.size());
    }

    @Test
    public void getDisplayableCards_fourEligibleCards_shouldShowDefaultCardCount() {
        final List<ContextualCard> fourCards = getContextualCardList();
        doReturn(fourCards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader
                .getDisplayableCards(fourCards);

        assertThat(result).hasSize(DEFAULT_CARD_COUNT);
    }

    @Test
    public void getDisplayableCards_refreshCardUri_shouldLogContextualCard() {
        mContextualCardLoader.mNotifyUri = CardContentProvider.REFRESH_CARD_URI;

        mContextualCardLoader.getDisplayableCards(new ArrayList<>());

        verify(mFakeFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_CONTEXTUAL_CARD_NOT_SHOW), any(String.class));
    }

    @Test
    public void getDisplayableCards_deleteCardUri_shouldNotLogContextualCard() {
        mContextualCardLoader.mNotifyUri = CardContentProvider.DELETE_CARD_URI;

        mContextualCardLoader.getDisplayableCards(new ArrayList<>());

        verify(mFakeFeatureFactory.metricsFeatureProvider, never()).action(any(),
                eq(SettingsEnums.ACTION_CONTEXTUAL_CARD_NOT_SHOW), any(String.class));
    }

    private List<ContextualCard> getContextualCardList() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_wifi")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_flashlight")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(
                        Uri.parse("content://com.android.settings.test.slices/action/flashlight"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_connected")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        return cards;
    }
}
