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

import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** Controls the update for battery tips card */
public class BatteryTipsController extends BasePreferenceController {

    private static final String TAG = "BatteryTipsController";
    private static final String ROOT_PREFERENCE_KEY = "battery_tips_category";
    private static final String CARD_PREFERENCE_KEY = "battery_tips_card";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    /** A callback listener for the battery tips is confirmed. */
    interface OnAnomalyConfirmListener {
        /** The callback function for the battery tips is confirmed. */
        void onAnomalyConfirm();
    }

    /** A callback listener for the battery tips is rejected. */
    interface OnAnomalyRejectListener {
        /** The callback function for the battery tips is rejected. */
        void onAnomalyReject();
    }

    private OnAnomalyConfirmListener mOnAnomalyConfirmListener;
    private OnAnomalyRejectListener mOnAnomalyRejectListener;

    @VisibleForTesting BatteryTipsCardPreference mCardPreference;
    @VisibleForTesting AnomalyEventWrapper mAnomalyEventWrapper = null;
    @VisibleForTesting Boolean mIsAcceptable = false;

    public BatteryTipsController(Context context) {
        super(context, ROOT_PREFERENCE_KEY);
        final FeatureFactory featureFactory = FeatureFactory.getFeatureFactory();
        mMetricsFeatureProvider = featureFactory.getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCardPreference = screen.findPreference(CARD_PREFERENCE_KEY);
    }

    void setOnAnomalyConfirmListener(OnAnomalyConfirmListener listener) {
        mOnAnomalyConfirmListener = listener;
    }

    void setOnAnomalyRejectListener(OnAnomalyRejectListener listener) {
        mOnAnomalyRejectListener = listener;
    }

    void acceptTipsCard() {
        if (mAnomalyEventWrapper == null || !mIsAcceptable) {
            return;
        }
        // For anomaly events with same record key, dismissed until next time full charged.
        final String dismissRecordKey = mAnomalyEventWrapper.getDismissRecordKey();
        if (!TextUtils.isEmpty(dismissRecordKey)) {
            DatabaseUtils.setDismissedPowerAnomalyKeys(mContext, dismissRecordKey);
        }
        mCardPreference.setVisible(false);
        mMetricsFeatureProvider.action(
                /* attribution= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                /* action= */ SettingsEnums.ACTION_BATTERY_TIPS_CARD_ACCEPT,
                /* pageId= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                /* key= */ mAnomalyEventWrapper.getEventId(),
                /* value= */ mAnomalyEventWrapper.getAnomalyKeyNumber());
    }

    void handleBatteryTipsCardUpdated(
            AnomalyEventWrapper anomalyEventWrapper, boolean isAcceptable) {
        mAnomalyEventWrapper = anomalyEventWrapper;
        mIsAcceptable = isAcceptable;
        if (mAnomalyEventWrapper == null) {
            mCardPreference.setVisible(false);
            return;
        }

        final String eventId = mAnomalyEventWrapper.getEventId();
        final int anomalyKeyNumber = mAnomalyEventWrapper.getAnomalyKeyNumber();

        // Update card & buttons preference
        if (!mAnomalyEventWrapper.updateTipsCardPreference(mCardPreference)) {
            mCardPreference.setVisible(false);
            return;
        }

        // Set battery tips card listener
        mCardPreference.setOnConfirmListener(
                () -> {
                    mCardPreference.setVisible(false);
                    if (mOnAnomalyConfirmListener != null) {
                        mOnAnomalyConfirmListener.onAnomalyConfirm();
                    } else if (mAnomalyEventWrapper.launchSubSetting()) {
                        mMetricsFeatureProvider.action(
                                /* attribution= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                                /* action= */ SettingsEnums.ACTION_BATTERY_TIPS_CARD_ACCEPT,
                                /* pageId= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                                /* key= */ eventId,
                                /* value= */ anomalyKeyNumber);
                    }
                });
        mCardPreference.setOnRejectListener(
                () -> {
                    mCardPreference.setVisible(false);
                    if (mOnAnomalyRejectListener != null) {
                        mOnAnomalyRejectListener.onAnomalyReject();
                    }
                    // For anomaly events with same record key, dismissed until next time full
                    // charged.
                    final String dismissRecordKey = mAnomalyEventWrapper.getDismissRecordKey();
                    if (!TextUtils.isEmpty(dismissRecordKey)) {
                        DatabaseUtils.setDismissedPowerAnomalyKeys(mContext, dismissRecordKey);
                    }
                    mMetricsFeatureProvider.action(
                            /* attribution= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                            /* action= */ SettingsEnums.ACTION_BATTERY_TIPS_CARD_DISMISS,
                            /* pageId= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                            /* key= */ eventId,
                            /* value= */ anomalyKeyNumber);
                });

        mCardPreference.setVisible(true);
        mMetricsFeatureProvider.action(
                /* attribution= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                /* action= */ SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW,
                /* pageId= */ SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                /* key= */ eventId,
                /* value= */ anomalyKeyNumber);
    }
}
