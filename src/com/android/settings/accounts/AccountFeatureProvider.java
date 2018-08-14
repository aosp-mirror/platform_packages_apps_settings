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

package com.android.settings.accounts;

import android.accounts.Account;
import android.content.Context;
import android.util.FeatureFlagUtils;

import com.android.settings.core.FeatureFlags;

public interface AccountFeatureProvider {
    String getAccountType();
    Account[] getAccounts(Context context);
    /**
     * Checks whether or not to display the new About Phone page.
     */
    default boolean isAboutPhoneV2Enabled(Context context) {
        return FeatureFlagUtils.isEnabled(context, FeatureFlags.ABOUT_PHONE_V2);
    }
}
