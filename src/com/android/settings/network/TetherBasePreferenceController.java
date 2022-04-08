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

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.datausage.DataSaverBackend;

public abstract class TetherBasePreferenceController extends TogglePreferenceController
        implements LifecycleObserver,  DataSaverBackend.Listener,
        TetherEnabler.OnTetherStateUpdateListener {

    private static final String TAG = "TetherBasePreferenceController";
    final ConnectivityManager mCm;
    private final DataSaverBackend mDataSaverBackend;

    private TetherEnabler mTetherEnabler;
    Preference mPreference;
    private boolean mDataSaverEnabled;
    int mTetheringState;

    TetherBasePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mDataSaverBackend = new DataSaverBackend(context);
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
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
        mDataSaverBackend.addListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (mTetherEnabler != null) {
            mTetherEnabler.removeListener(this);
        }
        mDataSaverBackend.remListener(this);
    }

    @Override
    public boolean isChecked() {
        return TetherEnabler.isTethering(mTetheringState, getTetherType());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mTetherEnabler == null) {
            return false;
        }
        if (isChecked) {
            mTetherEnabler.startTethering(getTetherType());
        } else {
            mTetherEnabler.stopTethering(getTetherType());
        }
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (isAvailable()) {
            preference.setEnabled(getAvailabilityStatus() != DISABLED_DEPENDENT_SETTING);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!shouldShow()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        if (mDataSaverEnabled || !shouldEnable()) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public void onTetherStateUpdated(@TetherEnabler.TetheringState int state) {
        mTetheringState = state;
        updateState(mPreference);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
    }

    /**
     * Used to enable or disable the preference.
     * @return true if the preference should be enabled; false otherwise.
     */
    public abstract boolean shouldEnable();

    /**
     * Used to determine visibility of the preference.
     * @return true if the preference should be visible; false otherwise.
     */
    public abstract boolean shouldShow();

    /**
     * Get the type of tether interface that is controlled by the preference.
     * @return the tether interface, like {@link ConnectivityManager#TETHERING_WIFI}
     */
    public abstract int getTetherType();
}
