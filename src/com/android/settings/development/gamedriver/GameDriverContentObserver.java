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

package com.android.settings.development.gamedriver;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

/**
 * Helper class to observe Game Driver settings global change.
 */
public class GameDriverContentObserver extends ContentObserver {

    interface OnGameDriverContentChangedListener {
        void onGameDriverContentChanged();
    }

    @VisibleForTesting
    OnGameDriverContentChangedListener mListener;

    public GameDriverContentObserver(Handler handler, OnGameDriverContentChangedListener listener) {
        super(handler);
        mListener = listener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        mListener.onGameDriverContentChanged();
    }

    public void register(ContentResolver contentResolver) {
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.GAME_DRIVER_ALL_APPS), false, this);
    }

    public void unregister(ContentResolver contentResolver) {
        contentResolver.unregisterContentObserver(this);
    }
}
