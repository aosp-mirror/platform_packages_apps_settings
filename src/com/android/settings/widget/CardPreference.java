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

package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.google.android.material.card.MaterialCardView;

/**
 * Preference that wrapped by {@link MaterialCardView}, only support to set icon, title and summary
 */
public class CardPreference extends Preference {

    private static final @ColorInt int INVALID_COLOR = -1;

    private @ColorInt int mCardBackgroundColor = INVALID_COLOR;

    public CardPreference(Context context) {
        this(context, null /* attrs */);
    }

    public CardPreference(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.cardPreferenceStyle);
    }

    /** Set card background color of the MaterialCardView in CardPreference. */
    public void setCardBackgroundColor(@ColorInt int color) {
        if (mCardBackgroundColor == color) {
            return;
        }
        mCardBackgroundColor = color;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        if (mCardBackgroundColor != INVALID_COLOR) {
            final MaterialCardView cardView = (MaterialCardView) view.findViewById(R.id.container);
            cardView.setCardBackgroundColor(mCardBackgroundColor);
        }
    }
}
