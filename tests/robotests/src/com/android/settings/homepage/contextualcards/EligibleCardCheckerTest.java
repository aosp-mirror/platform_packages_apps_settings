/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.app.slice.Slice.HINT_ERROR;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.wifi.slice.ContextualWifiSlice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class EligibleCardCheckerTest {

    private static final Uri TEST_SLICE_URI = Uri.parse("content://test/test");

    private Context mContext;
    private EligibleCardChecker mEligibleCardChecker;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mEligibleCardChecker =
                spy(new EligibleCardChecker(mContext, getContextualCard(TEST_SLICE_URI)));
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void isSliceToggleable_cardWithToggle_returnTrue() {
        final ContextualWifiSlice wifiSlice = new ContextualWifiSlice(mContext);
        final Slice slice = wifiSlice.getSlice();

        assertThat(mEligibleCardChecker.isSliceToggleable(slice)).isTrue();
    }

    @Test
    public void isCardEligibleToDisplay_toggleSlice_hasInlineActionShouldBeTrue() {
        final ContextualWifiSlice wifiSlice = new ContextualWifiSlice(mContext);
        final Slice slice = wifiSlice.getSlice();
        doReturn(slice).when(mEligibleCardChecker).bindSlice(any(Uri.class));

        mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(TEST_SLICE_URI));

        assertThat(mEligibleCardChecker.mCard.hasInlineAction()).isTrue();
    }

    @Test
    public void isCardEligibleToDisplay_invalidScheme_returnFalse() {
        final Uri invalidUri = Uri.parse("contet://com.android.settings.slices/action/flashlight");

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(invalidUri)))
                .isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_invalidRankingScore_returnFalse() {
        final ContextualCard card = new ContextualCard.Builder()
                .setName("test_card")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.FLASHLIGHT_SLICE_URI)
                .setRankingScore(-1)
                .build();

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(card))
                .isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_nullSlice_returnFalse() {
        doReturn(null).when(mEligibleCardChecker).bindSlice(any(Uri.class));

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(TEST_SLICE_URI)))
                .isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_errorSlice_returnFalse() {
        final Slice slice = new Slice.Builder(TEST_SLICE_URI)
                .addHints(HINT_ERROR).build();
        doReturn(slice).when(mEligibleCardChecker).bindSlice(any(Uri.class));

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(TEST_SLICE_URI)))
                .isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_sliceNotNull_cacheSliceToCard() {
        final ContextualWifiSlice wifiSlice = new ContextualWifiSlice(mContext);
        final Slice slice = wifiSlice.getSlice();
        doReturn(slice).when(mEligibleCardChecker).bindSlice(any(Uri.class));

        mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(TEST_SLICE_URI));

        assertThat(mEligibleCardChecker.mCard.getSlice()).isNotNull();
    }

    private ContextualCard getContextualCard(Uri sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_card")
                .setRankingScore(0.5)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(sliceUri)
                .build();
    }
}
