/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.activeunlock;

import android.content.Context;

/** Listens to summary updates from the content provider and fetches the latest value. */
public class ActiveUnlockSummaryListener {
    private static final String TAG = "ActiveUnlockSummaryListener";
    private static final String METHOD_NAME = "getSummary";
    private static final String SUMMARY_KEY = "com.android.settings.summary";

    private final ActiveUnlockContentListener mContentListener;
    public ActiveUnlockSummaryListener(
            Context context, ActiveUnlockContentListener.OnContentChangedListener listener) {
        mContentListener = new ActiveUnlockContentListener(
                context, listener, TAG, METHOD_NAME, SUMMARY_KEY);
    }

    /** Subscribes for summary updates. */
    public void subscribe() {
        mContentListener.subscribe();
    }

    /** Unsubscribes from summary updates. */
    public void unsubscribe() {
        mContentListener.unsubscribe();
    }
}
