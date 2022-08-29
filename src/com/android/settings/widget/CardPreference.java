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
import android.view.View;
import android.widget.Button;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.google.android.material.card.MaterialCardView;

import java.util.Optional;

/**
 * Preference that wrapped by {@link MaterialCardView}, only support to set icon, title and summary
 */
public class CardPreference extends Preference {

    private View.OnClickListener mPrimaryBtnClickListener = null;
    private View.OnClickListener mSecondaryBtnClickListener = null;

    private String mPrimaryButtonText = null;
    private String mSecondaryButtonText = null;

    private Optional<Button> mPrimaryButton = Optional.empty();
    private Optional<Button> mSecondaryButton = Optional.empty();
    private Optional<View> mButtonsGroup = Optional.empty();

    private boolean mPrimaryButtonVisible = false;
    private boolean mSecondaryButtonVisible = false;

    public CardPreference(Context context) {
        this(context, null /* attrs */);
    }

    public CardPreference(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.cardPreferenceStyle);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        initButtonsAndLayout(holder);
    }

    private void initButtonsAndLayout(PreferenceViewHolder holder) {
        mPrimaryButton = Optional.ofNullable((Button) holder.findViewById(android.R.id.button1));
        mSecondaryButton = Optional.ofNullable((Button) holder.findViewById(android.R.id.button2));
        mButtonsGroup = Optional.ofNullable(holder.findViewById(R.id.card_preference_buttons));

        setPrimaryButtonText(mPrimaryButtonText);
        setPrimaryButtonClickListener(mPrimaryBtnClickListener);
        setPrimaryButtonVisible(mPrimaryButtonVisible);
        setSecondaryButtonText(mSecondaryButtonText);
        setSecondaryButtonClickListener(mSecondaryBtnClickListener);
        setSecondaryButtonVisible(mSecondaryButtonVisible);
    }

    /**
     * Register a callback to be invoked when the primary button is clicked.
     *
     * @param l the callback that will run
     */
    public void setPrimaryButtonClickListener(View.OnClickListener l) {
        mPrimaryButton.ifPresent(button -> button.setOnClickListener(l));
        mPrimaryBtnClickListener = l;
    }

    /**
     * Register a callback to be invoked when the secondary button is clicked.
     *
     * @param l the callback that will run
     */
    public void setSecondaryButtonClickListener(View.OnClickListener l) {
        mSecondaryButton.ifPresent(button -> button.setOnClickListener(l));
        mSecondaryBtnClickListener = l;
    }

    /**
     * Sets the text to be displayed on primary button.
     *
     * @param text text to be displayed
     */
    public void setPrimaryButtonText(String text) {
        mPrimaryButton.ifPresent(button -> button.setText(text));
        mPrimaryButtonText = text;
    }

    /**
     * Sets the text to be displayed on secondary button.
     *
     * @param text text to be displayed
     */
    public void setSecondaryButtonText(String text) {
        mSecondaryButton.ifPresent(button -> button.setText(text));
        mSecondaryButtonText = text;
    }

    /**
     * Set the visible on the primary button.
     *
     * @param visible {@code true} for visible
     */
    public void setPrimaryButtonVisible(boolean visible) {
        mPrimaryButton.ifPresent(
                button -> button.setVisibility(visible ? View.VISIBLE : View.GONE));
        mPrimaryButtonVisible = visible;
        updateButtonGroupsVisibility();
    }

    /**
     * Set the visible on the secondary button.
     *
     * @param visible {@code true} for visible
     */
    public void setSecondaryButtonVisible(boolean visible) {
        mSecondaryButton.ifPresent(
                button -> button.setVisibility(visible ? View.VISIBLE : View.GONE));
        mSecondaryButtonVisible = visible;
        updateButtonGroupsVisibility();
    }

    /**
     * Sets the text of content description on secondary button.
     *
     * @param text text for the content description
     */
    public void setSecondaryButtonContentDescription(String text) {
        mSecondaryButton.ifPresent(button -> button.setContentDescription(text));
    }

    private void updateButtonGroupsVisibility() {
        int visibility =
                (mPrimaryButtonVisible || mSecondaryButtonVisible) ? View.VISIBLE : View.GONE;
        mButtonsGroup.ifPresent(group -> group.setVisibility(visibility));
    }
}
