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

package com.android.settings.dashboard.suggestions;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.settings.suggestions.ISuggestionService;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.VisibleForTesting;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * Manages IPC communication to SettingsIntelligence for suggestion related services.
 */
public class SuggestionControllerMixin implements LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    static final String FEATURE_FLAG = "new_settings_suggestion";
    private static final String TAG = "SuggestionCtrlMixin";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final Intent mServiceIntent;
    private final ServiceConnection mServiceConnection;

    private ISuggestionService mRemoteService;

    public static boolean isEnabled() {
        return FeatureFlagUtils.isEnabled(FEATURE_FLAG);
    }

    public SuggestionControllerMixin(Context context, Lifecycle lifecycle) {
        mContext = context.getApplicationContext();
        mServiceIntent = new Intent().setComponent(
                new ComponentName(
                        "com.android.settings.intelligence",
                        "com.android.settings.intelligence.suggestions.SuggestionService"));
        mServiceConnection = createServiceConnection();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        if (!isEnabled()) {
            Log.w(TAG, "Feature not enabled, skipping");
            return;
        }
        mContext.bindServiceAsUser(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE,
                android.os.Process.myUserHandle());
    }

    @Override
    public void onStop() {
        if (mRemoteService != null) {
            mRemoteService = null;
            mContext.unbindService(mServiceConnection);
        }
    }

    /**
     * Get setting suggestions.
     */
    @Nullable
    public List<Suggestion> getSuggestions() {
        if (!isReady()) {
            return null;
        }
        try {
            return mRemoteService.getSuggestions();
        } catch (RemoteException e) {
            Log.w(TAG, "Error when calling getSuggestion()", e);
            return null;
        }
    }

    /**
     * Whether or not the manager is ready
     */
    private boolean isReady() {
        return mRemoteService != null;
    }

    @VisibleForTesting
    void onServiceConnected() {
        // TODO: Call API to get data from a loader instead of in current thread.
        final List<Suggestion> data = getSuggestions();
        Log.d(TAG, "data size " + (data == null ? 0 : data.size()));
    }

    private void onServiceDisconnected() {

    }

    /**
     * Create a new {@link ServiceConnection} object to handle service connect/disconnect event.
     */
    private ServiceConnection createServiceConnection() {
        return new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (DEBUG) {
                    Log.d(TAG, "Service is connected");
                }
                mRemoteService = ISuggestionService.Stub.asInterface(service);
                SuggestionControllerMixin.this.onServiceConnected();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mRemoteService = null;
                SuggestionControllerMixin.this.onServiceDisconnected();
            }
        };
    }

}
