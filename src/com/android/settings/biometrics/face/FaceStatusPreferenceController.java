/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.hardware.face.FaceManager;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

public class FaceStatusPreferenceController extends BiometricStatusPreferenceController
        implements LifecycleObserver {

    public static final String KEY_FACE_SETTINGS = "face_settings";

    protected final FaceManager mFaceManager;

    @VisibleForTesting
    RestrictedPreference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private final FaceStatusUtils mFaceStatusUtils;

    public FaceStatusPreferenceController(Context context) {
        this(context, KEY_FACE_SETTINGS, null /* lifecycle */);
    }

    public FaceStatusPreferenceController(Context context, String key) {
        this(context, key, null /* lifecycle */);
    }

    public FaceStatusPreferenceController(Context context, Lifecycle lifecycle) {
        this(context, KEY_FACE_SETTINGS, lifecycle);
    }

    public FaceStatusPreferenceController(Context context, String key, Lifecycle lifecycle) {
        super(context, key);
        mFaceManager = Utils.getFaceManagerOrNull(context);
        mFaceStatusUtils = new FaceStatusUtils(context, mFaceManager, getUserId());

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        updateStateInternal();
        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPreference = screen.findPreference(mPreferenceKey);
    }

    @Override
    protected boolean isDeviceSupported() {
        return mFaceStatusUtils.isAvailable();
    }

    @Override
    protected boolean isHardwareSupported() {
        return Utils.hasFaceHardware(mContext);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateStateInternal();
    }

    private void updateStateInternal() {
        updateStateInternal(mFaceStatusUtils.getDisablingAdmin());
    }

    @VisibleForTesting
    void updateStateInternal(@Nullable RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        if (mPreference != null) {
            mPreference.setDisabledByAdmin(enforcedAdmin);
        }
    }

    @Override
    protected String getSummaryText() {
        return mFaceStatusUtils.getSummary();
    }

    @Override
    protected String getSettingsClassName() {
        return mFaceStatusUtils.getSettingsClassName();
    }

    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        mPreferenceScreen = preferenceScreen;
    }
}
