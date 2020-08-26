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

import static android.app.slice.Slice.HINT_ERROR;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.LayoutRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
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
import com.android.settingslib.utils.ThreadUtils;

import java.util.Map;
import java.util.Set;

/**
 * Card renderer for {@link ContextualCard} built as slice full card or slice half card.
 */
public class SliceContextualCardRenderer implements ContextualCardRenderer, LifecycleObserver {
    public static final int VIEW_TYPE_FULL_WIDTH = R.layout.contextual_slice_full_tile;
    public static final int VIEW_TYPE_HALF_WIDTH = R.layout.contextual_slice_half_tile;
    public static final int VIEW_TYPE_STICKY = R.layout.contextual_slice_sticky_tile;

    private static final String TAG = "SliceCardRenderer";

    @VisibleForTesting
    final Map<Uri, LiveData<Slice>> mSliceLiveDataMap;
    @VisibleForTesting
    final Set<RecyclerView.ViewHolder> mFlippedCardSet;

    private final Context mContext;
    private final LifecycleOwner mLifecycleOwner;
    private final ControllerRendererPool mControllerRendererPool;
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
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View view, @LayoutRes int viewType) {
        if (viewType == VIEW_TYPE_HALF_WIDTH) {
            return mHalfCardHelper.createViewHolder(view);
        }
        return mFullCardHelper.createViewHolder(view);
    }

    @Override
    public void bindView(RecyclerView.ViewHolder holder, ContextualCard card) {
        final Uri uri = card.getSliceUri();

        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Log.w(TAG, "Invalid uri, skipping slice: " + uri);
            return;
        }

        // Show cached slice first before slice binding completed to avoid jank.
        if (holder.getItemViewType() != VIEW_TYPE_HALF_WIDTH) {
            mFullCardHelper.bindView(holder, card, card.getSlice());
        }

        LiveData<Slice> sliceLiveData = mSliceLiveDataMap.get(uri);

        if (sliceLiveData == null) {
            sliceLiveData = SliceLiveData.fromUri(mContext, uri,
                    (int type, Throwable source) -> {
                        // onSliceError doesn't handle error Slices.
                        Log.w(TAG, "Slice may be null. uri = " + uri + ", error = " + type);
                        ThreadUtils.postOnMainThread(
                                () -> mSliceLiveDataMap.get(uri).removeObservers(mLifecycleOwner));
                        mContext.getContentResolver()
                                .notifyChange(CardContentProvider.REFRESH_CARD_URI, null);
                    });
            mSliceLiveDataMap.put(uri, sliceLiveData);
        }

        final View swipeBackground = holder.itemView.findViewById(R.id.dismissal_swipe_background);
        sliceLiveData.removeObservers(mLifecycleOwner);
        // set the background to GONE in case the holder is reused.
        if (swipeBackground != null) {
            swipeBackground.setVisibility(View.GONE);
        }
        sliceLiveData.observe(mLifecycleOwner, slice -> {
            if (slice == null) {
                // The logic handling this case is in OnErrorListener. Adding this check is to
                // prevent from NPE when it calls .hasHint().
                return;
            }
            if (slice.hasHint(HINT_ERROR)) {
                Log.w(TAG, "Slice has HINT_ERROR, skipping rendering. uri=" + slice.getUri());
                mSliceLiveDataMap.get(slice.getUri()).removeObservers(mLifecycleOwner);
                mContext.getContentResolver().notifyChange(CardContentProvider.REFRESH_CARD_URI,
                        null);
                return;
            }

            if (holder.getItemViewType() == VIEW_TYPE_HALF_WIDTH) {
                mHalfCardHelper.bindView(holder, card, slice);
            } else {
                mFullCardHelper.bindView(holder, card, slice);
            }
            if (swipeBackground != null) {
                swipeBackground.setVisibility(View.VISIBLE);
            }
        });

        if (holder.getItemViewType() != VIEW_TYPE_STICKY) {
            initDismissalActions(holder, card);

            if (card.isPendingDismiss()) {
                showDismissalView(holder);
                mFlippedCardSet.add(holder);
            }
        }
    }

    private void initDismissalActions(RecyclerView.ViewHolder holder, ContextualCard card) {
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

        ViewCompat.setAccessibilityDelegate(getInitialView(holder),
                new AccessibilityDelegateCompat() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(View host,
                            AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS);
                        info.setDismissable(true);
                    }

                    @Override
                    public boolean performAccessibilityAction(View host, int action, Bundle args) {
                        if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS) {
                            mControllerRendererPool.getController(mContext,
                                    card.getCardType()).onDismissed(card);
                        }
                        return super.performAccessibilityAction(host, action, args);
                    }
                });

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mFlippedCardSet.forEach(holder -> resetCardView(holder));
        mFlippedCardSet.clear();
    }

    private void resetCardView(RecyclerView.ViewHolder holder) {
        holder.itemView.findViewById(R.id.dismissal_view).setVisibility(View.GONE);
        getInitialView(holder).setVisibility(View.VISIBLE);
    }

    private void showDismissalView(RecyclerView.ViewHolder holder) {
        holder.itemView.findViewById(R.id.dismissal_view).setVisibility(View.VISIBLE);
        getInitialView(holder).setVisibility(View.INVISIBLE);
    }

    private View getInitialView(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == VIEW_TYPE_HALF_WIDTH) {
            return ((SliceHalfCardRendererHelper.HalfCardViewHolder) viewHolder).content;
        }
        return ((SliceFullCardRendererHelper.SliceViewHolder) viewHolder).sliceView;
    }
}
