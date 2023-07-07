/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.model;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;

import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.android.settings.SetupWizardUtils;
import com.android.settings.biometrics.BiometricEnrollActivity;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * Biometric enrollment generic intent data, which includes
 * 1. isSuw
 * 2. isAfterSuwOrSuwSuggestedAction
 * 3. theme
 * 4. isFromSettingsSummery
 * 5. isSkipIntro
 * 6. isSkipFindSensor
 * 7. a helper method, getSetupWizardExtras
 */
public final class EnrollmentRequest {

    public static final String EXTRA_SKIP_FIND_SENSOR = "skip_find_sensor";

    private final boolean mIsSuw;
    private final boolean mIsAfterSuwOrSuwSuggestedAction;
    private final boolean mIsFromSettingsSummery;
    private final boolean mIsSkipIntro;
    private final boolean mIsSkipFindSensor;
    private final int mTheme;
    private final Bundle mSuwExtras;

    public EnrollmentRequest(@NonNull Intent intent, @NonNull Context context) {
        mIsSuw = WizardManagerHelper.isAnySetupWizard(intent);
        mIsAfterSuwOrSuwSuggestedAction = WizardManagerHelper.isDeferredSetupWizard(intent)
                || WizardManagerHelper.isPortalSetupWizard(intent)
                || intent.getBooleanExtra(EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, false);
        mSuwExtras = getSuwExtras(mIsSuw, intent);
        mIsFromSettingsSummery = intent.getBooleanExtra(EXTRA_FROM_SETTINGS_SUMMARY, false);
        mIsSkipIntro = intent.getBooleanExtra(BiometricEnrollActivity.EXTRA_SKIP_INTRO, false);
        mIsSkipFindSensor = intent.getBooleanExtra(EXTRA_SKIP_FIND_SENSOR, false);
        mTheme = SetupWizardUtils.getTheme(context, intent);
    }

    public boolean isSuw() {
        return mIsSuw;
    }

    public boolean isAfterSuwOrSuwSuggestedAction() {
        return mIsAfterSuwOrSuwSuggestedAction;
    }

    public boolean isFromSettingsSummery() {
        return mIsFromSettingsSummery;
    }

    public boolean isSkipIntro() {
        return mIsSkipIntro;
    }

    public boolean isSkipFindSensor() {
        return mIsSkipFindSensor;
    }

    public int getTheme() {
        return mTheme;
    }

    @NonNull
    public Bundle getSuwExtras() {
        return new Bundle(mSuwExtras);
    }

    /**
     * Returns a string representation of the object
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + ":{isSuw:" + mIsSuw
                + ", isAfterSuwOrSuwSuggestedAction:" + mIsAfterSuwOrSuwSuggestedAction
                + ", isFromSettingsSummery:" + mIsFromSettingsSummery
                + "}";
    }

    @NonNull
    private static Bundle getSuwExtras(boolean isSuw, @NonNull Intent intent) {
        final Intent toIntent = new Intent();
        if (isSuw) {
            SetupWizardUtils.copySetupExtras(intent, toIntent);
        }
        return toIntent.getExtras() != null ? toIntent.getExtras() : new Bundle();
    }
}
