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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data class representing a {@link ContextualCard}.
 */
public class ContextualCard {

    /**
     * Flags indicating the type of the ContextualCard.
     */
    @IntDef({CardType.DEFAULT, CardType.SLICE, CardType.SUGGESTION, CardType.CONDITIONAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CardType {
        int DEFAULT = 0;
        int SLICE = 1;
        int SUGGESTION = 2;
        int CONDITIONAL = 3;
    }

    private final String mName;
    @CardType
    private final int mCardType;
    private final double mRankingScore;
    private final String mSliceUri;
    private final int mCategory;
    private final String mLocalizedToLocale;
    private final String mPackageName;
    private final long mAppVersion;
    private final String mTitleResName;
    private final String mTitleText;
    private final String mSummaryResName;
    private final String mSummaryText;
    private final String mIconResName;
    private final int mIconResId;
    private final int mCardAction;
    private final long mExpireTimeMS;
    private final boolean mIsHalfWidth;
    private final Drawable mIconDrawable;

    String getName() {
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

    public String getLocalizedToLocale() {
        return mLocalizedToLocale;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public long getAppVersion() {
        return mAppVersion;
    }

    public String getTitleResName() {
        return mTitleResName;
    }

    public String getTitleText() {
        return mTitleText;
    }

    public String getSummaryResName() {
        return mSummaryResName;
    }

    public String getSummaryText() {
        return mSummaryText;
    }

    public String getIconResName() {
        return mIconResName;
    }

    public int getIconResId() {
        return mIconResId;
    }

    public int getCardAction() {
        return mCardAction;
    }

    public long getExpireTimeMS() {
        return mExpireTimeMS;
    }

    public Drawable getIconDrawable() {
        return mIconDrawable;
    }

    public boolean isHalfWidth() {
        return mIsHalfWidth;
    }

    boolean isCustomCard() {
        return TextUtils.isEmpty(mSliceUri);
    }

    public ContextualCard(Builder builder) {
        mName = builder.mName;
        mCardType = builder.mCardType;
        mRankingScore = builder.mRankingScore;
        mSliceUri = builder.mSliceUri;
        mCategory = builder.mCategory;
        mLocalizedToLocale = builder.mLocalizedToLocale;
        mPackageName = builder.mPackageName;
        mAppVersion = builder.mAppVersion;
        mTitleResName = builder.mTitleResName;
        mTitleText = builder.mTitleText;
        mSummaryResName = builder.mSummaryResName;
        mSummaryText = builder.mSummaryText;
        mIconResName = builder.mIconResName;
        mIconResId = builder.mIconResId;
        mCardAction = builder.mCardAction;
        mExpireTimeMS = builder.mExpireTimeMS;
        mIconDrawable = builder.mIconDrawable;
        mIsHalfWidth = builder.mIsHalfWidth;
    }

    ContextualCard(Cursor c) {
        mName = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.NAME));
        mCardType = c.getInt(c.getColumnIndex(CardDatabaseHelper.CardColumns.TYPE));
        mRankingScore = c.getDouble(c.getColumnIndex(CardDatabaseHelper.CardColumns.SCORE));
        mSliceUri = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.SLICE_URI));
        mCategory = c.getInt(c.getColumnIndex(CardDatabaseHelper.CardColumns.CATEGORY));
        mLocalizedToLocale = c.getString(
                c.getColumnIndex(CardDatabaseHelper.CardColumns.LOCALIZED_TO_LOCALE));
        mPackageName = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.PACKAGE_NAME));
        mAppVersion = c.getLong(c.getColumnIndex(CardDatabaseHelper.CardColumns.APP_VERSION));
        mTitleResName = c.getString(
                c.getColumnIndex(CardDatabaseHelper.CardColumns.TITLE_RES_NAME));
        mTitleText = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.TITLE_TEXT));
        mSummaryResName = c.getString(
                c.getColumnIndex(CardDatabaseHelper.CardColumns.SUMMARY_RES_NAME));
        mSummaryText = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.SUMMARY_TEXT));
        mIconResName = c.getString(c.getColumnIndex(CardDatabaseHelper.CardColumns.ICON_RES_NAME));
        mIconResId = c.getInt(c.getColumnIndex(CardDatabaseHelper.CardColumns.ICON_RES_ID));
        mCardAction = c.getInt(c.getColumnIndex(CardDatabaseHelper.CardColumns.CARD_ACTION));
        mExpireTimeMS = c.getLong(c.getColumnIndex(CardDatabaseHelper.CardColumns.EXPIRE_TIME_MS));
        mIsHalfWidth = (c.getInt(
                c.getColumnIndex(CardDatabaseHelper.CardColumns.SUPPORT_HALF_WIDTH)) == 1);
        mIconDrawable = null;
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

    public static class Builder {
        private String mName;
        private int mCardType;
        private double mRankingScore;
        private String mSliceUri;
        private int mCategory;
        private String mLocalizedToLocale;
        private String mPackageName;
        private long mAppVersion;
        private String mTitleResName;
        private String mTitleText;
        private String mSummaryResName;
        private String mSummaryText;
        private String mIconResName;
        private int mIconResId;
        private int mCardAction;
        private long mExpireTimeMS;
        private Drawable mIconDrawable;
        private boolean mIsHalfWidth;

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

        public Builder setLocalizedToLocale(String localizedToLocale) {
            mLocalizedToLocale = localizedToLocale;
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

        public Builder setTitleResName(String titleResName) {
            mTitleResName = titleResName;
            return this;
        }

        public Builder setTitleText(String titleText) {
            mTitleText = titleText;
            return this;
        }

        public Builder setSummaryResName(String summaryResName) {
            mSummaryResName = summaryResName;
            return this;
        }

        public Builder setSummaryText(String summaryText) {
            mSummaryText = summaryText;
            return this;
        }

        public Builder setIconResName(String iconResName) {
            mIconResName = iconResName;
            return this;
        }

        public Builder setIconResId(int iconResId) {
            mIconResId = iconResId;
            return this;
        }

        public Builder setCardAction(int cardAction) {
            mCardAction = cardAction;
            return this;
        }

        public Builder setExpireTimeMS(long expireTimeMS) {
            mExpireTimeMS = expireTimeMS;
            return this;
        }

        public Builder setIconDrawable(Drawable iconDrawable) {
            mIconDrawable = iconDrawable;
            return this;
        }

        public Builder setIsHalfWidth(boolean isHalfWidth) {
            mIsHalfWidth = isHalfWidth;
            return this;
        }

        public ContextualCard build() {
            return new ContextualCard(this);
        }
    }
}
