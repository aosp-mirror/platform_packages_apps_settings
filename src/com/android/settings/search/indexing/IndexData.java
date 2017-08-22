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
import com.android.settings.search.SearchIndexableResources;

import java.util.Objects;

/**
 * Data class representing a single row in the Setting Search results database.
 */
public class IndexData {
    public final String locale;
    public final String updatedTitle;
    public final String normalizedTitle;
    public final String updatedSummaryOn;
    public final String normalizedSummaryOn;
    public final String updatedSummaryOff;
    public final String normalizedSummaryOff;
    public final String entries;
    public final String className;
    public final String childClassName;
    public final String screenTitle;
    public final int iconResId;
    public final int rank;
    public final String spaceDelimitedKeywords;
    public final String intentAction;
    public final String intentTargetPackage;
    public final String intentTargetClass;
    public final boolean enabled;
    public final String key;
    public final int userId;
    public final int payloadType;
    public final byte[] payload;

    private IndexData(Builder builder) {
        locale = builder.mLocale;
        updatedTitle = builder.mUpdatedTitle;
        normalizedTitle = builder.mNormalizedTitle;
        updatedSummaryOn = builder.mUpdatedSummaryOn;
        normalizedSummaryOn = builder.mNormalizedSummaryOn;
        updatedSummaryOff = builder.mUpdatedSummaryOff;
        normalizedSummaryOff = builder.mNormalizedSummaryOff;
        entries = builder.mEntries;
        className = builder.mClassName;
        childClassName = builder.mChildClassName;
        screenTitle = builder.mScreenTitle;
        iconResId = builder.mIconResId;
        rank = builder.mRank;
        spaceDelimitedKeywords = builder.mSpaceDelimitedKeywords;
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

    public static class Builder {
        private String mLocale;
        private String mUpdatedTitle;
        private String mNormalizedTitle;
        private String mUpdatedSummaryOn;
        private String mNormalizedSummaryOn;
        private String mUpdatedSummaryOff;
        private String mNormalizedSummaryOff;
        private String mEntries;
        private String mClassName;
        private String mChildClassName;
        private String mScreenTitle;
        private int mIconResId;
        private int mRank;
        private String mSpaceDelimitedKeywords;
        private String mIntentAction;
        private String mIntentTargetPackage;
        private String mIntentTargetClass;
        private boolean mEnabled;
        private String mKey;
        private int mUserId;
        @ResultPayload.PayloadType
        private int mPayloadType;
        private ResultPayload mPayload;

        public Builder setLocale(String locale) {
            mLocale = locale;
            return this;
        }

        public Builder setUpdatedTitle(String updatedTitle) {
            mUpdatedTitle = updatedTitle;
            return this;
        }

        public Builder setNormalizedTitle(String normalizedTitle) {
            mNormalizedTitle = normalizedTitle;
            return this;
        }

        public Builder setUpdatedSummaryOn(String updatedSummaryOn) {
            mUpdatedSummaryOn = updatedSummaryOn;
            return this;
        }

        public Builder setNormalizedSummaryOn(String normalizedSummaryOn) {
            mNormalizedSummaryOn = normalizedSummaryOn;
            return this;
        }

        public Builder setUpdatedSummaryOff(String updatedSummaryOff) {
            mUpdatedSummaryOff = updatedSummaryOff;
            return this;
        }

        public Builder setNormalizedSummaryOff(String normalizedSummaryOff) {
            this.mNormalizedSummaryOff = normalizedSummaryOff;
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

        public Builder setRank(int rank) {
            mRank = rank;
            return this;
        }

        public Builder setSpaceDelimitedKeywords(String spaceDelimitedKeywords) {
            mSpaceDelimitedKeywords = spaceDelimitedKeywords;
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
            if (isEmptyIntentAction
                    || (!isEmptyIntentAction && TextUtils.equals(mIntentTargetPackage,
                    SearchIndexableResources.SUBSETTING_TARGET_PACKAGE))) {
                // Action is null, we will launch it as a sub-setting
                intent = DatabaseIndexingUtils.buildSubsettingIntent(context, mClassName, mKey,
                        mScreenTitle);
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