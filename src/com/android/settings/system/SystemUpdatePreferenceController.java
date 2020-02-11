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
package com.android.settings.system;

import static android.content.Context.CARRIER_CONFIG_SERVICE;
import static android.content.Context.SYSTEM_UPDATE_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemUpdateManager;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class SystemUpdatePreferenceController extends BasePreferenceController {

    private static final String TAG = "SysUpdatePrefContr";

    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";

    private final UserManager mUm;
    private final SystemUpdateManager mUpdateManager;

    public SystemUpdatePreferenceController(Context context) {
        super(context, KEY_SYSTEM_UPDATE_SETTINGS);
        mUm = UserManager.get(context);
        mUpdateManager = (SystemUpdateManager) context.getSystemService(SYSTEM_UPDATE_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_system_update_settings)
                && mUm.isAdminUser()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            Utils.updatePreferenceToSpecificActivityOrRemove(mContext, screen,
                    getPreferenceKey(),
                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
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
    public CharSequence getSummary() {
        CharSequence summary = mContext.getString(R.string.android_version_summary,
                Build.VERSION.RELEASE_OR_CODENAME);
        final FutureTask<Bundle> bundleFutureTask = new FutureTask<>(
                // Put the API call in a future to avoid StrictMode violation.
                () -> mUpdateManager.retrieveSystemUpdateInfo());
        final Bundle updateInfo;
        try {
            bundleFutureTask.run();
            updateInfo = bundleFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting system update info.");
            return summary;
        }
        switch (updateInfo.getInt(SystemUpdateManager.KEY_STATUS)) {
            case SystemUpdateManager.STATUS_WAITING_DOWNLOAD:
            case SystemUpdateManager.STATUS_IN_PROGRESS:
            case SystemUpdateManager.STATUS_WAITING_INSTALL:
            case SystemUpdateManager.STATUS_WAITING_REBOOT:
                summary = mContext.getText(R.string.android_version_pending_update_summary);
                break;
            case SystemUpdateManager.STATUS_UNKNOWN:
                Log.d(TAG, "Update statue unknown");
                // fall through to next branch
            case SystemUpdateManager.STATUS_IDLE:
                final String version = updateInfo.getString(SystemUpdateManager.KEY_TITLE);
                if (!TextUtils.isEmpty(version)) {
                    summary = mContext.getString(R.string.android_version_summary, version);
                }
                break;
        }
        return summary;
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
            intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
            mContext.getApplicationContext().sendBroadcast(intent);
        }
    }
}
