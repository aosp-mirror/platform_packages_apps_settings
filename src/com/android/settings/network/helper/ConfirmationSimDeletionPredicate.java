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
package com.android.settings.network.helper;

import android.app.KeyguardManager;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;

import java.util.function.Predicate;

/**
 * {@link Predicate} for detecting the configuration of confirm SIM deletion.
 */
public class ConfirmationSimDeletionPredicate implements Predicate<Context> {

    public static final String KEY_CONFIRM_SIM_DELETION = "confirm_sim_deletion";

    private static final ConfirmationSimDeletionPredicate sSingleton =
            new ConfirmationSimDeletionPredicate();

    // Get singleton of this predicate
    public static final ConfirmationSimDeletionPredicate getSingleton() {
        return sSingleton;
    }

    /**
     * Get default configuration of confirm SIM deletion.
     *
     * @param Context context
     * @return the configuration of confirm SIM deletion
     */
    private static boolean getDefaultValue(Context context) {
        return context.getResources()
                .getBoolean(R.bool.config_sim_deletion_confirmation_default_on);
    }

    /**
     * Get the configuration of confirm SIM deletion.
     *
     * @param Context context
     * @return the configuration of confirm SIM deletion
     */
    public boolean test(Context context) {
        final KeyguardManager keyguardManager = context.getSystemService(KeyguardManager.class);
        if ((keyguardManager != null) && !keyguardManager.isKeyguardSecure()) {
            return false;
        }
        return Settings.Global.getInt(context.getContentResolver(), KEY_CONFIRM_SIM_DELETION,
                getDefaultValue(context) ? 1 : 0) == 1;
    }
}
