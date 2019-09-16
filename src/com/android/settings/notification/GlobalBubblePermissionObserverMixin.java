/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

public class GlobalBubblePermissionObserverMixin extends ContentObserver {

    public interface Listener {
        void onGlobalBubblePermissionChanged();
    }

    private final Context mContext;
    private final Listener mListener;

    public GlobalBubblePermissionObserverMixin(Context context, Listener listener) {
        super(new Handler(Looper.getMainLooper()));
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (mListener != null) {
            mListener.onGlobalBubblePermissionChanged();
        }
    }

    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(
                        Settings.Global.NOTIFICATION_BUBBLES),
                false /* notifyForDescendants */,
                this /* observer */);
    }

    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(this /* observer */);
    }
}