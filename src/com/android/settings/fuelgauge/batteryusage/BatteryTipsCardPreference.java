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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.material.button.MaterialButton;

/**
 * A preference for displaying the battery tips card view.
 */
public class BatteryTipsCardPreference extends Preference implements View.OnClickListener {

    private static final String TAG = "BatteryTipsCardPreference";

    interface OnConfirmListener {
        void onConfirm();
    }

    interface OnRejectListener {
        void onReject();
    }

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private OnConfirmListener mOnConfirmListener;
    private OnRejectListener mOnRejectListener;
    private int mIconResourceId = 0;
    private int mMainButtonStrokeColorResourceId = 0;

    @VisibleForTesting
    CharSequence mMainButtonLabel;
    @VisibleForTesting
    CharSequence mDismissButtonLabel;

    public BatteryTipsCardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.battery_tips_card);
        setSelectable(false);
        final FeatureFactory featureFactory = FeatureFactory.getFeatureFactory();
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        mOnConfirmListener = listener;
    }

    public void setOnRejectListener(OnRejectListener listener) {
        mOnRejectListener = listener;
    }

    /**
     * Sets the icon in tips card.
     */
    public void setIconResourceId(int resourceId) {
        if (mIconResourceId != resourceId) {
            mIconResourceId = resourceId;
            notifyChanged();
        }
    }

    /**
     * Sets the stroke color of main button in tips card.
     */
    public void setMainButtonStrokeColorResourceId(int resourceId) {
        if (mMainButtonStrokeColorResourceId != resourceId) {
            mMainButtonStrokeColorResourceId = resourceId;
            notifyChanged();
        }
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

    @Override
    public void onClick(View view) {
        final int viewId = view.getId();
        if (viewId == R.id.main_button || viewId == R.id.tips_card) {
            if (mOnConfirmListener != null) {
                mOnConfirmListener.onConfirm();
            }
        } else if (viewId == R.id.dismiss_button) {
            if (mOnRejectListener != null) {
                mOnRejectListener.onReject();
            }
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
        if (mMainButtonStrokeColorResourceId != 0) {
            mainButton.setStrokeColorResource(mMainButtonStrokeColorResourceId);
        }
        MaterialButton dismissButton = (MaterialButton) view.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(this);
        dismissButton.setText(mDismissButtonLabel);
        if (mIconResourceId != 0) {
            ((ImageView) view.findViewById(R.id.icon)).setImageResource(mIconResourceId);
        }
    }
}
