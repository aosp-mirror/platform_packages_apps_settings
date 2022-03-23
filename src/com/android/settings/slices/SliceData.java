/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.slices;

import android.annotation.IntDef;
import android.net.Uri;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data class representing a slice stored by {@link SlicesIndexer}.
 * Note that {@link #mKey} is treated as a primary key for this class and determines equality.
 */
public class SliceData {
    /**
     * Flags indicating the UI type of the Slice.
     */
    @IntDef({SliceType.INTENT, SliceType.SWITCH, SliceType.SLIDER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceType {
        /**
         * Only supports content intent.
         */
        int INTENT = 0;

        /**
         * Supports toggle action.
         */
        int SWITCH = 1;

        /**
         * Supports progress bar.
         */
        int SLIDER = 2;
    }

    private final String mKey;

    private final String mTitle;

    private final String mSummary;

    private final CharSequence mScreenTitle;

    private final String mKeywords;

    private final int mIconResource;

    private final String mFragmentClassName;

    private final Uri mUri;

    private final String mPreferenceController;

    @SliceType
    private final int mSliceType;

    private final String mUnavailableSliceSubtitle;

    private final boolean mIsPublicSlice;

    public String getKey() {
        return mKey;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSummary() {
        return mSummary;
    }

    public CharSequence getScreenTitle() {
        return mScreenTitle;
    }

    public String getKeywords() {
        return mKeywords;
    }

    public int getIconResource() {
        return mIconResource;
    }

    public String getFragmentClassName() {
        return mFragmentClassName;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getPreferenceController() {
        return mPreferenceController;
    }

    public int getSliceType() {
        return mSliceType;
    }

    public String getUnavailableSliceSubtitle() {
        return mUnavailableSliceSubtitle;
    }

    public boolean isPublicSlice() {
        return mIsPublicSlice;
    }

    private SliceData(Builder builder) {
        mKey = builder.mKey;
        mTitle = builder.mTitle;
        mSummary = builder.mSummary;
        mScreenTitle = builder.mScreenTitle;
        mKeywords = builder.mKeywords;
        mIconResource = builder.mIconResource;
        mFragmentClassName = builder.mFragmentClassName;
        mUri = builder.mUri;
        mPreferenceController = builder.mPrefControllerClassName;
        mSliceType = builder.mSliceType;
        mUnavailableSliceSubtitle = builder.mUnavailableSliceSubtitle;
        mIsPublicSlice = builder.mIsPublicSlice;
    }

    @Override
    public int hashCode() {
        return mKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SliceData)) {
            return false;
        }
        SliceData newObject = (SliceData) obj;
        return TextUtils.equals(mKey, newObject.mKey);
    }

    static class Builder {
        private String mKey;

        private String mTitle;

        private String mSummary;

        private CharSequence mScreenTitle;

        private String mKeywords;

        private int mIconResource;

        private String mFragmentClassName;

        private Uri mUri;

        private String mPrefControllerClassName;

        private int mSliceType;

        private String mUnavailableSliceSubtitle;

        private boolean mIsPublicSlice;

        public Builder setKey(String key) {
            mKey = key;
            return this;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setSummary(String summary) {
            mSummary = summary;
            return this;
        }

        public Builder setScreenTitle(CharSequence screenTitle) {
            mScreenTitle = screenTitle;
            return this;
        }

        public Builder setKeywords(String keywords) {
            mKeywords = keywords;
            return this;
        }

        public Builder setIcon(int iconResource) {
            mIconResource = iconResource;
            return this;
        }

        public Builder setPreferenceControllerClassName(String controllerClassName) {
            mPrefControllerClassName = controllerClassName;
            return this;
        }

        public Builder setFragmentName(String fragmentClassName) {
            mFragmentClassName = fragmentClassName;
            return this;
        }

        public Builder setUri(Uri uri) {
            mUri = uri;
            return this;
        }

        public Builder setSliceType(@SliceType int sliceType) {
            mSliceType = sliceType;
            return this;
        }

        public Builder setUnavailableSliceSubtitle(
                String unavailableSliceSubtitle) {
            mUnavailableSliceSubtitle = unavailableSliceSubtitle;
            return this;
        }

        public Builder setIsPublicSlice(boolean isPublicSlice) {
            mIsPublicSlice = isPublicSlice;
            return this;
        }

        public SliceData build() {
            if (TextUtils.isEmpty(mKey)) {
                throw new InvalidSliceDataException("Key cannot be empty");
            }

            if (TextUtils.isEmpty(mTitle)) {
                throw new InvalidSliceDataException("Title cannot be empty");
            }

            if (TextUtils.isEmpty(mFragmentClassName)) {
                throw new InvalidSliceDataException("Fragment Name cannot be empty");
            }

            if (TextUtils.isEmpty(mPrefControllerClassName)) {
                throw new InvalidSliceDataException("Preference Controller cannot be empty");
            }

            return new SliceData(this);
        }

        public String getKey() {
            return mKey;
        }
    }

    public static class InvalidSliceDataException extends RuntimeException {

        public InvalidSliceDataException(String message) {
            super(message);
        }
    }
}