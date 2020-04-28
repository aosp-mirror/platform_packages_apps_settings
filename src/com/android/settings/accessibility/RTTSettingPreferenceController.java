/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.accessibility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

/** A controller to control the status for RTT setting in Accessibility screen.*/
public class RTTSettingPreferenceController extends BasePreferenceController {

    private static final String DIALER_RTT_CONFIGURATION = "dialer_rtt_configuration";

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final TelecomManager mTelecomManager;
    private final CharSequence[] mModes;
    private final String mDialerPackage;

    @VisibleForTesting
    Intent mRTTIntent;

    public RTTSettingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mModes = mContext.getResources().getTextArray(R.array.rtt_setting_mode);
        mDialerPackage = mContext.getString(R.string.config_rtt_setting_package_name);
        mPackageManager = context.getPackageManager();
        mTelecomManager = context.getSystemService(TelecomManager.class);
        mRTTIntent = new Intent(context.getString(R.string.config_rtt_setting_intent_action));
    }

    @Override
    public int getAvailabilityStatus() {
        final List<ResolveInfo> resolved =
                mPackageManager.queryIntentActivities(mRTTIntent, 0 /* flags */);
        return resolved != null && !resolved.isEmpty() && isDialerSupportRTTSetting()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(getPreferenceKey());
        pref.setIntent(mRTTIntent);
    }

    @Override
    public CharSequence getSummary() {
        final int option = Settings.Secure.getInt(mContext.getContentResolver(),
                DIALER_RTT_CONFIGURATION, 1 /* not visible */);
        return mModes[option];
    }

    @VisibleForTesting
    boolean isDialerSupportRTTSetting() {
        return TextUtils.equals(mTelecomManager.getDefaultDialerPackage(), mDialerPackage);
    }
}
