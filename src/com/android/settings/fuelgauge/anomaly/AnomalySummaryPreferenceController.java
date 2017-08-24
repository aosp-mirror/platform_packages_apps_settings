/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageAnomalyDetails;

import java.util.List;

/**
 * Manager that responsible for updating high usage preference and handling preference click.
 */
public class AnomalySummaryPreferenceController {
    private static final String TAG = "HighUsagePreferenceController";

    public static final String ANOMALY_KEY = "high_usage";

    private static final int REQUEST_ANOMALY_ACTION = 0;
    private PreferenceFragment mFragment;
    @VisibleForTesting
    Preference mAnomalyPreference;
    @VisibleForTesting
    List<Anomaly> mAnomalies;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private SettingsActivity mSettingsActivity;

    /**
     * Metrics key about fragment that create this controller
     *
     * @see com.android.internal.logging.nano.MetricsProto.MetricsEvent
     */
    private int mMetricsKey;

    public AnomalySummaryPreferenceController(SettingsActivity activity,
            PreferenceFragment fragment, int metricsKey) {
        mFragment = fragment;
        mSettingsActivity = activity;
        mAnomalyPreference = mFragment.getPreferenceScreen().findPreference(ANOMALY_KEY);
        mMetricsKey = metricsKey;
        mBatteryUtils = BatteryUtils.getInstance(activity.getApplicationContext());
        hideHighUsagePreference();
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        if (mAnomalies != null && ANOMALY_KEY.equals(preference.getKey())) {
            if (mAnomalies.size() == 1) {
                final Anomaly anomaly = mAnomalies.get(0);
                AnomalyDialogFragment dialogFragment = AnomalyDialogFragment.newInstance(anomaly,
                        mMetricsKey);
                dialogFragment.setTargetFragment(mFragment, REQUEST_ANOMALY_ACTION);
                dialogFragment.show(mFragment.getFragmentManager(), TAG);
            } else {
                PowerUsageAnomalyDetails.startBatteryAbnormalPage(mSettingsActivity, mFragment,
                        mAnomalies);
            }
            return true;
        }
        return false;
    }

    /**
     * Update anomaly preference based on {@code anomalies}, also store a reference
     * of {@paramref anomalies}, which would be used in {@link #onPreferenceTreeClick(Preference)}
     *
     * @param anomalies used to update the summary, this method will store a reference of it
     */
    public void updateAnomalySummaryPreference(List<Anomaly> anomalies) {
        final Context context = mFragment.getContext();
        mAnomalies = anomalies;

        if (!mAnomalies.isEmpty()) {
            mAnomalyPreference.setVisible(true);
            final int count = mAnomalies.size();
            final String title = context.getResources().getQuantityString(
                    R.plurals.power_high_usage_title, count, mAnomalies.get(0).displayName);
            final String summary = count > 1 ?
                    context.getString(R.string.battery_abnormal_apps_summary, count)
                    : context.getString(
                            mBatteryUtils.getSummaryResIdFromAnomalyType(mAnomalies.get(0).type));

            mAnomalyPreference.setTitle(title);
            mAnomalyPreference.setSummary(summary);
        } else {
            mAnomalyPreference.setVisible(false);
        }
    }

    public void hideHighUsagePreference() {
        mAnomalyPreference.setVisible(false);
    }
}
