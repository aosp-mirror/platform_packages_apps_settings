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
 *
 */

package com.android.settings.search.indexing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.android.settings.SettingsActivity;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.ResultPayload;
import com.android.settings.search.ResultPayloadUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Data class representing a single row in the Setting Search results database.
 */
public class IndexData {
    public final String locale;
    public final String updatedTitle;
    public final String normalizedTitle;
    public final String updatedSummaryOn;
    public final String normalizedSummaryOn;
    public final String entries;
    public final String className;
    public final String childClassName;
    public final String screenTitle;
    public final int iconResId;
    public final String spaceDelimitedKeywords;
    public final String intentAction;
    public final String intentTargetPackage;
    public final String intentTargetClass;
    public final boolean enabled;
    public final String key;
    public final int userId;
    public final int payloadType;
    public final byte[] payload;

    private static final String NON_BREAKING_HYPHEN = "\u2011";
    private static final String EMPTY = "";
    private static final String HYPHEN = "-";
    private static final String SPACE = " ";
    // Regex matching a comma, and any number of subsequent white spaces.
    private static final String LIST_DELIMITERS = "[,]\\s*";

    private static final Pattern REMOVE_DIACRITICALS_PATTERN
            = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private IndexData(Builder builder) {
        locale = Locale.getDefault().toString();
        updatedTitle = normalizeHyphen(builder.mTitle);
        updatedSummaryOn = normalizeHyphen(builder.mSummaryOn);
        if (Locale.JAPAN.toString().equalsIgnoreCase(locale)) {
            // Special case for JP. Convert charset to the same type for indexing purpose.
            normalizedTitle = normalizeJapaneseString(builder.mTitle);
            normalizedSummaryOn = normalizeJapaneseString(builder.mSummaryOn);
        } else {
            normalizedTitle = normalizeString(builder.mTitle);
            normalizedSummaryOn = normalizeString(builder.mSummaryOn);
        }
        entries = builder.mEntries;
        className = builder.mClassName;
        childClassName = builder.mChildClassName;
        screenTitle = builder.mScreenTitle;
        iconResId = builder.mIconResId;
        spaceDelimitedKeywords = normalizeKeywords(builder.mKeywords);
        intentAction = builder.mIntentAction;
        intentTargetPackage = builder.mIntentTargetPackage;
        intentTargetClass = builder.mIntentTargetClass;
        enabled = builder.mEnabled;
        key = builder.mKey;
        userId = builder.mUserId;
        payloadType = builder.mPayloadType;
        payload = builder.mPayload != null ? ResultPayloadUtils.marshall(builder.mPayload)
                : null;
    }

    /**
     * Returns the doc id for this row.
     */
    public int getDocId() {
        // Eventually we want all DocIds to be the data_reference key. For settings values,
        // this will be preference keys, and for non-settings they should be unique.
        return TextUtils.isEmpty(key)
                ? Objects.hash(updatedTitle, className, screenTitle, intentTargetClass)
                : key.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder(updatedTitle)
                .append(": ")
                .append(updatedSummaryOn)
                .toString();
    }

    /**
     * In the list of keywords, replace the comma and all subsequent whitespace with a single space.
     */
    public static String normalizeKeywords(String input) {
        return (input != null) ? input.replaceAll(LIST_DELIMITERS, SPACE) : EMPTY;
    }

    /**
     * @return {@param input} where all non-standard hyphens are replaced by normal hyphens.
     */
    public static String normalizeHyphen(String input) {
        return (input != null) ? input.replaceAll(NON_BREAKING_HYPHEN, HYPHEN) : EMPTY;
    }

