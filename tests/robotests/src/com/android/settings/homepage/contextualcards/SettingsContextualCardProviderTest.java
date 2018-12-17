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
 *
 */

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.slice.SliceManager;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.intelligence.ContextualCardProto.ContextualCard;
import com.android.settings.intelligence.ContextualCardProto.ContextualCardList;
import com.android.settings.slices.CustomSliceRegistry;

import com.google.android.settings.intelligence.libs.contextualcards.ContextualCardProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SettingsContextualCardProviderTest {

    @Mock
    private SliceManager mSliceManager;
    private SettingsContextualCardProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mProvider = spy(Robolectric.setupContentProvider(SettingsContextualCardProvider.class));
        final Context context = spy(RuntimeEnvironment.application);
        doReturn(mSliceManager).when(context).getSystemService(SliceManager.class);
        doReturn(context).when(mProvider).getContext();
    }

    @Test
    public void contentProviderCall_returnCorrectSize() throws Exception {
        final int actualNo = mProvider.getContextualCards().getCardCount();

        final Bundle returnValue =
                mProvider.call(ContextualCardProvider.METHOD_GET_CARD_LIST, "", null);
        final ContextualCardList cards =
                ContextualCardList.parseFrom(
                        returnValue.getByteArray(ContextualCardProvider.BUNDLE_CARD_LIST));
        assertThat(cards.getCardCount()).isEqualTo(actualNo);
    }

    @Test
    public void getContextualCards_wifiSlice_shouldGetImportantCategory() {
        final ContextualCardList cards = mProvider.getContextualCards();
        ContextualCard wifiCard = null;
        for (ContextualCard card : cards.getCardList()) {
            if (card.getSliceUri().equals(
                    CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI.toString())) {
                wifiCard = card;
            }
        }

        assertThat(wifiCard.getCardCategory()).isEqualTo(ContextualCard.Category.IMPORTANT);
    }
}