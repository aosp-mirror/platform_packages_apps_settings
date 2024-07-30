/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.common.base.Equivalence;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * A set of icons to be displayed in a {@link CircularIconsPreference}
 *
 * @param <T> The type of the items in the set. Can be an arbitrary type, the only requirement
 *           being that the {@code drawableLoader} supplied to the constructor is able to produce
 *           a {@link Drawable} from it (for example a resource id, a Content Uri, etc).
 */
class CircularIconSet<T> {

    @VisibleForTesting // Can be set by tests, before creating instances.
    static ExecutorService sExecutorService = Executors.newCachedThreadPool();

    static final CircularIconSet<?> EMPTY = new CircularIconSet<>(ImmutableList.of(),
            unused -> new ColorDrawable(Color.BLACK));

    private final ImmutableList<T> mItems;
    private final Function<T, Drawable> mDrawableLoader;
    private final ListeningExecutorService mBackgroundExecutor;

    private final ConcurrentHashMap<T, Drawable> mCachedIcons;

    CircularIconSet(List<T> items, Function<T, Drawable> drawableLoader) {
        mItems = ImmutableList.copyOf(items);
        mDrawableLoader = drawableLoader;
        mBackgroundExecutor = MoreExecutors.listeningDecorator(sExecutorService);
        mCachedIcons = new ConcurrentHashMap<>();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("items", mItems).toString();
    }

    @SuppressWarnings("unchecked")
    <OtherT> boolean hasSameItemsAs(CircularIconSet<OtherT> other,
            @Nullable Equivalence<OtherT> equivalence) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (equivalence == null) {
            return mItems.equals(other.mItems);
        }
        // Check that types match before applying equivalence (statically unsafe). :(
        Optional<Class<?>> thisItemClass = this.mItems.stream().findFirst().map(T::getClass);
        Optional<Class<?>> otherItemClass = other.mItems.stream().findFirst().map(OtherT::getClass);
        if (!thisItemClass.equals(otherItemClass)) {
            return false;
        }
        return equivalence.pairwise().equivalent((Iterable<OtherT>) this.mItems, other.mItems);
    }

    int size() {
        return mItems.size();
    }

    /**
     * Loads all icons from the set, using the supplied {@code drawableLoader}, in a background
     * thread.
     */
    List<ListenableFuture<Drawable>> getIcons() {
        return getIcons(Integer.MAX_VALUE);
    }

    /**
     * Loads up to {@code maxSize} icons from the set, using the supplied {@code drawableLoader}, in
     * a background thread.
     */
    List<ListenableFuture<Drawable>> getIcons(int maxNumber) {
        return mItems.stream().limit(maxNumber)
                .map(this::loadIcon)
                .toList();
    }

    private ListenableFuture<Drawable> loadIcon(T item) {
        return mBackgroundExecutor.submit(() -> {
            if (mCachedIcons.containsKey(item)) {
                return mCachedIcons.get(item);
            }
            Drawable drawable = mDrawableLoader.apply(item);
            if (drawable != null) {
                mCachedIcons.put(item, drawable);
            }
            return drawable;
        });
    }
}
