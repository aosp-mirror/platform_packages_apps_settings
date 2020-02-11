/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.graphicsdriver;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

/**
 * Helper class to observe Graphics Driver settings global change.
 */
public class GraphicsDriverContentObserver extends ContentObserver {

    interface OnGraphicsDriverContentChangedListener {
        void onGraphicsDriverContentChanged();
    }

    @VisibleForTesting
    OnGraphicsDriverContentChangedListener mListener;

    public GraphicsDriverContentObserver(Handler handler,
            OnGraphicsDriverContentChangedListener listener) {
        super(handler);
        mListener = listener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        mListener.onGraphicsDriverContentChanged();
    }

    /**
     * Register GraphicsDriverContentObserver to ContentResolver.
     */
    public void register(ContentResolver contentResolver) {
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.GAME_DRIVER_ALL_APPS), false, this);
    }

    /**
     * Unregister GraphicsDriverContentObserver.
     */
    public void unregister(ContentResolver contentResolver) {
        contentResolver.unregisterContentObserver(this);
    }
}
