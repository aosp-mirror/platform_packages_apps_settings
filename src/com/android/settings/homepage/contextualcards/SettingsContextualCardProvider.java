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

import android.annotation.Nullable;

import com.android.settings.intelligence.ContextualCardProto.ContextualCard;
import com.android.settings.intelligence.ContextualCardProto.ContextualCardList;
import com.android.settings.slices.CustomSliceRegistry;

import com.google.android.settings.intelligence.libs.contextualcards.ContextualCardProvider;

/** Provides dynamic card for SettingsIntelligence. */
public class SettingsContextualCardProvider extends ContextualCardProvider {

    public static final String CARD_AUTHORITY = "com.android.settings.homepage.contextualcards";

    @Override
    @Nullable
    public ContextualCardList getContextualCards() {
        final ContextualCard connectedDeviceCard =
                ContextualCard.newBuilder()
                        .setSliceUri(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI.toString())
                        .setCardName(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI.toString())
                        .setCardCategory(ContextualCard.Category.IMPORTANT)
                        .build();
        final ContextualCard lowStorageCard =
                ContextualCard.newBuilder()
                        .setSliceUri(CustomSliceRegistry.LOW_STORAGE_SLICE_URI.toString())
                        .setCardName(CustomSliceRegistry.LOW_STORAGE_SLICE_URI.toString())
                        .setCardCategory(ContextualCard.Category.IMPORTANT)
                        .build();
        final String contextualAdaptiveSleepSliceUri =
                CustomSliceRegistry.CONTEXTUAL_ADAPTIVE_SLEEP_URI.toString();
        final ContextualCard contextualAdaptiveSleepCard =
                ContextualCard.newBuilder()
                        .setSliceUri(contextualAdaptiveSleepSliceUri)
                        .setCardName(contextualAdaptiveSleepSliceUri)
                        .setCardCategory(ContextualCard.Category.DEFAULT)
                        .build();
        final ContextualCard contextualFaceSettingsCard =
                ContextualCard.newBuilder()
                        .setSliceUri(CustomSliceRegistry.FACE_ENROLL_SLICE_URI.toString())
                        .setCardName(CustomSliceRegistry.FACE_ENROLL_SLICE_URI.toString())
                        .setCardCategory(ContextualCard.Category.DEFAULT)
                        .build();
        final ContextualCard darkThemeCard =
                ContextualCard.newBuilder()
                        .setSliceUri(CustomSliceRegistry.DARK_THEME_SLICE_URI.toString())
                        .setCardName(CustomSliceRegistry.DARK_THEME_SLICE_URI.toString())
                        .setCardCategory(ContextualCard.Category.IMPORTANT)
                        .build();
        final ContextualCardList cards = ContextualCardList.newBuilder()
                .addCard(connectedDeviceCard)
                .addCard(lowStorageCard)
                .addCard(contextualAdaptiveSleepCard)
                .addCard(contextualFaceSettingsCard)
                .addCard(darkThemeCard)
                .build();

        return cards;
    }
}
