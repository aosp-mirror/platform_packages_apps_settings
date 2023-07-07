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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;
import com.android.settings.biometrics.activeunlock.ActiveUnlockContentListener.OnContentChangedListener;
import com.android.settings.biometrics.combination.CombinedBiometricStatusUtils;
import com.android.settingslib.RestrictedPreference;

/**
 * Preference controller for active unlock settings within the biometrics settings page, that
 * controls the ability to unlock the phone with watch authentication.
 */
public class ActiveUnlockStatusPreferenceController
        extends BiometricStatusPreferenceController implements LifecycleObserver {
    /**
     * Preference key.
     *
     * This must match the key found in security_settings_combined_biometric.xml
     **/
    public static final String KEY_ACTIVE_UNLOCK_SETTINGS = "biometric_active_unlock_settings";
    @Nullable private RestrictedPreference mPreference;
    @Nullable private PreferenceScreen mPreferenceScreen;
    @Nullable private String mSummary;
    private final ActiveUnlockStatusUtils mActiveUnlockStatusUtils;
    private final CombinedBiometricStatusUtils mCombinedBiometricStatusUtils;
    private final ActiveUnlockSummaryListener mActiveUnlockSummaryListener;
    private final ActiveUnlockDeviceNameListener mActiveUnlockDeviceNameListener;
    private final boolean mIsAvailable;

    public ActiveUnlockStatusPreferenceController(@NonNull Context context) {
        this(context, KEY_ACTIVE_UNLOCK_SETTINGS);
    }

    public ActiveUnlockStatusPreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
        mActiveUnlockStatusUtils = new ActiveUnlockStatusUtils(context);
        mIsAvailable = mActiveUnlockStatusUtils.isAvailable();
        mCombinedBiometricStatusUtils = new CombinedBiometricStatusUtils(context, getUserId());
        OnContentChangedListener onSummaryChangedListener = new OnContentChangedListener() {
            @Override
            public void onContentChanged(String newContent) {
                mSummary = newContent;
                if (mPreference != null) {
                    mPreference.setSummary(getSummaryText());
                }
            }
        };
        OnContentChangedListener onDeviceNameChangedListener =
                new OnContentChangedListener() {

            @Override
            public void onContentChanged(String newContent) {
                if (mPreference != null) {
                    mPreference.setSummary(getSummaryText());
                }
            }

        };
        mActiveUnlockSummaryListener =
                new ActiveUnlockSummaryListener(context, onSummaryChangedListener);
        mActiveUnlockDeviceNameListener =
                new ActiveUnlockDeviceNameListener(context, onDeviceNameChangedListener);
    }


    /** Subscribes to update preference summary dynamically. */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mIsAvailable) {
            mActiveUnlockSummaryListener.subscribe();
            mActiveUnlockDeviceNameListener.subscribe();
        }
    }

    /** Resets the preference reference on resume. */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
    }

    /** Unsubscribes to prevent leaked listener. */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        if (mIsAvailable) {
            mActiveUnlockSummaryListener.unsubscribe();
            mActiveUnlockDeviceNameListener.unsubscribe();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPreference = screen.findPreference(mPreferenceKey);
        updateState(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return mActiveUnlockStatusUtils.getAvailability();
    }

    @Override
    protected boolean isDeviceSupported() {
        // This should never be called, as getAvailabilityStatus() will return the exact value.
        // However, this is an abstract method in BiometricStatusPreferenceController, and so
        // needs to be overridden.
        return mIsAvailable;
    }

    @Override
    protected boolean isHardwareSupported() {
        // This should never be called, as getAvailabilityStatus() will return the exact value.
        // However, this is an abstract method in BiometricStatusPreferenceController, and so
        // needs to be overridden.
        return Utils.hasFaceHardware(mContext) || Utils.hasFingerprintHardware(mContext);
    }

    @Override
    protected String getSummaryText() {
        if (mActiveUnlockStatusUtils.useBiometricFailureLayout()
                && !mActiveUnlockDeviceNameListener.hasEnrolled()
                && !mCombinedBiometricStatusUtils.hasEnrolled()) {
            @Nullable final String setupString =
                    mActiveUnlockStatusUtils.getSummaryWhenBiometricSetupRequired();
            if (setupString != null) {
                return setupString;
            }
        }
        if (mSummary == null) {
            // return non-empty string to prevent re-sizing of the tile
            return " ";
        }
        return mSummary;
    }

    @Override
    protected String getSettingsClassName() {
        return ActiveUnlockRequireBiometricSetup.class.getName();
    }
}
