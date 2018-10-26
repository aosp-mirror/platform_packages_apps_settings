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

import static java.util.stream.Collectors.groupingBy;

import android.content.Context;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        ContextualCardUpdateListener {

    private static final String TAG = "ContextualCardManager";
    //The list for Settings Custom Card
    @ContextualCard.CardType
    private static final int[] SETTINGS_CARDS = {ContextualCard.CardType.CONDITIONAL};

    private final Context mContext;
    private final ControllerRendererPool mControllerRendererPool;
    private final Lifecycle mLifecycle;
    private final List<ContextualCard> mContextualCards;
    private final List<LifecycleObserver> mLifecycleObservers;

    private ContextualCardUpdateListener mListener;

    public ContextualCardManager(Context context, @NonNull Lifecycle lifecycle) {
        mContext = context;
        mLifecycle = lifecycle;
        mContextualCards = new ArrayList<>();
        mLifecycleObservers = new ArrayList<>();
        mControllerRendererPool = new ControllerRendererPool();
        //for data provided by Settings
        for (int cardType : SETTINGS_CARDS) {
            setupController(cardType);
        }
    }

    void loadContextualCards(ContextualCardsFragment fragment) {
        final CardContentLoaderCallbacks cardContentLoaderCallbacks =
                new CardContentLoaderCallbacks(mContext);
        cardContentLoaderCallbacks.setListener(this);
        LoaderManager.getInstance(fragment).initLoader(CARD_CONTENT_LOADER_ID, null /* bundle */,
                cardContentLoaderCallbacks);
    }

    private void loadCardControllers() {
        for (ContextualCard card : mContextualCards) {
            setupController(card.getCardType());
        }
    }

    private void setupController(int cardType) {
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

    //TODO(b/111822376): implement sorting mechanism.
    private void sortCards(List<ContextualCard> cards) {
        //take mContextualCards as the source and do the ranking based on the rule.
    }

    @Override
    public void onContextualCardUpdated(Map<Integer, List<ContextualCard>> updateList) {
        final Set<Integer> cardTypes = updateList.keySet();
        //Remove the existing data that matches the certain cardType before inserting new data.
        final List<ContextualCard> cardsToKeep = mContextualCards
                .stream()
                .filter(card -> !cardTypes.contains(card.getCardType()))
                .collect(Collectors.toList());
        final List<ContextualCard> allCards = new ArrayList<>();
        allCards.addAll(cardsToKeep);
        allCards.addAll(
                updateList.values().stream().flatMap(List::stream).collect(Collectors.toList()));

        sortCards(allCards);
        //replace with the new data
        mContextualCards.clear();
        mContextualCards.addAll(allCards);

        loadCardControllers();

        if (mListener != null) {
            final Map<Integer, List<ContextualCard>> cardsToUpdate = new ArrayMap<>();
            cardsToUpdate.put(ContextualCard.CardType.DEFAULT, mContextualCards);
            mListener.onContextualCardUpdated(cardsToUpdate);
        }
    }

    @Override
    public void onFinishCardLoading(List<ContextualCard> cards) {
        onContextualCardUpdated(cards.stream().collect(groupingBy(ContextualCard::getCardType)));
    }

    void setListener(ContextualCardUpdateListener listener) {
        mListener = listener;
    }

    public ControllerRendererPool getControllerRendererPool() {
        return mControllerRendererPool;
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
