/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.overlay;

import android.accounts.Account;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Feature provider for support tab.
 */
public interface SupportFeatureProvider {

    @IntDef({SupportType.EMAIL, SupportType.PHONE, SupportType.CHAT})
    @Retention(RetentionPolicy.SOURCE)
    @interface SupportType {
        int EMAIL = 1;
        int PHONE = 2;
        int CHAT = 3;
    }

    /**
     * Returns a intent that will open help forum.
     */
    Intent getForumIntent();

    /**
     * Whether or not a support type is enabled.
     */
    boolean isSupportTypeEnabled(Context context, @SupportType int type);

    /**
     * Returns an {@link Account} that's eligible for support options.
     */
    @NonNull
    Account[] getSupportEligibleAccounts(Context context);

    /**
     * Returns an {@link Intent} that opens email support for specified account.
     *
     * @param context A UI Context
     * @param account A account returned by {@link #getSupportEligibleAccounts}
     * @param type The type of support account needs.
     */
    Intent getSupportIntent(Context context, Account account, @SupportType int type);

}
