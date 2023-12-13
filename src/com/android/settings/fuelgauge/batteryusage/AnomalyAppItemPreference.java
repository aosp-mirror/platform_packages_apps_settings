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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

class AnomalyAppItemPreference extends PowerGaugePreference {

    private static final String TAG = "AnomalyAppItemPreference";

    private CharSequence mAnomalyHintText;

    AnomalyAppItemPreference(Context context) {
        super(context, /* attrs */ null);
        setLayoutResource(R.layout.anomaly_app_item_preference);
    }

    void setAnomalyHint(CharSequence anomalyHintText) {
        if (!TextUtils.equals(mAnomalyHintText, anomalyHintText)) {
            mAnomalyHintText = anomalyHintText;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder viewHolder) {
        super.onBindViewHolder(viewHolder);
        final LinearLayout warningChipView =
                (LinearLayout) viewHolder.findViewById(R.id.warning_chip);

        if (!TextUtils.isEmpty(mAnomalyHintText)) {
            ((TextView) warningChipView.findViewById(R.id.warning_info)).setText(mAnomalyHintText);
            warningChipView.setVisibility(View.VISIBLE);
        } else {
            warningChipView.setVisibility(View.GONE);
        }
    }
}
