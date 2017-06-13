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

package com.android.settings.core;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public abstract class DynamicAvailabilityPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume {

    private Preference mPreference;
    private PreferenceScreen mScreen;
    private PreferenceAvailabilityObserver mAvailabilityObserver = null;

    public DynamicAvailabilityPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    public void setAvailabilityObserver(PreferenceAvailabilityObserver observer) {
        mAvailabilityObserver = observer;
    }

    public PreferenceAvailabilityObserver getAvailabilityObserver() {
        return mAvailabilityObserver;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }

    @Override
    public void onResume() {
        if (!isAvailable()) {
            removePreference(mScreen, getPreferenceKey());
            return;
        }

        updateState(mPreference);
        if (mScreen.findPreference(getPreferenceKey()) == null) {
            mScreen.addPreference(mPreference);
        }
    }

    protected void notifyOnAvailabilityUpdate(boolean available) {
        if (mAvailabilityObserver != null) {
            mAvailabilityObserver.onPreferenceAvailabilityUpdated(getPreferenceKey(), available);
        }
    }
}
