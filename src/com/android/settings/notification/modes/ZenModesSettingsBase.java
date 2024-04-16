/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.util.Log;

import com.android.settings.dashboard.RestrictedDashboardFragment;

/**
 * Base class for all Settings pages controlling Modes behavior.
 */
abstract class ZenModesSettingsBase extends RestrictedDashboardFragment {
    protected static final String TAG = "ZenModesSettings";
    protected static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    protected Context mContext;

    protected ZenModesBackend mBackend;

    // Individual pages must implement this method based on what they should do when
    // the device's zen mode state changes.
    protected abstract void updateZenModeState();

    ZenModesSettingsBase() {
        super(UserManager.DISALLOW_ADJUST_VOLUME);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mBackend = ZenModesBackend.getInstance(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateZenModeState();
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
    public void onStop() {
        super.onStop();
        mSettingsObserver.unregister();
    }

    private final class SettingsObserver extends ContentObserver {
        private static final Uri ZEN_MODE_URI = Global.getUriFor(Global.ZEN_MODE);
        private static final Uri ZEN_MODE_CONFIG_ETAG_URI = Global.getUriFor(
                Global.ZEN_MODE_CONFIG_ETAG);

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
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            super.onChange(selfChange, uri);
            // Shouldn't have any other URIs trigger this method, but check just in case.
            if (ZEN_MODE_URI.equals(uri) || ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                updateZenModeState();
            }
        }
    }
}
