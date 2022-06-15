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
package com.android.settings.dashboard;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/**
 * Observer for updating injected dynamic data.
 */
public abstract class DynamicDataObserver extends ContentObserver {

    protected DynamicDataObserver() {
        super(new Handler(Looper.getMainLooper()));
    }

    /** Returns the uri of the callback. */
    public abstract Uri getUri();

    /** Called when data changes. */
    public abstract void onDataChanged();

    @Override
    public void onChange(boolean selfChange) {
        onDataChanged();
    }
}
