/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

abstract public class ZenModeSettingsBase extends RestrictedDashboardFragment {
    protected static final String TAG = "ZenModeSettings";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    protected Context mContext;
    protected int mZenMode;

    protected ZenModeBackend mBackend;

    protected void onZenModeConfigChanged() {};

    public ZenModeSettingsBase() {
        super(UserManager.DISALLOW_ADJUST_VOLUME);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mBackend = ZenModeBackend.getInstance(mContext);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        updateZenMode(false /*fireChanged*/);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateZenMode(true /*fireChanged*/);
        mSettingsObserver.register();
        if (isUiRestricted()) {
            if (isUiRestrictedByOnlyAdmin()) {
                getPreferenceScreen().removeAll();
                return;
            } else {
                finish();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.unregister();
    }

    private void updateZenMode(boolean fireChanged) {
        final int zenMode = Settings.Global.getInt(getContentResolver(), Global.ZEN_MODE, mZenMode);
        if (zenMode == mZenMode) return;
        mZenMode = zenMode;
        if (DEBUG) Log.d(TAG, "updateZenMode mZenMode=" + mZenMode + " " + fireChanged);
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE_URI = Global.getUriFor(Global.ZEN_MODE);
        private final Uri ZEN_MODE_CONFIG_ETAG_URI = Global.getUriFor(Global.ZEN_MODE_CONFIG_ETAG);

        private SettingsObserver() {
            super(mHandler);
        }

        public void register() {
            getContentResolver().registerContentObserver(ZEN_MODE_URI, false, this);
            getContentResolver().registerContentObserver(ZEN_MODE_CONFIG_ETAG_URI, false, this);
        }

        public void unregister() {
            getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (ZEN_MODE_URI.equals(uri)) {
                updateZenMode(true /*fireChanged*/);
            }
            if (ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                mBackend.updatePolicy();
                onZenModeConfigChanged();
            }
        }
    }

    void updatePreference(AbstractPreferenceController controller) {
        final PreferenceScreen screen = getPreferenceScreen();
        if (!controller.isAvailable()) {
            return;
        }
        final String key = controller.getPreferenceKey();

        final Preference preference = screen.findPreference(key);
        if (preference == null) {
            Log.d(TAG, String.format("Cannot find preference with key %s in Controller %s",
                    key, controller.getClass().getSimpleName()));
            return;
        }
        controller.updateState(preference);
    }
}
