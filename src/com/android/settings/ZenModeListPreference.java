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

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;

public class ZenModeListPreference extends ListPreference {
    private static final String TAG = "ZenModeListPreference";
    private static final boolean DEBUG = false;

    private final Handler mHandler = new Handler();
    private final ContentResolver mResolver;

    public ZenModeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DEBUG) Log.d(TAG, "new ZenModeListPreference()");
        mResolver = context.getContentResolver();
    }

    public void init() {
        if (DEBUG) Log.d(TAG, "init");
        loadZenModeSetting("init");
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (DEBUG) Log.d(TAG, "onPreferenceChange " + newValue);
                final boolean updateWithNewValue = saveZenModeSetting((String)newValue);
                return updateWithNewValue;
            }
        });
        mResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ZEN_MODE),
                false, new SettingsObserver());
    }

    private void loadZenModeSetting(String reason) {
        if (DEBUG) Log.d(TAG, "loadZenModeSetting " + reason);
        setValue(Integer.toString(Settings.Global.getInt(mResolver,
                Settings.Global.ZEN_MODE, Settings.Global.ZEN_MODE_OFF)));
    }

    private boolean saveZenModeSetting(String value) {
        if (DEBUG) Log.d(TAG, "saveZenModeSetting " + value);
        try {
            final int v = Integer.valueOf(value);
            checkZenMode(v);
            return Settings.Global.putInt(mResolver, Settings.Global.ZEN_MODE, v);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to update zen mode with value: " + value, t);
            return false;
        }
    }

    private static void checkZenMode(int mode) {
        if (mode < Settings.Global.ZEN_MODE_OFF || mode > Settings.Global.ZEN_MODE_FULL) {
            throw new IllegalArgumentException("Invalid zen mode: " + mode);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            loadZenModeSetting("change");
        }
    }
}
