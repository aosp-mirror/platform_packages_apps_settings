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
import android.annotation.StringRes;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.support.SupportPhone;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

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
     * Refreshes all operation rules.
     */
    void refreshOperationRules();

    /**
     * Returns the current country code if it has a operation config, otherwise returns null.
     */
    String getCurrentCountryCodeIfHasConfig(@SupportType int type);

    /**
     * Returns a support phone for specified country.
     */
    SupportPhone getSupportPhones(String countryCode, boolean isTollfree);

    /**
     * Returns array of {@link Account} that's eligible for support options.
     */
    @NonNull
    Account[] getSupportEligibleAccounts(Context context);

    /**
     * Starts support v2, invokes the support home page. Will no-op if support v2 is not enabled.
     *
     * @param activity Calling activity.
     */
    void startSupportV2(Activity activity);

    /**
     * Returns a url with information to introduce user to new device.
     */
    String getNewDeviceIntroUrl(Context context);
}
