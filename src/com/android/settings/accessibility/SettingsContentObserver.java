/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

abstract class SettingsContentObserver extends ContentObserver {
    public SettingsContentObserver(Handler handler) {
        super(handler);
    }

    public void register(ContentResolver contentResolver) {
        contentResolver.registerContentObserver(Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_ENABLED), false, this);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor(
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, this);
    }

    public void unregister(ContentResolver contentResolver) {
        contentResolver.unregisterContentObserver(this);
    }

    @Override
    public abstract void onChange(boolean selfChange, Uri uri);
}
