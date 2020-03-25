/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;

public abstract class TetherBasePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, TetherEnabler.OnTetherStateUpdateListener {

    private static final String TAG = "TetherBasePreferenceController";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    final ConnectivityManager mCm;

    TetherEnabler mTetherEnabler;
    Preference mPreference;

    public TetherBasePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Set TetherEnabler for the controller. Call this method to initialize the controller.
     * @param tetherEnabler The tetherEnabler to set for the controller.
     */
    public void setTetherEnabler(TetherEnabler tetherEnabler) {
        mTetherEnabler = tetherEnabler;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        // Must call setEnabler() before
        if (mTetherEnabler != null) {
            mTetherEnabler.addListener(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (mTetherEnabler != null) {
            mTetherEnabler.removeListener(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
    }
}
