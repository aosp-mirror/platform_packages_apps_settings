/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class TimeChangeListenerMixin extends BroadcastReceiver
        implements LifecycleObserver, OnPause, OnResume {

    private final Context mContext;
    private final UpdateTimeAndDateCallback mCallback;

    public TimeChangeListenerMixin(Context context, UpdateTimeAndDateCallback callback) {
        mContext = context;
        mCallback = callback;
    }

    @Override
    public void onResume() {
        // Register for time ticks and other reasons for time change
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        mContext.registerReceiver(this, filter, null, null);
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mCallback != null) {
            mCallback.updateTimeAndDateDisplay(mContext);
        }
    }
}
