/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Global;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.SettingPref;

public class BatterySaverSettings extends SettingsPreferenceFragment {
    private static final String TAG = "BatterySaverSettings";
    private static final String KEY_ALWAYS_ON = "always_on";
    private static final String KEY_TURN_ON_AUTOMATICALLY = "turn_on_automatically";
    private static final long WAIT_FOR_SWITCH_ANIM = 500;

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);

    private Context mContext;
    private boolean mCreated;
    private SettingPref mAlwaysOnPref;
    private SettingPref mTriggerPref;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) return;
        mCreated = true;
        addPreferencesFromResource(R.xml.battery_saver_settings);

        mContext = getActivity();
        mAlwaysOnPref = new SettingPref(SettingPref.TYPE_GLOBAL, KEY_ALWAYS_ON,
                Global.LOW_POWER_MODE, 0) {
            @Override
            protected boolean setSetting(Context context, int value) {
                mHandler.removeCallbacks(mStartMode);
                if (value == 0) {
                    return super.setSetting(context, value);
                } else {
                    // about lose animations, make sure we don't turn the mode on until the switch
                    // stops moving
                    mHandler.postDelayed(mStartMode, WAIT_FOR_SWITCH_ANIM);
                    return true;
                }
            }
        };
        mTriggerPref = new SettingPref(SettingPref.TYPE_GLOBAL, KEY_TURN_ON_AUTOMATICALLY,
                Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_lowBatteryWarningLevel),
                getResources().getIntArray(R.array.battery_saver_trigger_values)) {
            @Override
            protected String getCaption(Resources res, int value) {
                if (value > 0 && value < 100) {
                    return res.getString(R.string.battery_saver_turn_on_automatically_pct, value);
                }
                return res.getString(R.string.battery_saver_turn_on_automatically_never);
            }
        };
        mAlwaysOnPref.init(this);
        mTriggerPref.init(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsObserver.setListening(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.setListening(false);
    }

    private final Runnable mStartMode = new Runnable() {
        @Override
        public void run() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Starting LOW_POWER_MODE from settings");
                    Global.putInt(mContext.getContentResolver(), Global.LOW_POWER_MODE, 1);
                }
            });
        }
    };

    private final class SettingsObserver extends ContentObserver {
        private final Uri LOW_POWER_MODE_URI = Global.getUriFor(Global.LOW_POWER_MODE);
        private final Uri LOW_POWER_MODE_TRIGGER_LEVEL_URI
                = Global.getUriFor(Global.LOW_POWER_MODE_TRIGGER_LEVEL);

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (LOW_POWER_MODE_URI.equals(uri)) {
                mAlwaysOnPref.update(mContext);
            }
            if (LOW_POWER_MODE_TRIGGER_LEVEL_URI.equals(uri)) {
                mTriggerPref.update(mContext);
            }
        }

        public void setListening(boolean listening) {
            final ContentResolver cr = getContentResolver();
            if (listening) {
                cr.registerContentObserver(LOW_POWER_MODE_URI, false, this);
                cr.registerContentObserver(LOW_POWER_MODE_TRIGGER_LEVEL_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }
    }
}
