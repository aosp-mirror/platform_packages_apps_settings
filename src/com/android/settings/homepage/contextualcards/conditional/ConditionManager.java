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

package com.android.settings.homepage.contextualcards.conditional;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConditionManager {
    private static final String TAG = "ConditionManager";

    @VisibleForTesting
    final List<ConditionalCard> mCandidates;
    @VisibleForTesting
    final List<ConditionalCardController> mCardControllers;

    private static final long DISPLAYABLE_CHECKER_TIMEOUT_MS = 20;

    private final ExecutorService mExecutorService;
    private final Context mAppContext;
    private final ConditionListener mListener;

    private boolean mIsListeningToStateChange;

    public ConditionManager(Context context, ConditionListener listener) {
        mAppContext = context.getApplicationContext();
        mExecutorService = Executors.newCachedThreadPool();
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
        final List<Future<ConditionalCard>> displayableCards = new ArrayList<>();
        // Check displayable future
        for (ConditionalCard card : mCandidates) {
            final DisplayableChecker future = new DisplayableChecker(
                    card, getController(card.getId()));
            displayableCards.add(mExecutorService.submit(future));
        }
        // Collect future and add displayable cards
        for (Future<ConditionalCard> cardFuture : displayableCards) {
            try {
                final ConditionalCard card = cardFuture.get(DISPLAYABLE_CHECKER_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
                if (card != null) {
                    cards.add(card);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Log.w(TAG, "Failed to get displayable state for card, likely timeout. Skipping", e);
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
    }

    /**
     * Start monitoring state change for all conditions
     */
    public void startMonitoringStateChange() {
        if (mIsListeningToStateChange) {
            Log.d(TAG, "Already listening to condition state changes, skipping monitor setup");
        } else {
            mIsListeningToStateChange = true;
            for (ConditionalCardController controller : mCardControllers) {
                controller.startMonitoringStateChange();
            }
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
        mCardControllers.add(new AirplaneModeConditionController(mAppContext, this /* manager */));
        mCardControllers.add(
                new BackgroundDataConditionController(mAppContext, this /* manager */));
        mCardControllers.add(new BatterySaverConditionController(mAppContext, this /* manager */));
        mCardControllers.add(new CellularDataConditionController(mAppContext, this /* manager */));
        mCardControllers.add(new DndConditionCardController(mAppContext, this /* manager */));
        mCardControllers.add(new HotspotConditionController(mAppContext, this /* manager */));
        mCardControllers.add(new NightDisplayConditionController(mAppContext, this /* manager */));
        mCardControllers.add(new RingerVibrateConditionController(mAppContext, this /* manager */));
        mCardControllers.add(new RingerMutedConditionController(mAppContext, this /* manager */));
        mCardControllers.add(new WorkModeConditionController(mAppContext, this /* manager */));

        // Initialize ui model later. UI model depends on controller.
        mCandidates.add(new AirplaneModeConditionCard(mAppContext));
        mCandidates.add(new BackgroundDataConditionCard(mAppContext));
        mCandidates.add(new BatterySaverConditionCard(mAppContext));
        mCandidates.add(new CellularDataConditionCard(mAppContext));
        mCandidates.add(new DndConditionCard(mAppContext, this /* manager */));
        mCandidates.add(new HotspotConditionCard(mAppContext, this /* manager */));
        mCandidates.add(new NightDisplayConditionCard(mAppContext));
        mCandidates.add(new RingerMutedConditionCard(mAppContext));
        mCandidates.add(new RingerVibrateConditionCard(mAppContext));
        mCandidates.add(new WorkModeConditionCard(mAppContext));
    }

    /**
     * Returns card if controller says it's displayable. Otherwise returns null.
     */
    public static class DisplayableChecker implements Callable<ConditionalCard> {

        private final ConditionalCard mCard;
        private final ConditionalCardController mController;

        private DisplayableChecker(ConditionalCard card, ConditionalCardController controller) {
            mCard = card;
            mController = controller;
        }

        @Override
        public ConditionalCard call() throws Exception {
            return mController.isDisplayable() ? mCard : null;
        }
    }
}
