/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.app.slice.Slice.HINT_ERROR;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceViewManager;
import androidx.slice.core.SliceAction;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EligibleCardChecker implements Callable<ContextualCard> {

    private static final String TAG = "EligibleCardChecker";
    private static final long LATCH_TIMEOUT_MS = 200;

    private final Context mContext;

    @VisibleForTesting
    ContextualCard mCard;

    EligibleCardChecker(Context context, ContextualCard card) {
        mContext = context;
        mCard = card;
    }

    @Override
    public ContextualCard call() throws Exception {
        final long startTime = System.currentTimeMillis();
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();
        ContextualCard result;

        if (isCardEligibleToDisplay(mCard)) {
            metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_CONTEXTUAL_CARD_ELIGIBILITY,
                    SettingsEnums.SETTINGS_HOMEPAGE,
                    mCard.getTextSliceUri() /* key */, 1 /* true */);
            result = mCard;
        } else {
            metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_CONTEXTUAL_CARD_ELIGIBILITY,
                    SettingsEnums.SETTINGS_HOMEPAGE,
                    mCard.getTextSliceUri() /* key */, 0 /* false */);
            result = null;
        }
        // Log individual card loading time
        metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_CONTEXTUAL_CARD_LOAD,
                SettingsEnums.SETTINGS_HOMEPAGE,
                mCard.getTextSliceUri() /* key */,
                (int) (System.currentTimeMillis() - startTime) /* value */);

        return result;
    }

    @VisibleForTesting
    boolean isCardEligibleToDisplay(ContextualCard card) {
        if (card.getRankingScore() < 0) {
            return false;
        }
        if (card.isCustomCard()) {
            return true;
        }

        final Uri uri = card.getSliceUri();
        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return false;
        }

        final Slice slice = bindSlice(uri);

        if (isSliceToggleable(slice)) {
            mCard = card.mutate().setHasInlineAction(true).build();
        }

        if (slice == null || slice.hasHint(HINT_ERROR)) {
            Log.w(TAG, "Failed to bind slice, not eligible for display " + uri);
            return false;
        }
        return true;
    }

    @VisibleForTesting
    Slice bindSlice(Uri uri) {
        final SliceViewManager manager = SliceViewManager.getInstance(mContext);
        final Slice[] returnSlice = new Slice[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final SliceViewManager.SliceCallback callback =
                new SliceViewManager.SliceCallback() {
                    @Override
                    public void onSliceUpdated(Slice slice) {
                        try {
                            // We are just making sure the existence of the slice, so ignore
                            // slice loading state here.
                            returnSlice[0] = slice;
                            latch.countDown();
                        } catch (Exception e) {
                            Log.w(TAG, uri + " cannot be indexed", e);
                        } finally {
                            manager.unregisterSliceCallback(uri, this);
                        }
                    }
                };
        // Register a callback until we get a loaded slice.
        manager.registerSliceCallback(uri, callback);
        // Trigger the binding.
        callback.onSliceUpdated(manager.bindSlice(uri));
        try {
            latch.await(LATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error waiting for slice binding for uri" + uri, e);
            manager.unregisterSliceCallback(uri, callback);
        }
        return returnSlice[0];
    }

    @VisibleForTesting
    boolean isSliceToggleable(Slice slice) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final List<SliceAction> toggles = metadata.getToggles();

        return !toggles.isEmpty();
    }
}
