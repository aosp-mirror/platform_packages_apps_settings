/*
 * Copyright (C) 2022 Yet Another AOSP Project
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

package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

public class AutoBrightnessObserver {
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private Runnable mCallback;

    public AutoBrightnessObserver(Context context) {
        mContext = context;
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                mCallback.run();
            }
        };
    }

    public void subscribe(Runnable callback) {
        mCallback = callback;
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SCREEN_BRIGHTNESS_MODE),
                false, mContentObserver);
    }

    public void unsubscribe() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }
}
