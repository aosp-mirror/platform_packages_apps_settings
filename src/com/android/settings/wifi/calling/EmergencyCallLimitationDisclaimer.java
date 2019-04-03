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

package com.android.settings.wifi.calling;

import android.content.Context;
import android.telephony.CarrierConfigManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

/**
 * Disclaimer item class for displaying emergency call limitation UI on
 * {@link WifiCallingDisclaimerFragment}.
 */
public class EmergencyCallLimitationDisclaimer extends DisclaimerItem {
    private static final String DISCLAIMER_ITEM_NAME = "EmergencyCallLimitationDisclaimer";
    @VisibleForTesting
    static final String KEY_HAS_AGREED_EMERGENCY_LIMITATION_DISCLAIMER =
            "key_has_agreed_emergency_limitation_disclaimer";
    private static final int UNINITIALIZED_DELAY_VALUE = -1;

    public EmergencyCallLimitationDisclaimer(Context context, int subId) {
        super(context, subId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean shouldShow() {
        final int notificationDelay = getCarrierConfig().getInt(
                CarrierConfigManager.KEY_EMERGENCY_NOTIFICATION_DELAY_INT);
        if (notificationDelay == UNINITIALIZED_DELAY_VALUE) {
            logd("shouldShow: false due to carrier config is default(-1).");
            return false;
        }

        return super.shouldShow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getName() {
        return DISCLAIMER_ITEM_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getTitleId() {
        return R.string.wfc_disclaimer_emergency_limitation_title_text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMessageId() {
        return R.string.wfc_disclaimer_emergency_limitation_desc_text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPrefKey() {
        return KEY_HAS_AGREED_EMERGENCY_LIMITATION_DISCLAIMER;
    }
}
