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

import static com.android.settings.homepage.contextualcards.ContextualCardsAdapter.SPAN_COUNT;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.homepage.contextualcards.slices.SwipeDismissalDelegate;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.slice.ContextualWifiScanWorker;

public class ContextualCardsFragment extends InstrumentedFragment implements
        FocusRecyclerView.FocusListener {

    private FocusRecyclerView mCardsContainer;
    private GridLayoutManager mLayoutManager;
    private ContextualCardsAdapter mContextualCardsAdapter;
    private ContextualCardManager mContextualCardManager;
    private ItemTouchHelper mItemTouchHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getContext();
        if (savedInstanceState == null) {
            FeatureFactory.getFactory(context).getSlicesFeatureProvider().newUiSession();
        }
        mContextualCardManager = new ContextualCardManager(context, getSettingsLifecycle(),
                savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
        ContextualWifiScanWorker.newVisibleUiSession();
        mContextualCardManager.loadContextualCards(LoaderManager.getInstance(this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final Context context = getContext();
        final View rootView = inflater.inflate(R.layout.settings_homepage, container, false);
        mCardsContainer = rootView.findViewById(R.id.card_container);
        mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT,
                GridLayoutManager.VERTICAL, false /* reverseLayout */);
        mCardsContainer.setLayoutManager(mLayoutManager);
        mContextualCardsAdapter = new ContextualCardsAdapter(context, this /* lifecycleOwner */,
                mContextualCardManager);
        mCardsContainer.setAdapter(mContextualCardsAdapter);
        mContextualCardManager.setListener(mContextualCardsAdapter);
        mCardsContainer.setListener(this);
        mItemTouchHelper = new ItemTouchHelper(new SwipeDismissalDelegate(mContextualCardsAdapter));
        mItemTouchHelper.attachToRecyclerView(mCardsContainer);

        return rootView;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        mContextualCardManager.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_HOMEPAGE;
    }
}
