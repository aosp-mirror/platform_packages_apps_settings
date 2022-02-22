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

package com.android.settings.biometrics;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.Utils;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settingslib.transition.SettingsTransitionHelper;

/**
 * Utilities for navigation shared between Security Settings and Safety Center.
 */
public class BiometricNavigationUtils {

    private final int mUserId = UserHandle.myUserId();

    /**
     * Tries to launch the Settings screen if Quiet Mode is not enabled
     * for managed profile, otherwise shows a dialog to disable the Quiet Mode.
     *
     * @param className The class name of Settings screen to launch.
     * @param extras Extras to put into the launching {@link Intent}.
     * @return true if the Settings screen is launching.
     */
    public boolean launchBiometricSettings(Context context, String className, Bundle extras) {
        final UserManager userManager = UserManager.get(context);
        if (Utils.startQuietModeDialogIfNecessary(context, userManager, mUserId)) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME, className);
        if (!extras.isEmpty()) {
            intent.putExtras(extras);
        }
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true);
        intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        intent.putExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE);
        context.startActivity(intent);
        return true;
    }
}
