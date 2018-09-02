/*
 * Copyright (C) 2008 The Android Open Source Project
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

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

/**
 * ProviderPreference is used to display an image to the left of a provider name.
 * The preference ultimately calls AccountManager.addAccount() for the account type.
 */
public class ProviderPreference extends RestrictedPreference {
    private String mAccountType;

    public ProviderPreference(
            Context context, String accountType, Drawable icon, CharSequence providerName) {
        super(context);
        setIconSize(ICON_SIZE_MEDIUM);
        mAccountType = accountType;
        setIcon(icon);
        setPersistent(false);
        setTitle(providerName);
        useAdminDisabledSummary(true);
    }

    public String getAccountType() {
        return mAccountType;
    }

    public void checkAccountManagementAndSetDisabled(int userId) {
        EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfAccountManagementDisabled(
                getContext(), getAccountType(), userId);
        setDisabledByAdmin(admin);
    }
}
