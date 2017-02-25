/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.core.instrumentation;

import android.content.Context;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.suggestions.EventStore;

/**
 * {@link LogWriter} that writes setting suggestion related logs.
 */
public class SettingSuggestionsLogWriter implements LogWriter {

    private EventStore mEventStore;

    @Override
    public void visible(Context context, int source, int category) {
    }

    @Override
    public void hidden(Context context, int category) {
    }

    @Override
    public void action(Context context, int category, Pair<Integer, Object>... taggedData) {
    }

    @Override
    public void actionWithSource(Context context, int source, int category) {
    }

    @Override
    public void action(Context context, int category, int value) {
    }

    @Override
    public void action(Context context, int category, boolean value) {
    }

    @Override
    public void action(Context context, int category, String pkg,
            Pair<Integer, Object>... taggedData) {
        if (mEventStore == null) {
            mEventStore = new EventStore(context);
        }
        switch (category) {
            case MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION:
                mEventStore.writeEvent(pkg, EventStore.EVENT_SHOWN);
                break;
            case MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION:
                mEventStore.writeEvent(pkg, EventStore.EVENT_DISMISSED);
                break;
            case MetricsEvent.ACTION_SETTINGS_SUGGESTION:
                mEventStore.writeEvent(pkg, EventStore.EVENT_CLICKED);
                break;
        }
    }

    @Override
    public void count(Context context, String name, int value) {
    }

    @Override
    public void histogram(Context context, String name, int bucket) {
    }

}
