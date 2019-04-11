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

package com.android.settings.homepage.contextualcards.logging;

import android.util.Log;

import androidx.slice.widget.EventInfo;

import com.android.settings.homepage.contextualcards.ContextualCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils of building contextual card to string, and parse string back to {@link CardLog}
 */
public class ContextualCardLogUtils {

    private static final String TAG = "ContextualCardLogUtils";

    private static final class TapTarget {
        static int TARGET_DEFAULT = 0;
        static int TARGET_TITLE = 1;
        static int TARGET_TOGGLE = 2;
        static int TARGET_SLIDER = 3;
    }

    /**
     * Log data for a general contextual card event
     */
    public static class CardLog {
        private final String mSliceUri;
        private final double mRankingScore;

        public CardLog(Builder builder) {
            mSliceUri = builder.mSliceUri;
            mRankingScore = builder.mRankingScore;
        }

        public String getSliceUri() {
            return mSliceUri;
        }

        public double getRankingScore() {
            return mRankingScore;
        }

        public static class Builder {
            private String mSliceUri;
            private double mRankingScore;

            public Builder setSliceUri(String sliceUri) {
                mSliceUri = sliceUri;
                return this;
            }

            public Builder setRankingScore(double rankingScore) {
                mRankingScore = rankingScore;
                return this;
            }
            public CardLog build() {
                return new CardLog(this);
            }
        }
    }

    /**
     * Log data for a contextual card click event
     */
    public static class CardClickLog extends CardLog {
        private final int mSliceRow;
        private final int mSliceTapTarget;
        private final int mUiPosition;

        public CardClickLog(Builder builder) {
            super(builder);
            mSliceRow = builder.mSliceRow;
            mSliceTapTarget = builder.mSliceTapTarget;
            mUiPosition = builder.mUiPosition;
        }

        public int getSliceRow() {
            return mSliceRow;
        }

        public int getSliceTapTarget() {
            return mSliceTapTarget;
        }

        public int getUiPosition() {
            return mUiPosition;
        }

        public static class Builder extends CardLog.Builder {
            private int mSliceRow;
            private int mSliceTapTarget;
            private int mUiPosition;

            public Builder setSliceRow(int sliceRow) {
                mSliceRow = sliceRow;
                return this;
            }

            public Builder setSliceTapTarget(int sliceTapTarget) {
                mSliceTapTarget = sliceTapTarget;
                return this;
            }

            public Builder setUiPosition(int uiPosition) {
                mUiPosition = uiPosition;
                return this;
            }
            @Override
            public CardClickLog build() {
                return new CardClickLog(this);
            }
        }
    }

    /**
     * Serialize {@link ContextualCard} click event to string
     *
     * @param card Clicked Contextual card.
     * @param sliceRow A Slice can contains multiple row, which row are we clicked
     * @param tapTarget Integer value of {@link TapTarget}
     * @param uiPosition Contextual card position in Listview
     */
    public static String buildCardClickLog(ContextualCard card, int sliceRow, int tapTarget,
            int uiPosition) {
        final StringBuilder log = new StringBuilder();
        log.append(card.getTextSliceUri()).append("|")
                .append(card.getRankingScore()).append("|")
                .append(sliceRow).append("|")
                .append(actionTypeToTapTarget(tapTarget)).append("|")
                .append(uiPosition);
        return log.toString();
    }

    /**
     * Parse string to a {@link CardClickLog}
     */
    public static CardClickLog parseCardClickLog(String clickLog) {
        if (clickLog != null) {
            final String[] parts = clickLog.split("\\|");
            if (parts.length < 5) {
                return null;
            }
            try {
                final CardClickLog.Builder builder = new CardClickLog.Builder();
                builder.setSliceRow(Integer.parseInt(parts[2]))
                        .setSliceTapTarget(Integer.parseInt(parts[3]))
                        .setUiPosition(Integer.parseInt(parts[4]))
                        .setSliceUri(parts[0])
                        .setRankingScore(Double.parseDouble(parts[1]));
                return builder.build();
            } catch (Exception e) {
                Log.e(TAG, "error parsing log", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Serialize {@link ContextualCard} to string
     *
     * @param card Contextual card.
     */
    public static String buildCardDismissLog(ContextualCard card) {
        final StringBuilder log = new StringBuilder();
        log.append(card.getTextSliceUri())
                .append("|")
                .append(card.getRankingScore());
        return log.toString();
    }

    /**
     * Parse string to a {@link CardLog}
     */
    public static CardLog parseCardDismissLog(String dismissLog) {
        if (dismissLog != null) {
            final String[] parts = dismissLog.split("\\|");
            if (parts.length < 2) {
                return null;
            }
            try {
                final CardLog.Builder builder = new CardLog.Builder();
                builder.setSliceUri(parts[0])
                        .setRankingScore(Double.parseDouble(parts[1]));
                return builder.build();
            } catch (Exception e) {
                Log.e(TAG, "error parsing log", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Serialize List of {@link ContextualCard} to string
     */
    public static String buildCardListLog(List<ContextualCard> cards) {
        final StringBuilder log = new StringBuilder();
        log.append(cards.size());
        for (ContextualCard card : cards) {
            log.append("|").append(card.getTextSliceUri())
                    .append("|").append(card.getRankingScore());
        }
        return log.toString();
    }

    /**
     * Parse string to a List of {@link CardLog}
     */
    public static List<CardLog> parseCardListLog(String listLog) {
        final List<CardLog> logList = new ArrayList<>();
        try {
            final String[] parts = listLog.split("\\|");
            if (Integer.parseInt(parts[0]) < 0) {
                return logList;
            }
            final int size = parts.length;
            for (int i = 1; i < size; ) {
                final CardLog.Builder builder = new CardLog.Builder();
                builder.setSliceUri(parts[i++])
                        .setRankingScore(Double.parseDouble(parts[i++]));
                logList.add(builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing log", e);
            return logList;
        }
        return logList;
    }

    public static int actionTypeToTapTarget(int actionType) {
        switch (actionType) {
            case EventInfo.ACTION_TYPE_CONTENT:
                return TapTarget.TARGET_TITLE;
            case EventInfo.ACTION_TYPE_TOGGLE:
                return TapTarget.TARGET_TOGGLE;
            case EventInfo.ACTION_TYPE_SLIDER:
                return TapTarget.TARGET_SLIDER;
            default:
                Log.w(TAG, "unknown type " + actionType);
                return TapTarget.TARGET_DEFAULT;
        }
    }
}
