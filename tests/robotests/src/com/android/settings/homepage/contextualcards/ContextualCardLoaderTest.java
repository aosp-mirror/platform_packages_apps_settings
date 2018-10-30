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

import com.android.settings.homepage.contextualcards.deviceinfo.BatterySlice;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SettingsRobolectricTestRunner.class)
public class ContextualCardLoaderTest {

    private Context mContext;
    private ContextualCardLoader mContextualCardLoader;
    private SettingsSliceProvider mProvider;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContextualCardLoader = new ContextualCardLoader(mContext);
        mProvider = new SettingsSliceProvider();
        ShadowContentResolver.registerProviderInternal(SettingsSliceProvider.SLICE_AUTHORITY,
                mProvider);
    }

    @Test
    public void createStaticCards_shouldContainCorrectCards() {
        final Uri batteryInfo = BatterySlice.BATTERY_CARD_URI;
        final List<Uri> expectedUris = Arrays.asList(batteryInfo);

        final List<Uri> actualCardUris = mContextualCardLoader.createStaticCards().stream().map(
                ContextualCard::getSliceUri).collect(Collectors.toList());

        assertThat(actualCardUris).containsExactlyElementsIn(expectedUris);
    }

    @Test
    public void isCardEligibleToDisplay_customCard_returnTrue() {
        final ContextualCard customCard = new ContextualCard.Builder()
                .setName("custom_card")
                .setCardType(ContextualCard.CardType.DEFAULT)
                .setTitleText("custom_title")
                .setSummaryText("custom_summary")
                .build();

        assertThat(mContextualCardLoader.isCardEligibleToDisplay(customCard)).isTrue();
    }

    @Test
    public void isCardEligibleToDisplay_invalidScheme_returnFalse() {
        final String sliceUri = "contet://com.android.settings.slices/action/flashlight";

        assertThat(
                mContextualCardLoader.isCardEligibleToDisplay(
                        getContextualCard(sliceUri))).isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_noProvider_returnFalse() {
        final String sliceUri = "content://com.android.settings.test.slices/action/flashlight";

        assertThat(
                mContextualCardLoader.isCardEligibleToDisplay(
                        getContextualCard(sliceUri))).isFalse();
    }

    private ContextualCard getContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_card")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }
}
