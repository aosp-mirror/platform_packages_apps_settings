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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.material.button.MaterialButton;

/**
 * A preference for displaying the battery tips card view.
 */
public class BatteryTipsCardPreference extends Preference implements View.OnClickListener {

    private static final String TAG = "BatteryTipsCardPreference";

    private final PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private String mAnomalyEventId;

    @VisibleForTesting
    CharSequence mMainButtonLabel;
    @VisibleForTesting
    CharSequence mDismissButtonLabel;
    @VisibleForTesting
    String mDestinationComponentName;
    @VisibleForTesting
    Integer mSourceMetricsCategory;

    public BatteryTipsCardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.battery_tips_card);
        setSelectable(false);
        final FeatureFactory featureFactory = FeatureFactory.getFeatureFactory();
        mPowerUsageFeatureProvider =  featureFactory.getPowerUsageFeatureProvider();
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
    }

    /**
     * Sets the anomaly event id which is used in metrics.
     */
    public void setAnomalyEventId(final String anomalyEventId) {
        mAnomalyEventId = anomalyEventId;
    }

    /**
     * Sets the label of main button in tips card.
     */
    public void setMainButtonLabel(CharSequence label) {
        if (!TextUtils.equals(mMainButtonLabel, label)) {
            mMainButtonLabel = label;
            notifyChanged();
        }
    }

    /**
     * Sets the label of dismiss button in tips card.
     */
    public void setDismissButtonLabel(CharSequence label) {
        if (!TextUtils.equals(mDismissButtonLabel, label)) {
            mDismissButtonLabel = label;
            notifyChanged();
        }
    }

    /**
     * Sets the info of target fragment launched by main button.
     */
    public void setMainButtonLauncherInfo(final String destinationClassName,
            final Integer sourceMetricsCategory) {
        mDestinationComponentName = destinationClassName;
        mSourceMetricsCategory = sourceMetricsCategory;
    }

    @Override
    public void onClick(View view) {
        final int viewId = view.getId();
        if (viewId == R.id.main_button || viewId == R.id.tips_card) {
            if (TextUtils.isEmpty(mDestinationComponentName)) {
                return;
            }
            new SubSettingLauncher(getContext())
                    .setDestination(mDestinationComponentName)
                    .setSourceMetricsCategory(mSourceMetricsCategory)
                    .launch();
            setVisible(false);
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_BATTERY_TIPS_CARD_ACCEPT, mAnomalyEventId);
        } else if (viewId == R.id.dismiss_button) {
            setVisible(false);
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_BATTERY_TIPS_CARD_DISMISS, mAnomalyEventId);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        ((TextView) view.findViewById(R.id.title)).setText(getTitle());

        LinearLayout tipsCard = (LinearLayout) view.findViewById(R.id.tips_card);
        tipsCard.setOnClickListener(this);
        MaterialButton mainButton = (MaterialButton) view.findViewById(R.id.main_button);
        mainButton.setOnClickListener(this);
        mainButton.setText(mMainButtonLabel);
        MaterialButton dismissButton = (MaterialButton) view.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(this);
        dismissButton.setText(mDismissButtonLabel);

        if (!mPowerUsageFeatureProvider.isBatteryTipsFeedbackEnabled()) {
            return;
        }
        view.findViewById(R.id.tips_card)
                .setBackgroundResource(R.drawable.battery_tips_half_rounded_top_bg);
        view.findViewById(R.id.feedback_card).setVisibility(View.VISIBLE);

        ImageButton thumbUpButton = (ImageButton) view.findViewById(R.id.thumb_up);
        thumbUpButton.setOnClickListener(this);
        ImageButton thumbDownButton = (ImageButton) view.findViewById(R.id.thumb_down);
        thumbDownButton.setOnClickListener(this);
    }
}
