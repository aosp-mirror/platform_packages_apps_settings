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

import android.annotation.IntDef;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.LayoutRes;
import androidx.slice.Slice;

import com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data class representing a {@link ContextualCard}.
 */
public class ContextualCard {

    /**
     * Flags indicating the type of the ContextualCard.
     */
    @IntDef({CardType.DEFAULT, CardType.SLICE, CardType.LEGACY_SUGGESTION, CardType.CONDITIONAL,
            CardType.CONDITIONAL_HEADER, CardType.CONDITIONAL_FOOTER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CardType {
        int DEFAULT = 0;
        int SLICE = 1;
        int LEGACY_SUGGESTION = 2;
        int CONDITIONAL = 3;
        int CONDITIONAL_HEADER = 4;
        int CONDITIONAL_FOOTER = 5;
    }

    private final Builder mBuilder;
    private final String mName;
    @CardType
    private final int mCardType;
    private final double mRankingScore;
    private final String mSliceUri;
    private final int mCategory;
    private final String mPackageName;
    private final long mAppVersion;
    private final String mTitleText;
    private final String mSummaryText;
    private final boolean mIsLargeCard;
    private final Drawable mIconDrawable;
    @LayoutRes
    private final int mViewType;
    private final boolean mIsPendingDismiss;
    private final boolean mHasInlineAction;
    private final Slice mSlice;

    public String getName() {
        return mName;
    }

    public int getCardType() {
        return mCardType;
    }

    public double getRankingScore() {
        return mRankingScore;
    }

    public String getTextSliceUri() {
        return mSliceUri;
    }

    public Uri getSliceUri() {
        return Uri.parse(mSliceUri);
    }

    public int getCategory() {
        return mCategory;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public long getAppVersion() {
        return mAppVersion;
    }

    public String getTitleText() {
        return mTitleText;
    }

    public String getSummaryText() {
        return mSummaryText;
    }

    public Drawable getIconDrawable() {
        return mIconDrawable;
    }

    public boolean isLargeCard() {
        return mIsLargeCard;
    }

    public int getViewType() {
        return mViewType;
    }

    public boolean isPendingDismiss() {
        return mIsPendingDismiss;
    }

    public boolean hasInlineAction() {
        return mHasInlineAction;
    }

    public Slice getSlice() {
        return mSlice;
    }

    public Builder mutate() {
        return mBuilder;
    }

    public ContextualCard(Builder builder) {
        mBuilder = builder;
        mName = builder.mName;
        mCardType = builder.mCardType;
        mRankingScore = builder.mRankingScore;
        mSliceUri = builder.mSliceUri;
        mCategory = builder.mCategory;
        mPackageName = builder.mPackageName;
        mAppVersion = builder.mAppVersion;
        mTitleText = builder.mTitleText;
        mSummaryText = builder.mSummaryText;
        mIconDrawable = builder.mIconDrawable;
        mIsLargeCard = builder.mIsLargeCard;
        mViewType = builder.mViewType;
        mIsPendingDismiss = builder.mIsPendingDismiss;
        mHasInlineAction = builder.mHasInlineAction;
        mSlice = builder.mSlice;
    }

    ContextualCard(Cursor c) {
        mBuilder = new Builder();
        mName = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.NAME));
        mBuilder.setName(mName);
        mCardType = c.getInt(c.getColumnIndex(CardDatabaseHelper.CardColumns.TYPE));
        mBuilder.setCardType(mCardType);
        mRankingScore = c.getDouble(c.getColumnIndex(CardDatabaseHelper.CardColumns.SCORE));
        mBuilder.setRankingScore(mRankingScore);
        mSliceUri = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.SLICE_URI));
        mBuilder.setSliceUri(Uri.parse(mSliceUri));
        mCategory = c.getInt(c.getColumnIndex(CardDatabaseHelper.CardColumns.CATEGORY));
        mBuilder.setCategory(mCategory);
        mPackageName = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.PACKAGE_NAME));
        mBuilder.setPackageName(mPackageName);
        mAppVersion = c.getLong(c.getColumnIndex(CardDatabaseHelper.CardColumns.APP_VERSION));
        mBuilder.setAppVersion(mAppVersion);
        mTitleText = "";
        mBuilder.setTitleText(mTitleText);
        mSummaryText = "";
        mBuilder.setTitleText(mSummaryText);
        mIsLargeCard = false;
        mBuilder.setIsLargeCard(mIsLargeCard);
        mIconDrawable = null;
        mBuilder.setIconDrawable(mIconDrawable);
        mViewType = getViewTypeByCardType(mCardType);
        mBuilder.setViewType(mViewType);
        mIsPendingDismiss = false;
        mBuilder.setIsPendingDismiss(mIsPendingDismiss);
        mHasInlineAction = false;
        mBuilder.setHasInlineAction(mHasInlineAction);
        mSlice = null;
        mBuilder.setSlice(mSlice);
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    /**
     * Note that {@link #mName} is treated as a primary key for this class and determines equality.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ContextualCard)) {
            return false;
        }
        final ContextualCard that = (ContextualCard) obj;

        return TextUtils.equals(mName, that.mName);
    }

    private int getViewTypeByCardType(int cardType) {
        if (cardType == CardType.SLICE) {
            return SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH;
        }
        return 0;
    }

    public static class Builder {
        private String mName;
        private int mCardType;
        private double mRankingScore;
        private String mSliceUri;
        private int mCategory;
        private String mPackageName;
        private long mAppVersion;
        private String mTitleText;
        private String mSummaryText;
        private Drawable mIconDrawable;
        private boolean mIsLargeCard;
        @LayoutRes
        private int mViewType;
        private boolean mIsPendingDismiss;
        private boolean mHasInlineAction;
        private Slice mSlice;

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setCardType(int cardType) {
            mCardType = cardType;
            return this;
        }

        public Builder setRankingScore(double rankingScore) {
            mRankingScore = rankingScore;
            return this;
        }

        public Builder setSliceUri(Uri sliceUri) {
            mSliceUri = sliceUri.toString();
            return this;
        }

        public Builder setCategory(int category) {
            mCategory = category;
            return this;
        }

        public Builder setPackageName(String packageName) {
            mPackageName = packageName;
            return this;
        }

        public Builder setAppVersion(long appVersion) {
            mAppVersion = appVersion;
            return this;
        }

        public Builder setTitleText(String titleText) {
            mTitleText = titleText;
            return this;
        }

        public Builder setSummaryText(String summaryText) {
            mSummaryText = summaryText;
            return this;
        }

        public Builder setIconDrawable(Drawable iconDrawable) {
            mIconDrawable = iconDrawable;
            return this;
        }

        public Builder setIsLargeCard(boolean isLargeCard) {
            mIsLargeCard = isLargeCard;
            return this;
        }

        public Builder setViewType(@LayoutRes int viewType) {
            mViewType = viewType;
            return this;
        }

        public Builder setIsPendingDismiss(boolean isPendingDismiss) {
            mIsPendingDismiss = isPendingDismiss;
            return this;
        }

        public Builder setHasInlineAction(boolean hasInlineAction) {
            mHasInlineAction = hasInlineAction;
            return this;
        }

        /**
         * Cache a slice created at pre-check time for later usage.
         */
        public Builder setSlice(Slice slice) {
            mSlice = slice;
            return this;
        }

        public ContextualCard build() {
            return new ContextualCard(this);
        }
    }
}
