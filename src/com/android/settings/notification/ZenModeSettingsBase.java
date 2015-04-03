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

import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;

import java.util.Objects;

abstract public class ZenModeSettingsBase extends SettingsPreferenceFragment {
    protected static final String TAG = "ZenModeSettings";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    protected Context mContext;
    protected ZenModeConfig mConfig;

    abstract protected void onZenModeChanged();
    abstract protected void updateControls();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();
        mConfig = getZenModeConfig();
        if (DEBUG) Log.d(TAG, "Loaded mConfig=" + mConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        mConfig = getZenModeConfig();
        updateControls();
        mSettingsObserver.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.unregister();
    }

    protected boolean setZenModeConfig(ZenModeConfig config) {
        final INotificationManager nm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            final boolean success = nm.setZenModeConfig(config);
            if (success) {
                mConfig = config;
                if (DEBUG) Log.d(TAG, "Saved mConfig=" + mConfig);
                updateControls();
            }
            return success;
        } catch (Exception e) {
           Log.w(TAG, "Error calling NoMan", e);
           return false;
        }
    }

    protected static boolean isDowntimeSupported(Context context) {
        return NotificationManager.from(context)
                .isSystemConditionProviderEnabled(ZenModeConfig.DOWNTIME_PATH);
    }

    private void updateZenModeConfig() {
        final ZenModeConfig config = getZenModeConfig();
        if (Objects.equals(config, mConfig)) return;
        mConfig = config;
        if (DEBUG) Log.d(TAG, "updateZenModeConfig mConfig=" + mConfig);
        updateControls();
    }

    private ZenModeConfig getZenModeConfig() {
        final INotificationManager nm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            return nm.getZenModeConfig();
        } catch (Exception e) {
           Log.w(TAG, "Error calling NoMan", e);
           return new ZenModeConfig();
        }
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
                onZenModeChanged();
            }
            if (ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                updateZenModeConfig();
            }
        }
    }
}
