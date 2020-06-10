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

import android.icu.text.TimeZoneFormat;
import android.icu.text.TimeZoneNames;
import android.icu.util.TimeZone;
import android.text.TextUtils;

import com.android.settingslib.datetime.ZoneGetter;

import java.util.Date;
import java.util.Locale;

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

    public TimeZoneInfo(Builder builder) {
        mTimeZone = builder.mTimeZone;
        mId = mTimeZone.getID();
        mGenericName = builder.mGenericName;
        mStandardName = builder.mStandardName;
        mDaylightName = builder.mDaylightName;
        mExemplarLocation = builder.mExemplarLocation;
        mGmtOffset = builder.mGmtOffset;
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

    public static class Builder {
        private final TimeZone mTimeZone;
        private String mGenericName;
        private String mStandardName;
        private String mDaylightName;
        private String mExemplarLocation;
        private CharSequence mGmtOffset;

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

        public TimeZoneInfo build() {
            if (TextUtils.isEmpty(mGmtOffset)) {
                throw new IllegalStateException("gmtOffset must not be empty!");
            }
            return new TimeZoneInfo(this);
        }
    }

    public static class Formatter {
        private final Locale mLocale;
        private final Date mNow;
        private final TimeZoneFormat mTimeZoneFormat;

        public Formatter(Locale locale, Date now) {
            mLocale = locale;
            mNow = now;
            mTimeZoneFormat = TimeZoneFormat.getInstance(locale);
        }

        /**
         * @param timeZoneId Olson time zone id
         * @return TimeZoneInfo containing time zone names, exemplar locations and GMT offset
         */
        public TimeZoneInfo format(String timeZoneId) {
            TimeZone timeZone = TimeZone.getFrozenTimeZone(timeZoneId);
            return format(timeZone);
        }

        /**
         * @param timeZone Olson time zone object
         * @return TimeZoneInfo containing time zone names, exemplar locations and GMT offset
         */
        public TimeZoneInfo format(TimeZone timeZone) {
            String canonicalZoneId = getCanonicalZoneId(timeZone);
            final TimeZoneNames timeZoneNames = mTimeZoneFormat.getTimeZoneNames();
            final java.util.TimeZone javaTimeZone = java.util.TimeZone.getTimeZone(canonicalZoneId);
            final CharSequence gmtOffset = ZoneGetter.getGmtOffsetText(mTimeZoneFormat, mLocale,
                javaTimeZone, mNow);
            return new TimeZoneInfo.Builder(timeZone)
                    .setGenericName(timeZoneNames.getDisplayName(canonicalZoneId,
                            TimeZoneNames.NameType.LONG_GENERIC, mNow.getTime()))
                    .setStandardName(timeZoneNames.getDisplayName(canonicalZoneId,
                            TimeZoneNames.NameType.LONG_STANDARD, mNow.getTime()))
                    .setDaylightName(timeZoneNames.getDisplayName(canonicalZoneId,
                            TimeZoneNames.NameType.LONG_DAYLIGHT, mNow.getTime()))
                    .setExemplarLocation(timeZoneNames.getExemplarLocationName(canonicalZoneId))
                    .setGmtOffset(gmtOffset)
                    .build();
        }

        private static String getCanonicalZoneId(TimeZone timeZone) {
            final String id = timeZone.getID();
            final String canonicalId = TimeZone.getCanonicalID(id);
            if (canonicalId != null) {
                return canonicalId;
            }
            return id;
        }
    }

}
