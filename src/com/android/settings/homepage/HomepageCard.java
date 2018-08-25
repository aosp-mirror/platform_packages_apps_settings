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

package com.android.settings.homepage;

import android.annotation.IntDef;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data class representing a {@link HomepageCard}.
 */
public class HomepageCard {

    /**
     * Flags indicating the type of the HomepageCard.
     */
    @IntDef({CardType.INVALID, CardType.SLICE, CardType.SUGGESTION, CardType.CONDITIONAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CardType {
        int INVALID = -1;
        int SLICE = 1;
        int SUGGESTION = 2;
        int CONDITIONAL = 3;
    }

    private final String mName;
    @CardType
    private final int mCardType;
    private final double mScore;
    private final String mSliceUri;
    private final int mCategory;
    private final String mLocalizedToLocale;
    private final String mPackageName;
    private final String mAppVersion;
    private final String mTitleResName;
    private final String mTitleText;
    private final String mSummaryResName;
    private final String mSummaryText;
    private final String mIconResName;
    private final int mIconResId;
    private final String mCardAction;
    private final long mExpireTimeMS;
    private final Drawable mDrawable;
    private final boolean mSupportHalfWidth;

    String getName() {
        return mName;
    }

    int getCardType() {
        return mCardType;
    }

    double getScore() {
        return mScore;
    }

    String getTextSliceUri() {
        return mSliceUri;
    }

    Uri getSliceUri() {
        return Uri.parse(mSliceUri);
    }

    int getCategory() {
        return mCategory;
    }

    String getLocalizedToLocale() {
        return mLocalizedToLocale;
    }

    String getPackageName() {
        return mPackageName;
    }

    String getAppVersion() {
        return mAppVersion;
    }

    String getTitleResName() {
        return mTitleResName;
    }

    String getTitleText() {
        return mTitleText;
    }

    String getSummaryResName() {
        return mSummaryResName;
    }

    String getSummaryText() {
        return mSummaryText;
    }

    String getIconResName() {
        return mIconResName;
    }

    int getIconResId() {
        return mIconResId;
    }

    String getCardAction() {
        return mCardAction;
    }

    long getExpireTimeMS() {
        return mExpireTimeMS;
    }

    Drawable getDrawable() {
        return mDrawable;
    }

    boolean getSupportHalfWidth() {
        return mSupportHalfWidth;
    }

    HomepageCard(Builder builder) {
        mName = builder.mName;
        mCardType = builder.mCardType;
        mScore = builder.mScore;
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
        mDrawable = builder.mDrawable;
        mSupportHalfWidth = builder.mSupportHalfWidth;
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
        if (!(obj instanceof HomepageCard)) {
            return false;
        }
        final HomepageCard that = (HomepageCard) obj;

        return TextUtils.equals(mName, that.mName);
    }

    static class Builder {
        private String mName;
        private int mCardType;
        private double mScore;
        private String mSliceUri;
        private int mCategory;
        private String mLocalizedToLocale;
        private String mPackageName;
        private String mAppVersion;
        private String mTitleResName;
        private String mTitleText;
        private String mSummaryResName;
        private String mSummaryText;
        private String mIconResName;
        private int mIconResId;
        private String mCardAction;
        private long mExpireTimeMS;
        private Drawable mDrawable;
        private boolean mSupportHalfWidth;

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setCardType(int cardType) {
            mCardType = cardType;
            return this;
        }

        public Builder setScore(double score) {
            mScore = score;
            return this;
        }

        public Builder setSliceUri(String sliceUri) {
            mSliceUri = sliceUri;
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

        public Builder setAppVersion(String appVersion) {
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

        public Builder setCardAction(String cardAction) {
            mCardAction = cardAction;
            return this;
        }

        public Builder setExpireTimeMS(long expireTimeMS) {
            mExpireTimeMS = expireTimeMS;
            return this;
        }

        public Builder setDrawable(Drawable drawable) {
            mDrawable = drawable;
            return this;
        }

        public Builder setSupportHalfWidth(boolean supportHalfWidth) {
            mSupportHalfWidth = supportHalfWidth;
            return this;
        }

        public HomepageCard build() {
            return new HomepageCard(this);
        }
    }
}
