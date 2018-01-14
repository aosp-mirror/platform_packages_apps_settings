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
 * limitations under the License.
 */
package com.android.settings.datetime.timezone;

import android.icu.util.TimeZone;
import android.text.TextUtils;

/**
 * Data object containing information for displaying a time zone for the user to select.
 */
public class TimeZoneInfo {

    private final String mId;
    private final TimeZone mTimeZone;
    private final String mGenericName;
    private final String mStandardName;
    private final String mDaylightName;
    private final String mExemplarLocation;
    private final CharSequence mGmtOffset;
    // Arbitrary id that's unique within all TimeZoneInfo objects created by a given DataLoader instance.
    private final long mItemId;

    public TimeZoneInfo(Builder builder) {
        mTimeZone = builder.mTimeZone;
        mId = mTimeZone.getID();
        mGenericName = builder.mGenericName;
        mStandardName = builder.mStandardName;
        mDaylightName = builder.mDaylightName;
        mExemplarLocation = builder.mExemplarLocation;
        mGmtOffset = builder.mGmtOffset;
        mItemId = builder.mItemId;
    }

    public String getId() {
        return mId;
    }

    public TimeZone getTimeZone() {
        return mTimeZone;
    }

    public String getExemplarLocation() {
        return mExemplarLocation;
    }

    public String getGenericName() {
        return mGenericName;
    }

    public String getStandardName() {
        return mStandardName;
    }

    public String getDaylightName() {
        return mDaylightName;
    }

    public CharSequence getGmtOffset() {
        return mGmtOffset;
    }

    public long getItemId() {
        return mItemId;
    }

    public static class Builder {
        private final TimeZone mTimeZone;
        private String mGenericName;
        private String mStandardName;
        private String mDaylightName;
        private String mExemplarLocation;
        private CharSequence mGmtOffset;
        private long mItemId = -1;

        public Builder(TimeZone timeZone) {
            if (timeZone == null) {
                throw new IllegalArgumentException("TimeZone must not be null!");
            }
            mTimeZone = timeZone;
        }

        public Builder setGenericName(String genericName) {
            this.mGenericName = genericName;
            return this;
        }

        public Builder setStandardName(String standardName) {
            this.mStandardName = standardName;
            return this;
        }

        public Builder setDaylightName(String daylightName) {
            mDaylightName = daylightName;
            return this;
        }

        public Builder setExemplarLocation(String exemplarLocation) {
            mExemplarLocation = exemplarLocation;
            return this;
        }

        public Builder setGmtOffset(CharSequence gmtOffset) {
            mGmtOffset = gmtOffset;
            return this;
        }

        public Builder setItemId(long itemId) {
            mItemId = itemId;
            return this;
        }

        public TimeZoneInfo build() {
            if (TextUtils.isEmpty(mGmtOffset)) {
                throw new IllegalStateException("gmtOffset must not be empty!");
            }
            if (mItemId == -1) {
                throw new IllegalStateException("ItemId not set!");
            }
            return new TimeZoneInfo(this);
        }

    }
}
