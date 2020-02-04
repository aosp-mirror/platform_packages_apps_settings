/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Observes changes for dark night settings*/
public class DarkModeObserver {
    private static final String TAG = "DarkModeObserver";
    private ContentObserver mContentObserver;
    private final BroadcastReceiver mBatterySaverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mCallback.run();
        }
    };
    private Runnable mCallback;
    private Context mContext;

    public DarkModeObserver(Context context) {
        mContext = context;
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                final String setting = uri == null ? null : uri.getLastPathSegment();
                if (setting != null && mCallback != null) {
                    mCallback.run();
                }
            }
        };
    }

    /**
     * subscribe callback when night mode changed in the database
     *
     * @param callback the callback that gets triggered when subscribed
     */
    public void subscribe(Runnable callback) {
        callback.run();
        mCallback = callback;
        final Uri uri = Settings.Secure.getUriFor(Settings.Secure.UI_NIGHT_MODE);
        final Uri customStart =
                Settings.Secure.getUriFor(Settings.Secure.DARK_THEME_CUSTOM_START_TIME);
        final Uri customEnd =
                Settings.Secure.getUriFor(Settings.Secure.DARK_THEME_CUSTOM_END_TIME);
        mContext.getContentResolver()
                .registerContentObserver(uri, false, mContentObserver);
        mContext.getContentResolver()
                .registerContentObserver(customStart, false, mContentObserver);
        mContext.getContentResolver()
                .registerContentObserver(customEnd, false, mContentObserver);
        final IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        mContext.registerReceiver(
                mBatterySaverReceiver, batteryFilter);
    }

    /**
     * unsubscribe from dark ui database changes
     */
    public void unsubscribe() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        try {
            mContext.unregisterReceiver(mBatterySaverReceiver);
        } catch (IllegalArgumentException e) {
            /* Ignore: unregistering an unregistered receiver */
            Log.w(TAG, e.getMessage());
        }
        // NO-OP
        mCallback = null;
    }

    @VisibleForTesting
    protected void setContentObserver(ContentObserver co) {
        mContentObserver = co;
    }

    @VisibleForTesting
    protected ContentObserver getContentObserver() {
        return mContentObserver;
    }
}
