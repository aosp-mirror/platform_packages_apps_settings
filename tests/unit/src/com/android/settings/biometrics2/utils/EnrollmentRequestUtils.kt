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
package com.android.settings.biometrics2.utils

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics2.ui.model.EnrollmentRequest
import com.google.android.setupcompat.util.WizardManagerHelper

object EnrollmentRequestUtils {
    @JvmStatic
    fun newAllFalseRequest(context: Context): EnrollmentRequest {
        return newRequest(
            context = context,
            isSuw = false,
            isSuwDeferred = false,
            isSuwPortal = false,
            isSuwSuggestedActionFlow = false,
            isSuwFirstRun = false,
            isFromSettingsSummery = false)
    }

    @JvmStatic
    fun newIsSuwRequest(context: Context): EnrollmentRequest {
        return newRequest(
            context = context,
            isSuw = true,
            isSuwDeferred = false,
            isSuwPortal = false,
            isSuwSuggestedActionFlow = false,
            isSuwFirstRun = false,
            isFromSettingsSummery = false)
    }

    @JvmStatic
    fun newIsSuwDeferredRequest(context: Context): EnrollmentRequest {
        return newRequest(
            context = context,
            isSuw = true,
            isSuwDeferred = true,
            isSuwPortal = false,
            isSuwSuggestedActionFlow = false,
            isSuwFirstRun = false,
            isFromSettingsSummery = false, null)
    }

    @JvmStatic
    fun newIsSuwPortalRequest(context: Context): EnrollmentRequest {
        return newRequest(
            context = context,
            isSuw = true,
            isSuwDeferred = false,
            isSuwPortal = true,
            isSuwSuggestedActionFlow = false,
            isSuwFirstRun = false,
            isFromSettingsSummery = false)
    }

    @JvmStatic
    fun newIsSuwSuggestedActionFlowRequest(
        context: Context
    ): EnrollmentRequest {
        return newRequest(
            context = context,
            isSuw = true,
            isSuwDeferred = false,
            isSuwPortal = false,
            isSuwSuggestedActionFlow = true,
            isSuwFirstRun = false,
            isFromSettingsSummery = false)
    }

    fun newRequest(
        context: Context,
        isSuw: Boolean,
        isSuwDeferred: Boolean,
        isSuwPortal: Boolean,
        isSuwSuggestedActionFlow: Boolean,
        isSuwFirstRun: Boolean,
        isFromSettingsSummery: Boolean,
        theme: String? = null
    ): EnrollmentRequest {
        val i = Intent()
        i.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, isSuw)
        i.putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, isSuwDeferred)
        i.putExtra(WizardManagerHelper.EXTRA_IS_PORTAL_SETUP, isSuwPortal)
        i.putExtra(WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, isSuwSuggestedActionFlow)
        i.putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, isSuwFirstRun)
        i.putExtra(BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY, isFromSettingsSummery)
        if (!TextUtils.isEmpty(theme)) {
            i.putExtra(WizardManagerHelper.EXTRA_THEME, theme)
        }
        return EnrollmentRequest(i, context, true)
    }
}
