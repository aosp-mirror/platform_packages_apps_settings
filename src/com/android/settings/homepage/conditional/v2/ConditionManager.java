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

package com.android.settings.homepage.conditional.v2;

import android.content.Context;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.conditional.ConditionListener;

import java.util.ArrayList;
import java.util.List;

public class ConditionManager {
    private static final String TAG = "ConditionManager";

    @VisibleForTesting
    final List<ConditionalCard> mCandidates;
    @VisibleForTesting
    final List<ConditionalCardController> mCardControllers;

    private final Context mAppContext;
    private final ConditionListener mListener;

    private boolean mIsListeningToStateChange;

    /**
     * Whether or not the new condition manager is should be used.
     */
    public static boolean isEnabled(Context context) {
        return FeatureFlagUtils.isEnabled(context, FeatureFlags.CONDITION_MANAGER_V2);
    }

    public ConditionManager(Context context, ConditionListener listener) {
        mAppContext = context.getApplicationContext();
        mCandidates = new ArrayList<>();
        mCardControllers = new ArrayList<>();
        mListener = listener;
        initCandidates();
    }

    /**
     * Returns a list of {@link ConditionalCard}s eligible for display.
     */
    public List<ConditionalCard> getDisplayableCards() {
        final List<ConditionalCard> cards = new ArrayList<>();
        for (ConditionalCard card : mCandidates) {
            if (getController(card.getId()).isDisplayable()) {
                cards.add(card);
            }
        }
        return cards;
    }

    /**
     * Handler when the card is clicked.
     *
     * @see {@link ConditionalCardController#onPrimaryClick(Context)}
     */
    public void onPrimaryClick(Context context, long id) {
        getController(id).onPrimaryClick(context);
    }

    /**
     * Handler when the card action is clicked.
     *
     * @see {@link ConditionalCardController#onActionClick()}
     */
    public void onActionClick(long id) {
        getController(id).onActionClick();
        onConditionChanged();
    }


    /**
     * Start monitoring state change for all conditions
     */
    public void startMonitoringStateChange() {
        if (mIsListeningToStateChange) {
            Log.d(TAG, "Already listening to condition state changes, skipping");
            return;
        }
        mIsListeningToStateChange = true;
        for (ConditionalCardController controller : mCardControllers) {
            controller.startMonitoringStateChange();
        }
        // Force a refresh on listener
        onConditionChanged();
    }

    /**
     * Stop monitoring state change for all conditions
     */
    public void stopMonitoringStateChange() {
        if (!mIsListeningToStateChange) {
            Log.d(TAG, "Not listening to condition state changes, skipping");
            return;
        }
        for (ConditionalCardController controller : mCardControllers) {
            controller.stopMonitoringStateChange();
        }
        mIsListeningToStateChange = false;
    }

    /**
     * Called when some conditional card's state has changed
     */
    void onConditionChanged() {
        if (mListener != null) {
            mListener.onConditionsChanged();
        }
    }

    @NonNull
    <T extends ConditionalCardController> T getController(long id) {
        for (ConditionalCardController controller : mCardControllers) {
            if (controller.getId() == id) {
                return (T) controller;
            }
        }
        throw new IllegalStateException("Cannot find controller for " + id);
    }

    private void initCandidates() {
        // Initialize controllers first.
        mCardControllers.add(new DndConditionCardController(mAppContext, this /* manager */));

        // Initialize ui model later. UI model depends on controller.
        mCandidates.add(new DndConditionCard(mAppContext, this /* manager */));
    }
}
