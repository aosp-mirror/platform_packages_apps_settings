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

package com.android.settings.homepage;

import android.content.Context;
import android.widget.BaseAdapter;

import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a centralized manager of multiple {@link HomepageCardController}.
 *
 * {@link HomepageManager} first loads data from {@link CardContentLoader} and gets back a list of
 * {@link HomepageCard}. All subclasses of {@link HomepageCardController} are loaded here, which
 * will then trigger the {@link HomepageCardController} to load its data and listen to
 * corresponding changes. When every single {@link HomepageCardController} updates its data, the
 * data will be passed here, then going through some sorting mechanisms. The
 * {@link HomepageCardController} will end up building a list of {@link HomepageCard} for {@link
 * HomepageAdapter} and {@link BaseAdapter#notifyDataSetChanged()} will be called to get the page
 * refreshed.
 */
public class HomepageManager implements CardContentLoader.CardContentLoaderListener,
        HomepageCardUpdateListener {

    private static final String TAG = "HomepageManager";
    //The list for Settings Custom Card
    @HomepageCard.CardType
    private static final int[] SETTINGS_CARDS = {HomepageCard.CardType.CONDITIONAL};

    private final Context mContext;
    private final ControllerRendererPool mControllerRendererPool;
    private final Lifecycle mLifecycle;

    private List<HomepageCard> mHomepageCards;
    private HomepageCardUpdateListener mListener;


    public HomepageManager(Context context, Lifecycle lifecycle) {
        mContext = context;
        mLifecycle = lifecycle;
        mHomepageCards = new ArrayList<>();
        mControllerRendererPool = new ControllerRendererPool();
    }

    void startCardContentLoading() {
        final CardContentLoader cardContentLoader = new CardContentLoader();
        cardContentLoader.setListener(this);
    }

    private void loadCardControllers() {
        if (mHomepageCards != null) {
            for (HomepageCard card : mHomepageCards) {
                setupController(card.getCardType());
            }
        }

        //for data provided by Settings
        for (int cardType : SETTINGS_CARDS) {
            setupController(cardType);
        }
    }

    private void setupController(int cardType) {
        final HomepageCardController controller = mControllerRendererPool.getController(mContext,
                cardType);
        if (controller != null) {
            controller.setHomepageCardUpdateListener(this);
            controller.setLifecycle(mLifecycle);
        }
    }

    //TODO(b/111822376): implement sorting mechanism.
    private void sortCards() {
        //take mHomepageCards as the source and do the ranking based on the rule.
    }

    @Override
    public void onHomepageCardUpdated(int cardType, List<HomepageCard> updateList) {
        //TODO(b/112245748): Should implement a DiffCallback.
        //Keep the old list for comparison.
        final List<HomepageCard> prevCards = mHomepageCards;

        //Remove the existing data that matches the certain cardType so as to insert the new data.
        for (int i = mHomepageCards.size() - 1; i >= 0; i--) {
            if (mHomepageCards.get(i).getCardType() == cardType) {
                mHomepageCards.remove(i);
            }
        }

        //Append the new data
        mHomepageCards.addAll(updateList);

        sortCards();

        if (mListener != null) {
            mListener.onHomepageCardUpdated(HomepageCard.CardType.INVALID, mHomepageCards);
        }
    }

    @Override
    public void onFinishCardLoading(List<HomepageCard> homepageCards) {
        mHomepageCards = homepageCards;

        //Force card sorting here in case CardControllers of custom view have nothing to update
        // for the first launch.
        sortCards();

        loadCardControllers();
    }

    void setListener(HomepageCardUpdateListener listener) {
        mListener = listener;
    }

    public ControllerRendererPool getControllerRendererPool() {
        return mControllerRendererPool;
    }
}
