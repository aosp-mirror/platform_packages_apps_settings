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

package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.util.Log;

import com.android.settings.RestrictedSettingsFragment;

import java.util.Objects;

abstract public class ZenModeSettingsBase extends RestrictedSettingsFragment {
    protected static final String TAG = "ZenModeSettings";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    protected Context mContext;
    protected ZenModeConfig mConfig;
    protected int mZenMode;

    abstract protected void onZenModeChanged();
    abstract protected void onZenModeConfigChanged();

    public ZenModeSettingsBase() {
        super(UserManager.DISALLOW_ADJUST_VOLUME);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();
        updateZenMode(false /*fireChanged*/);
        updateZenModeConfig(false /*fireChanged*/);
        if (DEBUG) Log.d(TAG, "Loaded mConfig=" + mConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateZenMode(true /*fireChanged*/);
        updateZenModeConfig(true /*fireChanged*/);
        mSettingsObserver.register();
        if (isUiRestricted()) {
            finish();
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
        if (DEBUG) Log.d(TAG, "updateZenMode mZenMode=" + mZenMode);
        if (fireChanged) {
            onZenModeChanged();
        }
    }

    private void updateZenModeConfig(boolean fireChanged) {
        final ZenModeConfig config = getZenModeConfig();
        if (Objects.equals(config, mConfig)) return;
        mConfig = config;
        if (DEBUG) Log.d(TAG, "updateZenModeConfig mConfig=" + mConfig);
        if (fireChanged) {
            onZenModeConfigChanged();
        }
    }

    protected boolean setZenModeConfig(ZenModeConfig config) {
        final String reason = getClass().getSimpleName();
        final boolean success = NotificationManager.from(mContext).setZenModeConfig(config, reason);
        if (success) {
            mConfig = config;
            if (DEBUG) Log.d(TAG, "Saved mConfig=" + mConfig);
            onZenModeConfigChanged();
        }
        return success;
    }

    protected void setZenMode(int zenMode, Uri conditionId) {
        NotificationManager.from(mContext).setZenMode(zenMode, conditionId, TAG);
    }

    protected static boolean isScheduleSupported(Context context) {
        return NotificationManager.from(context)
                .isSystemConditionProviderEnabled(ZenModeConfig.SCHEDULE_PATH);
    }

    private ZenModeConfig getZenModeConfig() {
        return NotificationManager.from(mContext).getZenModeConfig();
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
                updateZenModeConfig(true /*fireChanged*/);
            }
        }
    }
}
