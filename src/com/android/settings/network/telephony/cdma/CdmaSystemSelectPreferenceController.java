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

package com.android.settings.network.telephony.cdma;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "System Select"
 */
public class CdmaSystemSelectPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, ListPreference.OnPreferenceChangeListener {

    @VisibleForTesting
    ListPreference mPreference;
    private TelephonyManager mTelephonyManager;
    private PreferenceManager mPreferenceManager;
    private DataContentObserver mDataContentObserver;
    private int mSubId;

    public CdmaSystemSelectPreferenceController(Context context, String key) {
        super(context, key);
        mDataContentObserver = new DataContentObserver(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onStart() {
        mDataContentObserver.register(mContext, mSubId);
    }

    @Override
    public void onStop() {
        mDataContentObserver.unRegister(mContext);
    }

    @Override
    public int getAvailabilityStatus() {
        return MobileNetworkUtils.isCdmaOptions(mContext, mSubId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (ListPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setVisible(getAvailabilityStatus() == AVAILABLE);
        final int mode = mTelephonyManager.getCdmaRoamingMode();
        if (mode != TelephonyManager.CDMA_ROAMING_MODE_RADIO_DEFAULT) {
            if (mode == TelephonyManager.CDMA_ROAMING_MODE_HOME
                    || mode == TelephonyManager.CDMA_ROAMING_MODE_ANY) {
                listPreference.setValue(Integer.toString(mode));
            } else {
                resetCdmaRoamingModeToDefault();
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        int newMode = Integer.parseInt((String) object);
        //TODO(b/117611981): only set it in one place
        if (mTelephonyManager.setCdmaRoamingMode(newMode)) {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.CDMA_ROAMING_MODE, newMode);
            return true;
        }

        return false;
    }

    public void init(PreferenceManager preferenceManager, int subId) {
        mPreferenceManager = preferenceManager;
        mSubId = subId;
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);
    }

    public void showDialog() {
        if (!mTelephonyManager.getEmergencyCallbackMode()) {
            mPreferenceManager.showDialog(mPreference);
        }
    }

    private void resetCdmaRoamingModeToDefault() {
        //set the mButtonCdmaRoam
        mPreference.setValue(Integer.toString(TelephonyManager.CDMA_ROAMING_MODE_ANY));
        //set the Settings.System
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.CDMA_ROAMING_MODE,
                TelephonyManager.CDMA_ROAMING_MODE_ANY);
        //Set the Status
        mTelephonyManager.setCdmaRoamingMode(TelephonyManager.CDMA_ROAMING_MODE_ANY);
    }

    /**
     * Listener that listens mobile data state change.
     */
    public class DataContentObserver extends ContentObserver {

        public DataContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            Uri uri = Settings.Global.getUriFor(Settings.Global.PREFERRED_NETWORK_MODE + subId);
            context.getContentResolver().registerContentObserver(uri, false, this);
        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }
    }
}
