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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.Nullable;

class ZenSettingsObserver extends ContentObserver {
    private static final Uri ZEN_MODE_URI = Settings.Global.getUriFor(Settings.Global.ZEN_MODE);
    private static final Uri ZEN_MODE_CONFIG_ETAG_URI = Settings.Global.getUriFor(
            Settings.Global.ZEN_MODE_CONFIG_ETAG);

    private final Context mContext;
    @Nullable private Runnable mCallback;

    ZenSettingsObserver(Context context) {
        this(context, null);
    }

    ZenSettingsObserver(Context context, @Nullable Runnable callback) {
        super(context.getMainExecutor(), 0);
        mContext = context;
        setOnChangeListener(callback);
    }

    void register() {
        mContext.getContentResolver().registerContentObserver(ZEN_MODE_URI, false, this);
        mContext.getContentResolver().registerContentObserver(ZEN_MODE_CONFIG_ETAG_URI, false,
                this);
    }

    void unregister() {
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    void setOnChangeListener(@Nullable Runnable callback) {
        mCallback = callback;
    }

    @Override
    public void onChange(boolean selfChange, @Nullable Uri uri) {
        super.onChange(selfChange, uri);
        // Shouldn't have any other URIs trigger this method, but check just in case.
        if (ZEN_MODE_URI.equals(uri) || ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
            if (mCallback != null) {
                mCallback.run();
            }
        }
    }
}
