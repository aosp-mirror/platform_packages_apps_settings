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
 * limitations under the License
 */

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.SubscriptionsChangeListener;

public class DataDuringCallsPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private SwitchPreference mPreference;
    private SubscriptionsChangeListener mChangeListener;
    private TelephonyManager mManager;
    private MobileDataContentObserver mMobileDataContentObserver;
    private PreferenceScreen mScreen;

    public DataDuringCallsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    public void init(Lifecycle lifecycle, int subId) {
        this.mSubId = subId;
        mManager = mContext.getSystemService(TelephonyManager.class).createForSubscriptionId(subId);
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        if (mChangeListener == null) {
            mChangeListener = new SubscriptionsChangeListener(mContext, this);
        }
        mChangeListener.start();
        if (mMobileDataContentObserver == null) {
            mMobileDataContentObserver = new MobileDataContentObserver(
                    new Handler(Looper.getMainLooper()));
            mMobileDataContentObserver.setOnMobileDataChangedListener(() -> refreshPreference());
        }
        mMobileDataContentObserver.register(mContext, mSubId);
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        if (mChangeListener != null) {
            mChangeListener.stop();
        }
        if (mMobileDataContentObserver != null) {
            mMobileDataContentObserver.unRegister(mContext);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mScreen = screen;
    }

    @Override
    public boolean isChecked() {
        return mManager.isDataAllowedInVoiceCall();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mManager.setDataAllowedDuringVoiceCall(isChecked);
        return true;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)
                || SubscriptionManager.getDefaultDataSubscriptionId() == subId) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        preference.setVisible(isAvailable());
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {}

    @Override
    public void onSubscriptionsChanged() {
        updateState(mPreference);
    }

    /**
     * Trigger displaying preference when Mobilde data content changed.
     */
    @VisibleForTesting
    public void refreshPreference() {
        if (mScreen != null) {
            super.displayPreference(mScreen);
        }
    }
}
