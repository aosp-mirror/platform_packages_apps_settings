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
package com.android.settings.applications.intentpicker;

import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.Log;

import java.util.List;
import java.util.stream.Collectors;

/** The common APIs for intent picker */
public class IntentPickerUtils {
    private static final String TAG = "IntentPickerUtils";
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;

    private IntentPickerUtils() {
    }

    /**
     * Gets the centralized title.
     *
     * @param title The title of the dialog box.
     * @return The spannable string with centralized title.
     */
    public static SpannableString getCentralizedDialogTitle(String title) {
        final SpannableString dialogTitle = new SpannableString(title);
        dialogTitle.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), /* start= */
                0, title.length(), /* flags= */ 0);
        return dialogTitle;
    }

    /**
     * Gets the {@link DomainVerificationUserState} for specific application.
     *
     * @param manager The {@link DomainVerificationManager}.
     * @param pkgName The package name of the target application.
     */
    public static DomainVerificationUserState getDomainVerificationUserState(
            DomainVerificationManager manager, String pkgName) {
        try {
            final DomainVerificationUserState domainVerificationUserState =
                    manager.getDomainVerificationUserState(pkgName);
            return domainVerificationUserState;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Gets the links list by {@link DomainVerificationUserState.DomainState}
     *
     * @param manager The {@link DomainVerificationManager}.
     * @param pkgName The package name of the target application.
     * @param state   The user state you want to query.
     * @return A links list.
     */
    public static List<String> getLinksList(DomainVerificationManager manager, String pkgName,
            @DomainVerificationUserState.DomainState int state) {
        final DomainVerificationUserState userStage = getDomainVerificationUserState(manager,
                pkgName);
        if (userStage == null) {
            return null;
        }
        return userStage.getHostToStateMap()
                .entrySet()
                .stream()
                .filter(it -> it.getValue() == state)
                .map(it -> it.getKey())
                .collect(Collectors.toList());
    }

    /** Logs the message in debug ROM. */
    public static void logd(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
