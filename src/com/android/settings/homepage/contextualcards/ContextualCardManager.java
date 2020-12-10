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

import static com.android.settings.homepage.contextualcards.ContextualCardLoader.CARD_CONTENT_LOADER_ID;
import static com.android.settings.intelligence.ContextualCardProto.ContextualCard.Category.STICKY_VALUE;
import static com.android.settings.intelligence.ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE;

import static java.util.stream.Collectors.groupingBy;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.contextualcards.conditional.ConditionalCardController;
import com.android.settings.homepage.contextualcards.logging.ContextualCardLogUtils;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This is a centralized manager of multiple {@link ContextualCardController}.
 *
 * {@link ContextualCardManager} first loads data from {@link ContextualCardLoader} and gets back a
 * list of {@link ContextualCard}. All subclasses of {@link ContextualCardController} are loaded
 * here, which will then trigger the {@link ContextualCardController} to load its data and listen to
 * corresponding changes. When every single {@link ContextualCardController} updates its data, the
 * data will be passed here, then going through some sorting mechanisms. The
 * {@link ContextualCardController} will end up building a list of {@link ContextualCard} for
 * {@link ContextualCardsAdapter} and {@link BaseAdapter#notifyDataSetChanged()} will be called to
 * get the page refreshed.
 */
public class ContextualCardManager implements ContextualCardLoader.CardContentLoaderListener,
        ContextualCardUpdateListener, LifecycleObserver, OnSaveInstanceState {

    @VisibleForTesting
    static final long CARD_CONTENT_LOADER_TIMEOUT_MS = DateUtils.SECOND_IN_MILLIS;
    @VisibleForTesting
    static final String KEY_GLOBAL_CARD_LOADER_TIMEOUT = "global_card_loader_timeout_key";
    @VisibleForTesting
    static final String KEY_CONTEXTUAL_CARDS = "key_contextual_cards";

    private static final String TAG = "ContextualCardManager";

    private final Context mContext;
    private final Lifecycle mLifecycle;
    private final List<LifecycleObserver> mLifecycleObservers;
    private ContextualCardUpdateListener mListener;

    @VisibleForTesting
    final ControllerRendererPool mControllerRendererPool;
    @VisibleForTesting
    final List<ContextualCard> mContextualCards;
    @VisibleForTesting
    long mStartTime;
    @VisibleForTesting
    boolean mIsFirstLaunch;
    @VisibleForTesting
    List<String> mSavedCards;

    public ContextualCardManager(Context context, Lifecycle lifecycle, Bundle savedInstanceState) {
        mContext = context;
        mLifecycle = lifecycle;
        mContextualCards = new ArrayList<>();
        mLifecycleObservers = new ArrayList<>();
        mControllerRendererPool = new ControllerRendererPool();
        mLifecycle.addObserver(this);
        if (savedInstanceState == null) {
            mIsFirstLaunch = true;
            mSavedCards = null;
        } else {
            mSavedCards = savedInstanceState.getStringArrayList(KEY_CONTEXTUAL_CARDS);
        }
        // for data provided by Settings
        for (@ContextualCard.CardType int cardType : getSettingsCards()) {
            setupController(cardType);
        }
    }

    void loadContextualCards(LoaderManager loaderManager, boolean restartLoaderNeeded) {
        if (mContext.getResources().getBoolean(R.bool.config_use_legacy_suggestion)) {
            Log.w(TAG, "Legacy suggestion contextual card enabled, skipping contextual cards.");
            return;
        }
        if (ContextualCardLoader.getCardCount(mContext) <= 0) {
            Log.w(TAG, "Card count is zero, skipping contextual cards.");
            return;
        }
        mStartTime = System.currentTimeMillis();
        final CardContentLoaderCallbacks cardContentLoaderCallbacks =
                new CardContentLoaderCallbacks(mContext);
        cardContentLoaderCallbacks.setListener(this);
        if (!restartLoaderNeeded) {
            // Use the cached data when navigating back to the first page and upon screen rotation.
            loaderManager.initLoader(CARD_CONTENT_LOADER_ID, null /* bundle */,
                    cardContentLoaderCallbacks);
        } else {
            // Reload all cards when navigating back after pressing home key, recent app key, or
            // turn off screen.
            mIsFirstLaunch = true;
            loaderManager.restartLoader(CARD_CONTENT_LOADER_ID, null /* bundle */,
                    cardContentLoaderCallbacks);
        }
    }

    private void loadCardControllers() {
        for (ContextualCard card : mContextualCards) {
            setupController(card.getCardType());
        }
    }

    @VisibleForTesting
    int[] getSettingsCards() {
        if (!FeatureFlagUtils.isEnabled(mContext, FeatureFlags.CONDITIONAL_CARDS)) {
            return new int[] {ContextualCard.CardType.LEGACY_SUGGESTION};
        }
        return new int[]
                {ContextualCard.CardType.CONDITIONAL, ContextualCard.CardType.LEGACY_SUGGESTION};
    }

    @VisibleForTesting
    void setupController(@ContextualCard.CardType int cardType) {
        final ContextualCardController controller = mControllerRendererPool.getController(mContext,
                cardType);
        if (controller == null) {
            Log.w(TAG, "Cannot find ContextualCardController for type " + cardType);
            return;
        }
        controller.setCardUpdateListener(this);
        if (controller instanceof LifecycleObserver && !mLifecycleObservers.contains(controller)) {
            mLifecycleObservers.add((LifecycleObserver) controller);
            mLifecycle.addObserver((LifecycleObserver) controller);
        }
    }

    @VisibleForTesting
    List<ContextualCard> sortCards(List<ContextualCard> cards) {
        // take mContextualCards as the source and do the ranking based on the rule.
        final List<ContextualCard> result = cards.stream()
                .sorted((c1, c2) -> Double.compare(c2.getRankingScore(), c1.getRankingScore()))
                .collect(Collectors.toList());
        final List<ContextualCard> stickyCards = result.stream()
                .filter(c -> c.getCategory() == STICKY_VALUE)
                .collect(Collectors.toList());
        // make sticky cards be at the tail end.
        result.removeAll(stickyCards);
        result.addAll(stickyCards);
        return result;
    }

    @Override
    public void onContextualCardUpdated(Map<Integer, List<ContextualCard>> updateList) {
        final Set<Integer> cardTypes = updateList.keySet();
        // Remove the existing data that matches the certain cardType before inserting new data.
        List<ContextualCard> cardsToKeep;

        // We are not sure how many card types will be in the database, so when the list coming
        // from the database is empty (e.g. no eligible cards/cards are dismissed), we cannot
        // assign a specific card type for its map which is sending here. Thus, we assume that
        // except Conditional cards, all other cards are from the database. So when the map sent
        // here is empty, we only keep Conditional cards.
        if (cardTypes.isEmpty()) {
            final Set<Integer> conditionalCardTypes = new TreeSet<Integer>() {{
                add(ContextualCard.CardType.CONDITIONAL);
                add(ContextualCard.CardType.CONDITIONAL_HEADER);
                add(ContextualCard.CardType.CONDITIONAL_FOOTER);
            }};
            cardsToKeep = mContextualCards.stream()
                    .filter(card -> conditionalCardTypes.contains(card.getCardType()))
                    .collect(Collectors.toList());
        } else {
            cardsToKeep = mContextualCards.stream()
                    .filter(card -> !cardTypes.contains(card.getCardType()))
                    .collect(Collectors.toList());
        }

        final List<ContextualCard> allCards = new ArrayList<>();
        allCards.addAll(cardsToKeep);
        allCards.addAll(
                updateList.values().stream().flatMap(List::stream).collect(Collectors.toList()));

        //replace with the new data
        mContextualCards.clear();
        final List<ContextualCard> sortedCards = sortCards(allCards);
        mContextualCards.addAll(getCardsWithViewType(sortedCards));

        loadCardControllers();

        if (mListener != null) {
            final Map<Integer, List<ContextualCard>> cardsToUpdate = new ArrayMap<>();
            cardsToUpdate.put(ContextualCard.CardType.DEFAULT, mContextualCards);
            mListener.onContextualCardUpdated(cardsToUpdate);
        }
    }

    @Override
    public void onFinishCardLoading(List<ContextualCard> cards) {
        final long loadTime = System.currentTimeMillis() - mStartTime;
        Log.d(TAG, "Total loading time = " + loadTime);

        final List<ContextualCard> cardsToKeep = getCardsToKeep(cards);

        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();

        // navigate back to the homepage, screen rotate or after card dismissal
        if (!mIsFirstLaunch) {
            onContextualCardUpdated(cardsToKeep.stream()
                    .collect(groupingBy(ContextualCard::getCardType)));
            metricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_CONTEXTUAL_CARD_SHOW,
                    ContextualCardLogUtils.buildCardListLog(cardsToKeep));
            return;
        }

        final long timeoutLimit = getCardLoaderTimeout();
        if (loadTime <= timeoutLimit) {
            onContextualCardUpdated(cards.stream()
                    .collect(groupingBy(ContextualCard::getCardType)));
            metricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_CONTEXTUAL_CARD_SHOW,
                    ContextualCardLogUtils.buildCardListLog(cards));
        } else {
            // log timeout occurrence
            metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_CONTEXTUAL_CARD_LOAD_TIMEOUT,
                    SettingsEnums.SETTINGS_HOMEPAGE,
                    null /* key */, (int) loadTime /* value */);

            // display a card on timeout if the one-card space is pre-allocated
            if (!cards.isEmpty() && ContextualCardLoader.getCardCount(mContext) == 1) {
                onContextualCardUpdated(cards.stream()
                        .collect(groupingBy(ContextualCard::getCardType)));
                metricsFeatureProvider.action(mContext,
                        SettingsEnums.ACTION_CONTEXTUAL_CARD_SHOW,
                        ContextualCardLogUtils.buildCardListLog(cards));
            }
        }
        // only log homepage display upon a fresh launch
        final long totalTime = System.currentTimeMillis() - mStartTime;
        metricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_CONTEXTUAL_HOME_SHOW, (int) totalTime);

        mIsFirstLaunch = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        final ArrayList<String> cards = mContextualCards.stream()
                .map(ContextualCard::getName)
                .collect(Collectors.toCollection(ArrayList::new));

        outState.putStringArrayList(KEY_CONTEXTUAL_CARDS, cards);
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // Duplicate a list to avoid java.util.ConcurrentModificationException.
        final List<ContextualCard> cards = new ArrayList<>(mContextualCards);
        boolean hasConditionController = false;
        for (ContextualCard card : cards) {
            final ContextualCardController controller = getControllerRendererPool()
                    .getController(mContext, card.getCardType());
            if (controller instanceof ConditionalCardController) {
                hasConditionController = true;
            }
            if (hasWindowFocus && controller instanceof OnStart) {
                ((OnStart) controller).onStart();
            }
            if (!hasWindowFocus && controller instanceof OnStop) {
                ((OnStop) controller).onStop();
            }
        }
        // Conditional cards will always be refreshed whether or not there are conditional cards
        // in the homepage.
        if (!hasConditionController) {
            final ContextualCardController controller = getControllerRendererPool()
                    .getController(mContext, ContextualCard.CardType.CONDITIONAL);
            if (hasWindowFocus && controller instanceof OnStart) {
                ((OnStart) controller).onStart();
            }
            if (!hasWindowFocus && controller instanceof OnStop) {
                ((OnStop) controller).onStop();
            }
        }
    }

    public ControllerRendererPool getControllerRendererPool() {
        return mControllerRendererPool;
    }

    void setListener(ContextualCardUpdateListener listener) {
        mListener = listener;
    }

    @VisibleForTesting
    List<ContextualCard> getCardsWithViewType(List<ContextualCard> cards) {
        if (cards.isEmpty()) {
            return cards;
        }

        final List<ContextualCard> result = getCardsWithStickyViewType(cards);
        return getCardsWithSuggestionViewType(result);
    }

    @VisibleForTesting
    long getCardLoaderTimeout() {
        // Return the timeout limit if Settings.Global has the KEY_GLOBAL_CARD_LOADER_TIMEOUT key,
        // else return default timeout.
        return Settings.Global.getLong(mContext.getContentResolver(),
                KEY_GLOBAL_CARD_LOADER_TIMEOUT, CARD_CONTENT_LOADER_TIMEOUT_MS);
    }

    private List<ContextualCard> getCardsWithSuggestionViewType(List<ContextualCard> cards) {
        // Shows as half cards if 2 suggestion type of cards are next to each other.
        // Shows as full card if 1 suggestion type of card lives alone.
        final List<ContextualCard> result = new ArrayList<>(cards);
        for (int index = 1; index < result.size(); index++) {
            final ContextualCard previous = result.get(index - 1);
            final ContextualCard current = result.get(index);
            if (current.getCategory() == SUGGESTION_VALUE
                    && previous.getCategory() == SUGGESTION_VALUE) {
                result.set(index - 1, previous.mutate().setViewType(
                        SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH).build());
                result.set(index, current.mutate().setViewType(
                        SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH).build());
                index++;
            }
        }
        return result;
    }

    private List<ContextualCard> getCardsWithStickyViewType(List<ContextualCard> cards) {
        final List<ContextualCard> result = new ArrayList<>(cards);
        for (int index = 0; index < result.size(); index++) {
            final ContextualCard card = cards.get(index);
            if (card.getCategory() == STICKY_VALUE) {
                result.set(index, card.mutate().setViewType(
                        SliceContextualCardRenderer.VIEW_TYPE_STICKY).build());
            }
        }
        return result;
    }

    @VisibleForTesting
    List<ContextualCard> getCardsToKeep(List<ContextualCard> cards) {
        if (mSavedCards != null) {
            //screen rotate
            final List<ContextualCard> cardsToKeep = cards.stream()
                    .filter(card -> mSavedCards.contains(card.getName()))
                    .collect(Collectors.toList());
            mSavedCards = null;
            return cardsToKeep;
        } else {
            //navigate back to the homepage or after dismissing a card
            return cards.stream()
                    .filter(card -> mContextualCards.contains(card))
                    .collect(Collectors.toList());
        }
    }

    static class CardContentLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<List<ContextualCard>> {

        private Context mContext;
        private ContextualCardLoader.CardContentLoaderListener mListener;

        CardContentLoaderCallbacks(Context context) {
            mContext = context.getApplicationContext();
        }

        protected void setListener(ContextualCardLoader.CardContentLoaderListener listener) {
            mListener = listener;
        }

        @NonNull
        @Override
        public Loader<List<ContextualCard>> onCreateLoader(int id, @Nullable Bundle bundle) {
            if (id == CARD_CONTENT_LOADER_ID) {
                return new ContextualCardLoader(mContext);
            } else {
                throw new IllegalArgumentException("Unknown loader id: " + id);
            }
        }

        @Override
        public void onLoadFinished(@NonNull Loader<List<ContextualCard>> loader,
                List<ContextualCard> contextualCards) {
            if (mListener != null) {
                mListener.onFinishCardLoading(contextualCards);
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<List<ContextualCard>> loader) {

        }
    }
}
