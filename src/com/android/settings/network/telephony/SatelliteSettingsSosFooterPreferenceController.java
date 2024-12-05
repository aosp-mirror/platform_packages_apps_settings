/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

/** Footer for Satellite SOS. */
public class SatelliteSettingsSosFooterPreferenceController extends BasePreferenceController {
    private static final String TAG = "SatelliteSettingsSosFooterPrefCtrl";

    public SatelliteSettingsSosFooterPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null || !preference.getKey().equals(getPreferenceKey())) {
            Log.d(TAG, "Wrong key for footer");
            return;
        }
        FooterPreference footerPreference = (FooterPreference) preference;
        footerPreference.setLearnMoreAction(v -> openSatelliteSosLearnMoreLink());
    }

    private void openSatelliteSosLearnMoreLink() {
        mContext.startActivity(
                HelpUtils.getHelpIntent(
                        mContext,
                        mContext.getString(R.string.satellite_sos_learn_more_link),
                        /*backupContext=*/""));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
