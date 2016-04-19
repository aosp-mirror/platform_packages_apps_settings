/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.annotation.DrawableRes;
import android.annotation.IdRes;
import android.annotation.StringRes;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.InstrumentedFragment;
import com.android.settings.R;

/**
 * Fragment for support tab in SettingsGoogle.
 */
public final class SupportFragment extends InstrumentedFragment {

    private View mContent;

    @Override
    protected int getMetricsCategory() {
        return SUPPORT_FRAGMENT;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContent = inflater.inflate(R.layout.support_fragment, container, false);
        // Update escalation items.
        updateEscalationCard(R.id.escalation_by_phone, R.string.support_escalation_by_phone);
        updateEscalationCard(R.id.escalation_by_email, R.string.support_escalation_by_email);
        // Update other support items.
        updateSupportTile(R.id.forum_tile, R.drawable.ic_forum_24dp, R.string.support_forum_title);
        updateSupportTile(R.id.article_tile, R.drawable.ic_help_24dp,
                R.string.support_articles_title);
        // Update feedback item.
        updateSupportTile(R.id.feedback_tile, R.drawable.ic_feedback_24dp,
                R.string.support_feedback_title);
        return mContent;
    }

    private void updateEscalationCard(@IdRes int cardId, @StringRes int title) {
        final View card = mContent.findViewById(cardId);
        ((TextView) card.findViewById(R.id.title)).setText(title);
    }

    private void updateSupportTile(@IdRes int tileId, @DrawableRes int icon, @StringRes int title) {
        final View tile = mContent.findViewById(tileId);
        ((ImageView) tile.findViewById(android.R.id.icon)).setImageResource(icon);
        ((TextView) tile.findViewById(android.R.id.title)).setText(title);
    }
}
