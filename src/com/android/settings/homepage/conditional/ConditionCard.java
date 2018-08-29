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

package com.android.settings.homepage.conditional;

import com.android.settings.homepage.HomepageCard;

/**
 * Data class representing a {@link ConditionCard}.
 *
 * Use this class to store additional attributes on top of {@link HomepageCard} for
 * {@link ConditionalCard}.
 */
public class ConditionCard extends HomepageCard {

    private final long mConditionId;
    private final int mMetricsConstant;
    private final CharSequence mActionText;

    private ConditionCard(Builder builder) {
        super(builder);

        mConditionId = builder.mConditionId;
        mMetricsConstant = builder.mMetricsConstant;
        mActionText = builder.mActionText;
    }

    public long getConditionId() {
        return mConditionId;
    }

    public int getMetricsConstant() {
        return mMetricsConstant;
    }

    public CharSequence getActionText() {
        return mActionText;
    }

    static class Builder extends HomepageCard.Builder {

        private long mConditionId;
        private int mMetricsConstant;
        private CharSequence mActionText;

        public Builder setConditionId(long id) {
            mConditionId = id;
            return this;
        }

        public Builder setMetricsConstant(int metricsConstant) {
            mMetricsConstant = metricsConstant;
            return this;
        }

        public Builder setActionText(CharSequence actionText) {
            mActionText = actionText;
            return this;
        }

        public ConditionCard build() {
            return new ConditionCard(this);
        }
    }
}