/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.content.Context.CARRIER_CONFIG_SERVICE;

public class SystemUpdatePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String TAG = "SysUpdatePrefContr";

    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";

    private final UserManager mUm;

    public SystemUpdatePreferenceController(Context context, UserManager um) {
        super(context);
        mUm = um;
    }

    @Override
    public boolean isAvailable() {
        return mUm.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SYSTEM_UPDATE_SETTINGS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (isAvailable()) {
            Utils.updatePreferenceToSpecificActivityOrRemove(mContext, screen,
                    KEY_SYSTEM_UPDATE_SETTINGS,
                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        } else {
            removePreference(screen, KEY_SYSTEM_UPDATE_SETTINGS);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_SYSTEM_UPDATE_SETTINGS.equals(preference.getKey())) {
            CarrierConfigManager configManager =
                    (CarrierConfigManager) mContext.getSystemService(CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configManager.getConfig();
            if (b != null && b.getBoolean(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL)) {
                ciActionOnSysUpdate(b);
            }
        }
        // always return false here because this handler does not want to block other handlers.
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(mContext.getString(R.string.about_summary,
                Build.VERSION.RELEASE));
    }

    /**
     * Trigger client initiated action (send intent) on system update
     */
    private void ciActionOnSysUpdate(PersistableBundle b) {
        String intentStr = b.getString(CarrierConfigManager.
                KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING);
        if (!TextUtils.isEmpty(intentStr)) {
            String extra = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_STRING);
            String extraVal = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_VAL_STRING);

            Intent intent = new Intent(intentStr);
            if (!TextUtils.isEmpty(extra)) {
                intent.putExtra(extra, extraVal);
            }
            Log.d(TAG, "ciActionOnSysUpdate: broadcasting intent " + intentStr +
                    " with extra " + extra + ", " + extraVal);
            mContext.getApplicationContext().sendBroadcast(intent);
        }
    }
}
