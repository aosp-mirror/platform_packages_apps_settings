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

package com.android.settings.homepage.contextualcards.slices;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

import androidx.annotation.LayoutRes;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.CardContentProvider;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardRenderer;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;

import java.util.Map;
import java.util.Set;

/**
 * Card renderer for {@link ContextualCard} built as slice full card or slice half card.
 */
public class SliceContextualCardRenderer implements ContextualCardRenderer, LifecycleObserver {
    public static final int VIEW_TYPE_FULL_WIDTH = R.layout.homepage_slice_tile;
    public static final int VIEW_TYPE_HALF_WIDTH = R.layout.homepage_slice_half_tile;
    public static final int VIEW_TYPE_DEFERRED_SETUP = R.layout.homepage_slice_deferred_setup_tile;

    private static final String TAG = "SliceCardRenderer";

    @VisibleForTesting
    final Map<Uri, LiveData<Slice>> mSliceLiveDataMap;
    @VisibleForTesting
    final Set<RecyclerView.ViewHolder> mFlippedCardSet;

    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    private final ControllerRendererPool mControllerRendererPool;
    private final SliceDeferredSetupCardRendererHelper mDeferredSetupCardHelper;
    private final SliceFullCardRendererHelper mFullCardHelper;
    private final SliceHalfCardRendererHelper mHalfCardHelper;

    public SliceContextualCardRenderer(Context context, LifecycleOwner lifecycleOwner,
            ControllerRendererPool controllerRendererPool) {
        mContext = context;
        mLifecycleOwner = lifecycleOwner;
        mSliceLiveDataMap = new ArrayMap<>();
        mControllerRendererPool = controllerRendererPool;
        mFlippedCardSet = new ArraySet<>();
        mLifecycleOwner.getLifecycle().addObserver(this);
        mFullCardHelper = new SliceFullCardRendererHelper(context);
        mHalfCardHelper = new SliceHalfCardRendererHelper(context);
        mDeferredSetupCardHelper = new SliceDeferredSetupCardRendererHelper(context);
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View view, @LayoutRes int viewType) {
        switch (viewType) {
            case VIEW_TYPE_DEFERRED_SETUP:
                return mDeferredSetupCardHelper.createViewHolder(view);
            case VIEW_TYPE_HALF_WIDTH:
                return mHalfCardHelper.createViewHolder(view);
            default:
                return mFullCardHelper.createViewHolder(view);
        }
    }

    @Override
    public void bindView(RecyclerView.ViewHolder holder, ContextualCard card) {
        final Uri uri = card.getSliceUri();

        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Log.w(TAG, "Invalid uri, skipping slice: " + uri);
            return;
        }

        LiveData<Slice> sliceLiveData = mSliceLiveDataMap.get(uri);

        if (sliceLiveData == null) {
            sliceLiveData = SliceLiveData.fromUri(mContext, uri);
            mSliceLiveDataMap.put(uri, sliceLiveData);
        }

        sliceLiveData.removeObservers(mLifecycleOwner);
        sliceLiveData.observe(mLifecycleOwner, slice -> {
            if (slice == null) {
                Log.w(TAG, "Slice is null");
                mContext.getContentResolver().notifyChange(CardContentProvider.REFRESH_CARD_URI,
                        null);
                return;
            }

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_DEFERRED_SETUP:
                    mDeferredSetupCardHelper.bindView(holder, card, slice);
                    break;
                case VIEW_TYPE_HALF_WIDTH:
                    mHalfCardHelper.bindView(holder, card, slice);
                    break;
                default:
                    mFullCardHelper.bindView(holder, card, slice);
            }
        });

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_DEFERRED_SETUP:
                // Deferred setup is never dismissible.
                break;
            case VIEW_TYPE_HALF_WIDTH:
                initDismissalActions(holder, card, R.id.content);
                break;
            default:
                initDismissalActions(holder, card, R.id.slice_view);
        }
    }

    private void initDismissalActions(RecyclerView.ViewHolder holder, ContextualCard card,
            int initialViewId) {
        // initialView is the first view in the ViewFlipper.
        final View initialView = holder.itemView.findViewById(initialViewId);
        initialView.setOnLongClickListener(v -> {
            flipCardToDismissalView(holder);
            mFlippedCardSet.add(holder);
            return true;
        });

        final Button btnKeep = holder.itemView.findViewById(R.id.keep);
        btnKeep.setOnClickListener(v -> {
            mFlippedCardSet.remove(holder);
            resetCardView(holder);
        });

        final Button btnRemove = holder.itemView.findViewById(R.id.remove);
        btnRemove.setOnClickListener(v -> {
            mControllerRendererPool.getController(mContext, card.getCardType()).onDismissed(card);
            mFlippedCardSet.remove(holder);
            resetCardView(holder);
            mSliceLiveDataMap.get(card.getSliceUri()).removeObservers(mLifecycleOwner);
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mFlippedCardSet.stream().forEach(holder -> resetCardView(holder));
        mFlippedCardSet.clear();
    }

    private void resetCardView(RecyclerView.ViewHolder holder) {
        final ViewFlipper viewFlipper = holder.itemView.findViewById(R.id.view_flipper);
        viewFlipper.setDisplayedChild(0 /* whichChild */);
    }

    private void flipCardToDismissalView(RecyclerView.ViewHolder holder) {
        final ViewFlipper viewFlipper = holder.itemView.findViewById(R.id.view_flipper);
        viewFlipper.showNext();
    }
}
