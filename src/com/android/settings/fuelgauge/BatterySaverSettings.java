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
import android.os.Message;
import android.provider.Settings.Global;
import android.util.Log;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.SettingPref;
import com.android.settings.widget.SwitchBar;

public class BatterySaverSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener {
    private static final String TAG = "BatterySaverSettings";
    private static final String KEY_TURN_ON_AUTOMATICALLY = "turn_on_automatically";
    private static final long WAIT_FOR_SWITCH_ANIM = 500;

    private final H mHandler = new H();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);

    private Context mContext;
    private SwitchBar mSwitchBar;
    private boolean mSwitchBarListenerAdded;
    private SettingPref mTriggerPref;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.battery_saver_settings);

        mContext = getActivity();
        mSwitchBar = ((SettingsActivity) mContext).getSwitchBar();
        updateSwitchBar();
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
        mTriggerPref.init(this);
    }

    private void updateSwitchBar() {
        final boolean mode = Global.getInt(getContentResolver(), Global.LOW_POWER_MODE, 0) != 0;
        if (mSwitchBarListenerAdded) {
            mSwitchBar.removeOnSwitchChangeListener(this);
        }
        mSwitchBar.getSwitch().setChecked(mode);
        if (mSwitchBarListenerAdded) {
            mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    private void updateTriggerLevel() {
        mTriggerPref.update(mContext);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchBar();
        mSettingsObserver.setListening(true);
        mSwitchBar.show();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBarListenerAdded = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.setListening(false);
        if (mSwitchBarListenerAdded) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mSwitchBarListenerAdded = false;
        }
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mHandler.postSetMode(isChecked);
    }

    private void handleSetMode(final boolean mode) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "LOW_POWER_MODE from settings: " + mode);
                Global.putInt(getContentResolver(), Global.LOW_POWER_MODE, mode ? 1 : 0);
            }
        });
    }

    private final class H extends Handler {
        private static final int SET_MODE = 1;

        public void postSetMode(boolean mode) {
            removeMessages(SET_MODE);
            final Message m = obtainMessage(SET_MODE, mode ? 1 : 0, 0);
            sendMessageDelayed(m, mode ? WAIT_FOR_SWITCH_ANIM : 0);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SET_MODE) {
                handleSetMode(msg.arg1 != 0);
            }
        }
    }

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
                updateSwitchBar();
            }
            if (LOW_POWER_MODE_TRIGGER_LEVEL_URI.equals(uri)) {
                updateTriggerLevel();
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
