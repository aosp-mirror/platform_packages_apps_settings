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

package com.android.settings.development;

import android.content.Context;
import android.content.Intent;

/**
 * Host of {@link RebootConfirmationDialogFragment} that provides callback when user interacts with
 * the UI.
 */
public interface RebootConfirmationDialogHost {
    /** Called when user made a decision to reboot the device. */
    default void onRebootConfirmed(Context context) {
        // user presses button "Reboot now", reboot the device
        final Intent intent = new Intent(Intent.ACTION_REBOOT);
        context.startActivity(intent);
    }

    /** Called when user made a decision to cancel the reboot Default to do nothing */
    default void onRebootCancelled() {}

    /** Called when reboot dialog is dismissed Default to do nothing */
    default void onRebootDialogDismissed() {}
}
