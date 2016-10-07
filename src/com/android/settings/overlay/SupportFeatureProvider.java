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
import android.annotation.StringRes;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

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
     * Returns a intent that will open help & feedback.
     */
    Intent getHelpIntent(Context context);

    /**
     * Whether or not a support type is enabled.
     */
    boolean isSupportTypeEnabled(Context context, @SupportType int type);

    /**
     * Refreshes all operation rules.
     */
    void refreshOperationRules();

    /**
     * Whether or not a support type is in operation 24/7. If country is null, use
     * current country.
     */
    boolean isAlwaysOperating(@SupportType int type, String countryCode);

    /**
     * Whether or not a support type is operating now.
     */
    boolean isOperatingNow(@SupportType int type);

    /**
     * Returns the current country code if it has a operation config, otherwise returns null.
     */
    String getCurrentCountryCodeIfHasConfig(@SupportType int type);

    /**
     * Returns localized string for operation hours in specified country. If country is null, use
     * current country to figure out operation hours.
     */
    CharSequence getOperationHours(Context context, @SupportType int type, String countryCode,
            boolean hasInternet);

    /**
     * Returns a localized string indicating estimated wait time for a support time.
     */
    String getEstimatedWaitTime(Context context, @SupportType int type);

    /**
     * Returns a list of country codes that have phone support.
     */
    List<String> getPhoneSupportCountryCodes();

    /**
     * Returns a list of countries that have phone support.
     */
    List<String> getPhoneSupportCountries();

    /**
     * Returns a support phone for specified country.
     */
    SupportPhone getSupportPhones(String countryCode, boolean isTollfree);

    /**
     * Whether or not a disclaimer dialog should be displayed.
     */
    boolean shouldShowDisclaimerDialog(Context context);

    /**
     * Sets whether or not a disclaimer dialog should be displayed.
     */
    void setShouldShowDisclaimerDialog(Context context, boolean shouldShow);

    /**
     * Returns an {@link Account} that's eligible for support options.
     */
    Account getSupportEligibleAccount(Context context);

    /**
     * Starts support activity of specified type
     *
     * @param activity Calling activity
     * @param account A account returned by {@link #getSupportEligibleAccount}
     * @param type The type of support account needs.
     */
    void startSupport(Activity activity, Account account, @SupportType int type);

    /**
     * Returns an {@link Intent} that opens help and allow user get help on sign in.
     */
    Intent getSignInHelpIntent(Context context);

    /**
     * Returns an intent that will start the add account UI.
     */
    Intent getAccountLoginIntent();

    /**
     * Returns an intent that will launch the tips and tricks UI.
     */
    Intent getTipsAndTricksIntent(Context context);

    /**
     * Returns the string for the disclaimer in the Support dialog
     */
    @StringRes
    int getDisclaimerStringResId();
}
