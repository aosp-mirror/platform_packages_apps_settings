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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.homepage.contextualcards.slices.BluetoothUpdateWorker;
import com.android.settings.homepage.contextualcards.slices.SwipeDismissalDelegate;
import com.android.settings.overlay.FeatureFactory;

public class ContextualCardsFragment extends InstrumentedFragment implements
        FocusRecyclerView.FocusListener {

    private static final String TAG = "ContextualCardsFragment";
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;

    @VisibleForTesting
    static boolean sRestartLoaderNeeded;

    @VisibleForTesting
    BroadcastReceiver mKeyEventReceiver;
    @VisibleForTesting
    BroadcastReceiver mScreenOffReceiver;

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
            FeatureFactory.getFeatureFactory().getSlicesFeatureProvider().newUiSession();
            BluetoothUpdateWorker.initLocalBtManager(getContext());
        }
        mContextualCardManager = new ContextualCardManager(context, getSettingsLifecycle(),
                savedInstanceState);
        mKeyEventReceiver = new KeyEventReceiver();
    }

    @Override
    public void onStart() {
        super.onStart();
        registerScreenOffReceiver();
        registerKeyEventReceiver();
        mContextualCardManager.loadContextualCards(LoaderManager.getInstance(this),
                sRestartLoaderNeeded);
        sRestartLoaderNeeded = false;
    }

    @Override
    public void onStop() {
        unregisterKeyEventReceiver();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        unregisterScreenOffReceiver();
        super.onDestroy();
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
        mCardsContainer.setItemAnimator(null);
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

    private void registerKeyEventReceiver() {
        getActivity().registerReceiver(mKeyEventReceiver,
                new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), Context.RECEIVER_EXPORTED);
    }

    private void unregisterKeyEventReceiver() {
        getActivity().unregisterReceiver(mKeyEventReceiver);
    }

    private void registerScreenOffReceiver() {
        if (mScreenOffReceiver == null) {
            mScreenOffReceiver = new ScreenOffReceiver();
            getActivity().registerReceiver(mScreenOffReceiver,
                    new IntentFilter(Intent.ACTION_SCREEN_OFF));
        }
    }

    private void unregisterScreenOffReceiver() {
        if (mScreenOffReceiver != null) {
            getActivity().unregisterReceiver(mScreenOffReceiver);
            mScreenOffReceiver = null;
        }
    }

    private void resetSession(Context context) {
        sRestartLoaderNeeded = true;
        unregisterScreenOffReceiver();
        FeatureFactory.getFeatureFactory().getSlicesFeatureProvider().newUiSession();
    }

    /**
     * Receiver for updating UI session when home key or recent app key is pressed.
     */
    @VisibleForTesting
    class KeyEventReceiver extends BroadcastReceiver {

        private static final String KEY_REASON = "reason";
        private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                return;
            }

            final String reason = intent.getStringExtra(KEY_REASON);
            if (!SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)
                    && !SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                return;
            }

            if (DEBUG) {
                Log.d(TAG, "key pressed = " + reason);
            }
            resetSession(context);
        }
    }

    /**
     * Receiver for updating UI session when screen is turned off.
     */
    @VisibleForTesting
    class ScreenOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                return;
            }

            if (DEBUG) {
                Log.d(TAG, "screen off");
            }
            resetSession(context);
        }
    }
}
