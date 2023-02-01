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

package com.android.settings.biometrics2.utils;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;

import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_FIRST_RUN;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_PORTAL_SETUP;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_THEME;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.settings.biometrics2.ui.model.EnrollmentRequest;

public class EnrollmentRequestUtils {

    @NonNull
    public static EnrollmentRequest newAllFalseRequest(@NonNull Context context) {
        return newRequest(context, false, false, false, false, false, false, null);
    }

    @NonNull
    public static EnrollmentRequest newIsSuwRequest(@NonNull Context context) {
        return newRequest(context, true, false, false, false, false, false, null);
    }

    @NonNull
    public static EnrollmentRequest newIsSuwDeferredRequest(@NonNull Context context) {
        return newRequest(context, true, true, false, false, false, false, null);
    }

    @NonNull
    public static EnrollmentRequest newIsSuwPortalRequest(@NonNull Context context) {
        return newRequest(context, true, false, true, false, false, false, null);
    }

    @NonNull
    public static EnrollmentRequest newIsSuwSuggestedActionFlowRequest(
            @NonNull Context context) {
        return newRequest(context, true, false, false, true, false, false, null);
    }

    @NonNull
    public static EnrollmentRequest newRequest(@NonNull Context context, boolean isSuw,
            boolean isSuwDeferred, boolean isSuwPortal, boolean isSuwSuggestedActionFlow,
            boolean isSuwFirstRun, boolean isFromSettingsSummery, String theme) {
        Intent i = new Intent();
        i.putExtra(EXTRA_IS_SETUP_FLOW, isSuw);
        i.putExtra(EXTRA_IS_DEFERRED_SETUP, isSuwDeferred);
        i.putExtra(EXTRA_IS_PORTAL_SETUP, isSuwPortal);
        i.putExtra(EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, isSuwSuggestedActionFlow);
        i.putExtra(EXTRA_IS_FIRST_RUN, isSuwFirstRun);
        i.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, isFromSettingsSummery);
        if (!TextUtils.isEmpty(theme)) {
            i.putExtra(EXTRA_THEME, theme);
        }
        return new EnrollmentRequest(i, context);
    }

}
