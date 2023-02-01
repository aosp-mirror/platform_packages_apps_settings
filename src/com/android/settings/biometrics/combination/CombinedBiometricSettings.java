/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.biometrics.combination;

import static com.android.settings.biometrics.activeunlock.ActiveUnlockStatusPreferenceController.KEY_ACTIVE_UNLOCK_SETTINGS;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.biometrics.activeunlock.ActiveUnlockContentListener.OnContentChangedListener;
import com.android.settings.biometrics.activeunlock.ActiveUnlockDeviceNameListener;
import com.android.settings.biometrics.activeunlock.ActiveUnlockRequireBiometricSetup;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Settings screen for multiple biometrics.
 */
@SearchIndexable
public class CombinedBiometricSettings extends BiometricsSettingsBase {
    private static final String TAG = "BiometricSettings";
    private static final String KEY_FACE_SETTINGS = "biometric_face_settings";
    private static final String KEY_FINGERPRINT_SETTINGS = "biometric_fingerprint_settings";
    private static final String KEY_UNLOCK_PHONE = "biometric_settings_biometric_keyguard";
    private static final String KEY_USE_IN_APPS = "biometric_settings_biometric_app";
    private static final String KEY_INTRO_PREFERENCE = "biometric_intro";

    private ActiveUnlockStatusUtils mActiveUnlockStatusUtils;
    private CombinedBiometricStatusUtils mCombinedBiometricStatusUtils;
    @Nullable private ActiveUnlockDeviceNameListener mActiveUnlockDeviceNameListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(BiometricSettingsKeyguardPreferenceController.class).setUserId(mUserId);
        use(BiometricSettingsAppPreferenceController.class).setUserId(mUserId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActiveUnlockStatusUtils = new ActiveUnlockStatusUtils(getActivity());
        mCombinedBiometricStatusUtils = new CombinedBiometricStatusUtils(getActivity(), mUserId);
        if (mActiveUnlockStatusUtils.isAvailable()) {
            updateUiForActiveUnlock();
        }
    }

    private void updateUiForActiveUnlock() {
        OnContentChangedListener listener = new OnContentChangedListener() {
            @Override
            public void onContentChanged(String newValue) {
                updateUnlockPhonePreferenceSummary();
            }
        };

        mActiveUnlockDeviceNameListener =
                new ActiveUnlockDeviceNameListener(getActivity(), listener);
        mActiveUnlockDeviceNameListener.subscribe();
        final Preference introPreference = findPreference(KEY_INTRO_PREFERENCE);
        if (introPreference != null) {
            introPreference.setTitle(mActiveUnlockStatusUtils.getIntroForActiveUnlock());
        }
        getActivity().setTitle(mActiveUnlockStatusUtils.getTitleForActiveUnlock());
    }

    @Override
    public void onDestroy() {
        if (mActiveUnlockDeviceNameListener != null) {
            mActiveUnlockDeviceNameListener.unsubscribe();
        }
        super.onDestroy();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_settings_combined_biometric;
    }

    @Override
    public String getFacePreferenceKey() {
        return KEY_FACE_SETTINGS;
    }

    @Override
    public String getFingerprintPreferenceKey() {
        return KEY_FINGERPRINT_SETTINGS;
    }

    @Override
    public String getUnlockPhonePreferenceKey() {
        return KEY_UNLOCK_PHONE;
    }

    @Override
    public String getUseInAppsPreferenceKey() {
        return KEY_USE_IN_APPS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.COMBINED_BIOMETRIC;
    }

    @Override
    protected boolean onRetryPreferenceTreeClick(Preference preference, final boolean retry) {
        if (!mActiveUnlockStatusUtils.isAvailable()
                || !KEY_ACTIVE_UNLOCK_SETTINGS.equals(preference.getKey())) {
            return super.onRetryPreferenceTreeClick(preference, retry);
        }
        mDoNotFinishActivity = true;
        Intent intent;
        if (mActiveUnlockStatusUtils.useBiometricFailureLayout()
                && mActiveUnlockDeviceNameListener != null
                && !mActiveUnlockDeviceNameListener.hasEnrolled()
                && !mCombinedBiometricStatusUtils.hasEnrolled()) {
            intent = new Intent(getActivity(), ActiveUnlockRequireBiometricSetup.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int userId = mUserId;
            if (mUserId != UserHandle.USER_NULL) {
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            }
            intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, getGkPwHandle());
        } else {
            intent = mActiveUnlockStatusUtils.getIntent();
        }
        if (intent != null) {
            startActivityForResult(intent, ACTIVE_UNLOCK_REQUEST);
        }
        return true;

    }

    @Override
    protected String getUseAnyBiometricSummary() {
        // either Active Unlock is not enabled or no device is enrolled.
        if (mActiveUnlockDeviceNameListener == null
                || !mActiveUnlockDeviceNameListener.hasEnrolled()) {
            return super.getUseAnyBiometricSummary();
        }
        return mActiveUnlockStatusUtils.getUnlockDeviceSummaryForActiveUnlock();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new CombinedBiometricSearchIndexProvider(R.xml.security_settings_combined_biometric);
}
