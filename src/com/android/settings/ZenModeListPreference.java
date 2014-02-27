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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ZenModeListPreference extends ListPreference {
    private static final String TAG = "ZenModeListPreference";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final Handler mHandler = new Handler();
    private final ContentResolver mResolver;

    private ImageView mConfigure;
    private int mMode;

    public ZenModeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DEBUG) Log.d(TAG, "new ZenModeListPreference()");
        mContext = context;
        mResolver = context.getContentResolver();
        setWidgetLayoutResource(R.layout.preference_zen_mode);
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

    @Override
    protected void onBindView(View view) {
        if (DEBUG) Log.d(TAG, "onBindView");
        super.onBindView(view);
        mConfigure = (ImageView)view.findViewById(R.id.configure_zen_mode);
        updateConfigureVisibility();
        mConfigure.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mMode != Settings.Global.ZEN_MODE_LIMITED) return;
                if (mContext instanceof SettingsActivity) {
                    SettingsActivity sa = (SettingsActivity)mContext;
                    sa.startPreferencePanel(ZenModeSettings.class.getName(),
                            null, R.string.zen_mode_settings_title, null, null, 0);
                }
            }
        });
    }

    private void updateConfigureVisibility() {
        if (mConfigure != null) {
            final boolean limited = mMode == Settings.Global.ZEN_MODE_LIMITED;
            mConfigure.setVisibility(limited ? View.VISIBLE : View.GONE);
        }
    }

    private void loadZenModeSetting(String reason) {
        if (DEBUG) Log.d(TAG, "loadZenModeSetting " + reason);
        mMode = Settings.Global.getInt(mResolver,
                Settings.Global.ZEN_MODE, Settings.Global.ZEN_MODE_OFF);
        setValue(Integer.toString(mMode));
        updateConfigureVisibility();
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
