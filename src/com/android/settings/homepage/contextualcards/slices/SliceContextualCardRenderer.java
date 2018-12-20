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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.widget.EventInfo;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.CardContentProvider;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider;
import com.android.settings.homepage.contextualcards.ContextualCardRenderer;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;
import com.android.settings.overlay.FeatureFactory;

import java.util.Map;
import java.util.Set;

/**
 * Card renderer for {@link ContextualCard} built as slices.
 */
public class SliceContextualCardRenderer implements ContextualCardRenderer,
        SliceView.OnSliceActionListener, LifecycleObserver {
    public static final int VIEW_TYPE = R.layout.homepage_slice_tile;

    private static final String TAG = "SliceCardRenderer";

    @VisibleForTesting
    final Map<Uri, LiveData<Slice>> mSliceLiveDataMap;
    @VisibleForTesting
    final Set<SliceViewHolder> mFlippedCardSet;

    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    private final ControllerRendererPool mControllerRendererPool;
    private final Set<ContextualCard> mCardSet;

    public SliceContextualCardRenderer(Context context, LifecycleOwner lifecycleOwner,
            ControllerRendererPool controllerRendererPool) {
        mContext = context;
        mLifecycleOwner = lifecycleOwner;
        mSliceLiveDataMap = new ArrayMap<>();
        mControllerRendererPool = controllerRendererPool;
        mCardSet = new ArraySet<>();
        mFlippedCardSet = new ArraySet<>();
        mLifecycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public int getViewType(boolean isHalfWidth) {
        return VIEW_TYPE;
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View view) {
        return new SliceViewHolder(view);
    }

    @Override
    public void bindView(RecyclerView.ViewHolder holder, ContextualCard card) {
        final SliceViewHolder cardHolder = (SliceViewHolder) holder;
        final Uri uri = card.getSliceUri();
        //TODO(b/120629936): Take this out once blank card issue is fixed.
        Log.d(TAG, "bindView - uri = " + uri);

        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Log.w(TAG, "Invalid uri, skipping slice: " + uri);
            return;
        }

        cardHolder.sliceView.setScrollable(false);
        cardHolder.sliceView.setTag(uri);
        //TODO(b/114009676): We will soon have a field to decide what slice mode we should set.
        cardHolder.sliceView.setMode(SliceView.MODE_LARGE);
        LiveData<Slice> sliceLiveData = mSliceLiveDataMap.get(uri);

        if (sliceLiveData == null) {
            sliceLiveData = SliceLiveData.fromUri(mContext, uri);
            mSliceLiveDataMap.put(uri, sliceLiveData);
        }
        mCardSet.add(card);

        sliceLiveData.removeObservers(mLifecycleOwner);
        sliceLiveData.observe(mLifecycleOwner, slice -> {
            if (slice == null) {
                Log.w(TAG, "Slice is null");
                mContext.getContentResolver().notifyChange(CardContentProvider.URI, null);
                return;
            } else {
                //TODO(b/120629936): Take this out once blank card issue is fixed.
                Log.d(TAG, "Slice callback - uri = " + slice.getUri());
            }
            cardHolder.sliceView.setSlice(slice);
        });

        // Set this listener so we can log the interaction users make on the slice
        cardHolder.sliceView.setOnSliceActionListener(this);

        // Customize slice view for Settings
        cardHolder.sliceView.showTitleItems(true);
        if (card.isLargeCard()) {
            cardHolder.sliceView.showHeaderDivider(true);
            cardHolder.sliceView.showActionDividers(true);
        }

        initDismissalActions(cardHolder, card);
    }

    private void initDismissalActions(SliceViewHolder cardHolder, ContextualCard card) {
        cardHolder.sliceView.setOnLongClickListener(v -> {
            cardHolder.viewFlipper.showNext();
            mFlippedCardSet.add(cardHolder);
            return true;
        });

        final Button btnKeep = cardHolder.itemView.findViewById(R.id.keep);
        btnKeep.setOnClickListener(v -> {
            cardHolder.resetCard();
            mFlippedCardSet.remove(cardHolder);
        });

        final Button btnRemove = cardHolder.itemView.findViewById(R.id.remove);
        btnRemove.setOnClickListener(v -> {
            mControllerRendererPool.getController(mContext, card.getCardType()).onDismissed(card);
            cardHolder.resetCard();
            mFlippedCardSet.remove(cardHolder);
            mSliceLiveDataMap.get(card.getSliceUri()).removeObservers(mLifecycleOwner);
        });
    }

    @Override
    public void onSliceAction(@NonNull EventInfo eventInfo, @NonNull SliceItem sliceItem) {
        //TODO(b/79698338): Log user interaction

        // sliceItem.getSlice().getUri() is like
        // content://android.settings.slices/action/wifi/_gen/0/_gen/0
        // contextualCard.getSliceUri() is prefix of sliceItem.getSlice().getUri()
        for (ContextualCard card : mCardSet) {
            if (sliceItem.getSlice().getUri().toString().startsWith(
                    card.getSliceUri().toString())) {
                ContextualCardFeatureProvider contexualCardFeatureProvider =
                        FeatureFactory.getFactory(mContext)
                                .getContextualCardFeatureProvider(mContext);
                contexualCardFeatureProvider.logContextualCardClick(card,
                        eventInfo.rowIndex, eventInfo.actionType);
                break;
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mFlippedCardSet.stream().forEach(holder -> holder.resetCard());
        mFlippedCardSet.clear();
    }

    public static class SliceViewHolder extends RecyclerView.ViewHolder {
        public final SliceView sliceView;
        public final ViewFlipper viewFlipper;

        public SliceViewHolder(View view) {
            super(view);
            sliceView = view.findViewById(R.id.slice_view);
            viewFlipper = view.findViewById(R.id.view_flipper);
        }

        public void resetCard() {
            viewFlipper.setDisplayedChild(0);
        }
    }
}
