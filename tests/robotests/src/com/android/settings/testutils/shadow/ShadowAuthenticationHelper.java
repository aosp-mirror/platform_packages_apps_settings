/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.settingslib.accounts.AuthenticatorHelper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(AuthenticatorHelper.class)
public class ShadowAuthenticationHelper {
    static final String[] TYPES = {"type1", "type2", "type3", "type4"};
    static final String[] LABELS = {"LABEL1", "LABEL2", "LABEL3", "LABEL4"};
    private static String[] sEnabledAccount = TYPES;

    @Implementation
    protected void __constructor__(Context context, UserHandle userHandle,
            AuthenticatorHelper.OnAccountsUpdateListener listener) {
    }

    public static void setEnabledAccount(String[] enabledAccount) {
        sEnabledAccount = enabledAccount;
    }

    @Resetter
    public static void reset() {
        sEnabledAccount = TYPES;
    }

    @Implementation
    protected String[] getEnabledAccountTypes() {
        return sEnabledAccount;
    }

    @Implementation
    protected CharSequence getLabelForType(Context context, final String accountType) {
        if (TextUtils.equals(accountType, TYPES[0])) {
            return LABELS[0];
        } else if (TextUtils.equals(accountType, TYPES[1])) {
            return LABELS[1];
        } else if (TextUtils.equals(accountType, TYPES[2])) {
            return LABELS[2];
        } else if (TextUtils.equals(accountType, TYPES[3])) {
            return LABELS[3];
        }
        return null;
    }

    @Implementation
    protected Drawable getDrawableForType(Context context, final String accountType) {
        return context.getPackageManager().getDefaultActivityIcon();
    }

    public static String[] getTypes() {
        return TYPES;
    }

    public static String[] getLabels() {
        return LABELS;
    }
}
