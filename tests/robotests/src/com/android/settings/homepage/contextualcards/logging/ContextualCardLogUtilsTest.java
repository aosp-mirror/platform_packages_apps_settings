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

package com.android.settings.homepage.contextualcards.logging;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import com.android.settings.homepage.contextualcards.ContextualCard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardLogUtilsTest {

    private static final String TEST_URI = "content://test/test2";
    private static final double TEST_SCORE = 0.12345f;

    private static final ContextualCard TEST_CARD =
            new ContextualCard.Builder()
                    .setSliceUri(Uri.parse(TEST_URI))
                    .setRankingScore(TEST_SCORE)
                    .build();

    @Test
    public void parseCardDismissLog_notValid_returnNull() {
        assertThat(ContextualCardLogUtils.parseCardDismissLog(TEST_URI + "|" + TEST_URI)).isNull();
    }

    @Test
    public void parseCardDismissLog_isValid_returnCorrectData() {
        final String log = ContextualCardLogUtils.buildCardDismissLog(TEST_CARD);

        final ContextualCardLogUtils.CardLog cardLog = ContextualCardLogUtils.parseCardDismissLog(
                log);

        assertThat(cardLog.getSliceUri()).isEqualTo(TEST_URI);
        assertThat(cardLog.getRankingScore()).isEqualTo(TEST_SCORE);
    }

    @Test
    public void parseCardClickLog_isValid_returnCorrectData() {
        final int row = 1;
        final int target = 2;
        final int position = 3;
        final String log = ContextualCardLogUtils.buildCardClickLog(TEST_CARD, row, target,
                position);

        final ContextualCardLogUtils.CardClickLog cardClickLog =
                ContextualCardLogUtils.parseCardClickLog(log);

        assertThat(cardClickLog.getSliceUri()).isEqualTo(TEST_URI);
        assertThat(cardClickLog.getRankingScore()).isEqualTo(TEST_SCORE);
        assertThat(cardClickLog.getSliceRow()).isEqualTo(row);
        assertThat(cardClickLog.getSliceTapTarget()).isEqualTo(
                ContextualCardLogUtils.actionTypeToTapTarget(target));
        assertThat(cardClickLog.getUiPosition()).isEqualTo(position);
    }

    @Test
    public void parseCardClickList_isValid_returnCorrectData() {
        final ContextualCard testcard =
                new ContextualCard.Builder()
                        .setSliceUri(Uri.parse("testtest"))
                        .setRankingScore(-1d)
                        .build();
        final List<ContextualCard> cardList = new ArrayList<>();
        cardList.add(TEST_CARD);
        cardList.add(testcard);
        final String log = ContextualCardLogUtils.buildCardListLog(cardList);

        final List<ContextualCardLogUtils.CardLog> cardClickLogList =
                ContextualCardLogUtils.parseCardListLog(log);

        assertThat(cardClickLogList.size()).isEqualTo(2);
        assertThat(cardClickLogList.get(0).getSliceUri()).isEqualTo(TEST_URI);
        assertThat(cardClickLogList.get(0).getRankingScore()).isEqualTo(TEST_SCORE);
        assertThat(cardClickLogList.get(1).getSliceUri()).isEqualTo("testtest");
        assertThat(cardClickLogList.get(1).getRankingScore()).isEqualTo(-1d);
    }
}