    /**
     * @return {@param input} with all hyphens removed, and all letters lower case.
     */
    public static String normalizeString(String input) {
        final String normalizedHypen = normalizeHyphen(input);
        final String nohyphen = (input != null) ? normalizedHypen.replaceAll(HYPHEN, EMPTY) : EMPTY;
        final String normalized = Normalizer.normalize(nohyphen, Normalizer.Form.NFD);

        return REMOVE_DIACRITICALS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    public static String normalizeJapaneseString(String input) {
        final String nohyphen = (input != null) ? input.replaceAll(HYPHEN, EMPTY) : EMPTY;
        final String normalized = Normalizer.normalize(nohyphen, Normalizer.Form.NFKD);
        final StringBuffer sb = new StringBuffer();
        final int length = normalized.length();
        for (int i = 0; i < length; i++) {
            char c = normalized.charAt(i);
            // Convert Hiragana to full-width Katakana
            if (c >= '\u3041' && c <= '\u3096') {
                sb.append((char) (c - '\u3041' + '\u30A1'));
            } else {
                sb.append(c);
            }
        }

        return REMOVE_DIACRITICALS_PATTERN.matcher(sb.toString()).replaceAll("").toLowerCase();
    }

    public static class Builder {
        private String mTitle;
        private String mSummaryOn;
        private String mEntries;
        private String mClassName;
        private String mChildClassName;
        private String mScreenTitle;
        private int mIconResId;
        private String mKeywords;
        private String mIntentAction;
        private String mIntentTargetPackage;
        private String mIntentTargetClass;
        private boolean mEnabled;
        private String mKey;
        private int mUserId;
        @ResultPayload.PayloadType
        private int mPayloadType;
        private ResultPayload mPayload;

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setSummaryOn(String summaryOn) {
            mSummaryOn = summaryOn;
            return this;
        }

        public Builder setEntries(String entries) {
            mEntries = entries;
            return this;
        }

        public Builder setClassName(String className) {
            mClassName = className;
            return this;
        }

        public Builder setChildClassName(String childClassName) {
            mChildClassName = childClassName;
            return this;
        }

        public Builder setScreenTitle(String screenTitle) {
            mScreenTitle = screenTitle;
            return this;
        }

        public Builder setIconResId(int iconResId) {
            mIconResId = iconResId;
            return this;
        }

        public Builder setKeywords(String keywords) {
            mKeywords = keywords;
            return this;
        }

        public Builder setIntentAction(String intentAction) {
            mIntentAction = intentAction;
            return this;
        }

        public Builder setIntentTargetPackage(String intentTargetPackage) {
            mIntentTargetPackage = intentTargetPackage;
            return this;
        }

        public Builder setIntentTargetClass(String intentTargetClass) {
            mIntentTargetClass = intentTargetClass;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        public Builder setKey(String key) {
            mKey = key;
            return this;
        }

        public Builder setUserId(int userId) {
            mUserId = userId;
            return this;
        }

        public Builder setPayload(ResultPayload payload) {
            mPayload = payload;

            if (mPayload != null) {
                setPayloadType(mPayload.getType());
            }
            return this;
        }

        /**
         * Payload type is added when a Payload is added to the Builder in {setPayload}
         *
         * @param payloadType PayloadType
         * @return The Builder
         */
        private Builder setPayloadType(@ResultPayload.PayloadType int payloadType) {
            mPayloadType = payloadType;
            return this;
        }

        /**
         * Adds intent to inline payloads, or creates an Intent Payload as a fallback if the
         * payload is null.
         */
        private void setIntent(Context context) {
            if (mPayload != null) {
                return;
            }
            final Intent intent = buildIntent(context);
            mPayload = new ResultPayload(intent);
            mPayloadType = ResultPayload.PayloadType.INTENT;
        }

        /**
         * Adds Intent payload to builder.
         */
        private Intent buildIntent(Context context) {
            final Intent intent;

            boolean isEmptyIntentAction = TextUtils.isEmpty(mIntentAction);
            // No intent action is set, or the intent action is for a subsetting.
            if (isEmptyIntentAction) {
                // Action is null, we will launch it as a sub-setting
                intent = DatabaseIndexingUtils.buildSearchResultPageIntent(context, mClassName,
                        mKey, mScreenTitle);
            } else {
                intent = new Intent(mIntentAction);
                final String targetClass = mIntentTargetClass;
                if (!TextUtils.isEmpty(mIntentTargetPackage)
                        && !TextUtils.isEmpty(targetClass)) {
                    final ComponentName component = new ComponentName(mIntentTargetPackage,
                            targetClass);
                    intent.setComponent(component);
                }
                intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, mKey);
            }
            return intent;
        }

        public IndexData build(Context context) {
            setIntent(context);
            return new IndexData(this);
        }
    }
}