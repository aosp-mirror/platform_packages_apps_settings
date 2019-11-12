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

package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * A listener for Settings.Global configuration change, with support of Lifecycle
 */
public abstract class GlobalSettingsChangeListener extends ContentObserver
        implements LifecycleObserver, AutoCloseable {

    /**
     * Constructor
     *
     * @param context of this listener
     * @param field field of Global Settings
     */
    public GlobalSettingsChangeListener(Context context, String field) {
        super(new Handler());
        mContext = context;
        mField = field;
        monitorUri(true);
    }

    private Context mContext;
    private String mField;
    private Uri mUri;
    private Lifecycle mLifecycle;

    /**
     * Observed Settings got changed
     */
    public abstract void onChanged(String field);

    /**
     * Notify change of Globals.Setting based on given Lifecycle
     *
     * @param lifecycle life cycle to reference
     */
    public void notifyChangeBasedOn(Lifecycle lifecycle) {
        if (mLifecycle != null) {
            mLifecycle.removeObserver(this);
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mLifecycle = lifecycle;
    }

    public void onChange(boolean selfChange) {
        if (!isMonitoring()) {
            return;
        }
        onChanged(mField);
    }

    @OnLifecycleEvent(ON_START)
    void onStart() {
        monitorUri(true);
    }

    @OnLifecycleEvent(ON_STOP)
    void onStop() {
        monitorUri(false);
    }

    @OnLifecycleEvent(ON_DESTROY)
    void onDestroy() {
        close();
    }

    /**
     * Implementation of AutoCloseable
     */
    public void close() {
        monitorUri(false);
        notifyChangeBasedOn(null);
    }

    private boolean isMonitoring() {
        return (mUri != null);
    }

    private void monitorUri(boolean on) {
        if (isMonitoring() == on) {
            return;
        }
        if (mUri == null) {
            mUri = Settings.Global.getUriFor(mField);
            mContext.getContentResolver().registerContentObserver(mUri, false, this);
            return;
        }
        mUri = null;
        mContext.getContentResolver().unregisterContentObserver(this);
    }
}
