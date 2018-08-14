/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development.autofill;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

final class AutofillDeveloperSettingsObserver extends ContentObserver {

    private final Runnable mChangeCallback;
    private final ContentResolver mResolver;

    public AutofillDeveloperSettingsObserver(Context context, Runnable changeCallback) {
        super(new Handler(Looper.getMainLooper()));

        mResolver = context.getContentResolver();
        mChangeCallback = changeCallback;
    }

    public void register() {
        mResolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.AUTOFILL_LOGGING_LEVEL), false, this,
                UserHandle.USER_ALL);
        mResolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.AUTOFILL_MAX_PARTITIONS_SIZE), false, this,
                UserHandle.USER_ALL);
        mResolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.AUTOFILL_MAX_VISIBLE_DATASETS), false, this,
                UserHandle.USER_ALL);
    }

    public void unregister() {
        mResolver.unregisterContentObserver(this);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri, int userId) {
        mChangeCallback.run(); // Run Forrest, Run!
    }
}
