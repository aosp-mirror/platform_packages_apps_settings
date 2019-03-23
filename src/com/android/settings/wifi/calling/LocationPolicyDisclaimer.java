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
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

/**
 * Disclaimer item class for displaying location privacy policy UI on
 * {@link WifiCallingDisclaimerFragment}.
 */
class LocationPolicyDisclaimer extends DisclaimerItem {
    private static final String DISCLAIMER_ITEM_NAME = "LocationPolicyDisclaimer";
    @VisibleForTesting
    static final String KEY_HAS_AGREED_LOCATION_DISCLAIMER
            = "key_has_agreed_location_disclaimer";

    LocationPolicyDisclaimer(Context context, int subId) {
        super(context, subId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean shouldShow() {
        PersistableBundle config = getCarrierConfig();
        if (!config.getBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL)) {
            logd("shouldShow: false due to carrier config is false.");
            return false;
        }

        if (config.getBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL)) {
            logd("shouldShow: false due to WFC is on as default.");
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
        return R.string.wfc_disclaimer_location_title_text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMessageId() {
        return R.string.wfc_disclaimer_location_desc_text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPrefKey() {
        return KEY_HAS_AGREED_LOCATION_DISCLAIMER;
    }
}
