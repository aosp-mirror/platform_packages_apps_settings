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
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceViewManager;
import androidx.slice.core.SliceAction;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;
import java.util.concurrent.Callable;

public class EligibleCardChecker implements Callable<ContextualCard> {

    private static final String TAG = "EligibleCardChecker";

    private final Context mContext;

    @VisibleForTesting
    ContextualCard mCard;

    EligibleCardChecker(Context context, ContextualCard card) {
        mContext = context;
        mCard = card;
    }

    @Override
    public ContextualCard call() {
        final long startTime = System.currentTimeMillis();
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
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

        final Uri uri = card.getSliceUri();
        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return false;
        }

        final Slice slice = bindSlice(uri);

        if (slice == null || slice.hasHint(HINT_ERROR)) {
            Log.w(TAG, "Failed to bind slice, not eligible for display " + uri);
            return false;
        }

        mCard = card.mutate().setSlice(slice).build();

        if (isSliceToggleable(slice)) {
            mCard = card.mutate().setHasInlineAction(true).build();
        }

        return true;
    }

    @VisibleForTesting
    Slice bindSlice(Uri uri) {
        final SliceViewManager manager = SliceViewManager.getInstance(mContext);
        final SliceViewManager.SliceCallback callback = slice -> { };

        // Register a trivial callback to pin the slice
        manager.registerSliceCallback(uri, callback);
        final Slice slice = manager.bindSlice(uri);

        // Workaround of unpinning slice in the same SerialExecutor of AsyncTask as SliceCallback's
        // observer.
        ThreadUtils.postOnMainThread(() -> AsyncTask.execute(() -> {
            try {
                manager.unregisterSliceCallback(uri, callback);
            } catch (SecurityException e) {
                Log.d(TAG, "No permission currently: " + e);
            }
        }));

        return slice;
    }

    @VisibleForTesting
    boolean isSliceToggleable(Slice slice) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final List<SliceAction> toggles = metadata.getToggles();

        return !toggles.isEmpty();
    }
}
