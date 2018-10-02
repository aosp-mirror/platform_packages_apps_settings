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

import static android.provider.SettingsSlicesContract.KEY_WIFI;

import android.annotation.Nullable;

import com.android.settings.homepage.deviceinfo.DataUsageSlice;
import com.android.settings.wifi.WifiSlice;

import com.google.android.settings.intelligence.libs.contextualcards.ContextualCard;
import com.google.android.settings.intelligence.libs.contextualcards.ContextualCardProvider;

import java.util.ArrayList;
import java.util.List;

/** Provides dynamic card for SettingsIntelligence. */
public class SettingsContextualCardProvider extends ContextualCardProvider {

    public static final String CARD_AUTHORITY = "com.android.settings.homepage.contextualcards";

    @Override
    @Nullable
    public List<ContextualCard> getContextualCards() {
        final List<ContextualCard> cards = new ArrayList<>();
        final ContextualCard wifiCard =
                new ContextualCard.Builder()
                        .setSliceUri(WifiSlice.WIFI_URI.toString())
                        .setName(KEY_WIFI)
                        .build();
        final ContextualCard dataUsageCard =
                new ContextualCard.Builder()
                        .setSliceUri(DataUsageSlice.DATA_USAGE_CARD_URI.toString())
                        .setName(DataUsageSlice.PATH_DATA_USAGE_CARD)
                        .build();

        cards.add(wifiCard);
        cards.add(dataUsageCard);
        return cards;
    }
}
