/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.material.button.MaterialButton;

/**
 * A preference for displaying the battery tips card view.
 */
public class BatteryTipsCardPreference extends Preference implements View.OnClickListener {

    private static final String TAG = "BatteryTipsCardPreference";

    private final PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    private MaterialButton mActionButton;
    private ImageButton mDismissButton;
    private ImageButton mThumbUpButton;
    private ImageButton mThumbDownButton;
    private CharSequence mTitle;
    private CharSequence mSummary;

    public BatteryTipsCardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.battery_tips_card);
        setSelectable(false);
        mPowerUsageFeatureProvider = FeatureFactory.getFeatureFactory()
            .getPowerUsageFeatureProvider();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        notifyChanged();
    }

    @Override
    public void setSummary(CharSequence summary) {
        mSummary = summary;
        notifyChanged();
    }

    @Override
    public void onClick(View view) {
        // TODO: replace with the settings anomaly obtained from detectSettingsAnomaly();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        ((TextView) view.findViewById(R.id.title)).setText(mTitle);
        ((TextView) view.findViewById(R.id.summary)).setText(mSummary);

        mActionButton = (MaterialButton) view.findViewById(R.id.action_button);
        mActionButton.setOnClickListener(this);
        mDismissButton = (ImageButton) view.findViewById(R.id.dismiss_button);
        mDismissButton.setOnClickListener(this);

        if (!mPowerUsageFeatureProvider.isBatteryTipsFeedbackEnabled()) {
            return;
        }
        view.findViewById(R.id.tips_card)
                .setBackgroundResource(R.drawable.battery_tips_half_rounded_top_bg);
        view.findViewById(R.id.feedback_card).setVisibility(View.VISIBLE);

        mThumbUpButton = (ImageButton) view.findViewById(R.id.thumb_up);
        mThumbUpButton.setOnClickListener(this);
        mThumbDownButton = (ImageButton) view.findViewById(R.id.thumb_down);
        mThumbDownButton.setOnClickListener(this);
    }
}
