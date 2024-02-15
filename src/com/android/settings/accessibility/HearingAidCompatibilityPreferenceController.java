/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/** Preference controller for Hearing Aid Compatibility (HAC) settings */
public class HearingAidCompatibilityPreferenceController extends TogglePreferenceController {

    // Hearing Aid Compatibility settings values
    static final String HAC_KEY = "HACSetting";
    static final String HAC_VAL_ON = "ON";
    static final String HAC_VAL_OFF = "OFF";
    @VisibleForTesting
    static final int HAC_DISABLED = 0;
    @VisibleForTesting
    static final int HAC_ENABLED = 1;

    private final TelephonyManager mTelephonyManager;
    private final AudioManager mAudioManager;

    public HearingAidCompatibilityPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.enforceTelephonyFeatureMappingForPublicApis()) {
            try {
                return mTelephonyManager.isHearingAidCompatibilitySupported() ? AVAILABLE
                        : UNSUPPORTED_ON_DEVICE;
            } catch (UnsupportedOperationException e) {
                // Device doesn't support FEATURE_TELEPHONY_CALLING
                return UNSUPPORTED_ON_DEVICE;
            }
        } else {
            return mTelephonyManager.isHearingAidCompatibilitySupported() ? AVAILABLE
                    : UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public boolean isChecked() {
        final int hac = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HEARING_AID, HAC_DISABLED);
        return hac == HAC_ENABLED;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        setAudioParameterHacEnabled(isChecked);
        return Settings.System.putInt(mContext.getContentResolver(), Settings.System.HEARING_AID,
                (isChecked ? HAC_ENABLED : HAC_DISABLED));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    private void setAudioParameterHacEnabled(boolean enabled) {
        mAudioManager.setParameters(HAC_KEY + "=" + (enabled ? HAC_VAL_ON : HAC_VAL_OFF) + ";");
    }
}
