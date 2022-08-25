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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class EligibleCardCheckerTest {

    private static final Uri TEST_SLICE_URI = Uri.parse("content://test/test");

    private Context mContext;
    private EligibleCardChecker mEligibleCardChecker;
    private Activity mActivity;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mEligibleCardChecker =
                spy(new EligibleCardChecker(mContext, getContextualCard(TEST_SLICE_URI)));
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void isSliceToggleable_cardWithToggle_returnTrue() {
        final Slice slice = buildSlice();

        assertThat(mEligibleCardChecker.isSliceToggleable(slice)).isTrue();
    }

    @Test
    public void isCardEligibleToDisplay_toggleSlice_hasInlineActionShouldBeTrue() {
        final Slice slice = buildSlice();
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
        final Slice slice = buildSlice();
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

    private Slice buildSlice() {
        final String title = "test_title";
        final IconCompat icon = IconCompat.createWithResource(mActivity, R.drawable.empty_icon);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                mActivity,
                title.hashCode() /* requestCode */,
                new Intent("test action"),
                PendingIntent.FLAG_IMMUTABLE);
        final SliceAction action
                = SliceAction.createDeeplink(pendingIntent, icon, ListBuilder.SMALL_IMAGE, title);
        return new ListBuilder(mActivity, TEST_SLICE_URI, ListBuilder.INFINITY)
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(icon, ListBuilder.ICON_IMAGE)
                        .setTitle(title)
                        .setPrimaryAction(action))
                .addAction(SliceAction.createToggle(pendingIntent, null /* actionTitle */, true))
                .build();
    }
}
