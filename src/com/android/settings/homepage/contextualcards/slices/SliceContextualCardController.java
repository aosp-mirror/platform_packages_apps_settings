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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardController;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider;
import com.android.settings.homepage.contextualcards.ContextualCardFeedbackDialog;
import com.android.settings.homepage.contextualcards.ContextualCardUpdateListener;
import com.android.settings.homepage.contextualcards.logging.ContextualCardLogUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

/**
 * Card controller for {@link ContextualCard} built as slices.
 */
public class SliceContextualCardController implements ContextualCardController {

    private static final String TAG = "SliceCardController";

    private final Context mContext;

    private ContextualCardUpdateListener mCardUpdateListener;

    public SliceContextualCardController(Context context) {
        mContext = context;
    }

    @Override
    public int getCardType() {
        return ContextualCard.CardType.SLICE;
    }

    @Override
    public void onPrimaryClick(ContextualCard card) {

    }

    @Override
    public void onActionClick(ContextualCard card) {

    }

    @Override
    public void onDismissed(ContextualCard card) {
        ThreadUtils.postOnBackgroundThread(() -> {
            final ContextualCardFeatureProvider cardFeatureProvider =
                    FeatureFactory.getFeatureFactory().getContextualCardFeatureProvider(mContext);
            cardFeatureProvider.markCardAsDismissed(mContext, card.getName());
        });
        showFeedbackDialog(card);

        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

        metricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_CONTEXTUAL_CARD_DISMISS,
                ContextualCardLogUtils.buildCardDismissLog(card));
    }

    @Override
    public void setCardUpdateListener(ContextualCardUpdateListener listener) {
        mCardUpdateListener = listener;
    }

    @VisibleForTesting
    void showFeedbackDialog(ContextualCard card) {
        final String email = mContext.getString(R.string.config_contextual_card_feedback_email);
        if (!isFeedbackEnabled(email)) {
            return;
        }
        final Intent feedbackIntent = new Intent(mContext, ContextualCardFeedbackDialog.class);
        feedbackIntent.putExtra(ContextualCardFeedbackDialog.EXTRA_CARD_NAME,
                getSimpleCardName(card));
        feedbackIntent.putExtra(ContextualCardFeedbackDialog.EXTRA_FEEDBACK_EMAIL, email);
        feedbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(feedbackIntent);
    }

    @VisibleForTesting
    boolean isFeedbackEnabled(String email) {
        return !TextUtils.isEmpty(email) && Build.IS_DEBUGGABLE;
    }

    private String getSimpleCardName(ContextualCard card) {
        final String[] split = card.getName().split("/");
        return split[split.length - 1];
    }
}
