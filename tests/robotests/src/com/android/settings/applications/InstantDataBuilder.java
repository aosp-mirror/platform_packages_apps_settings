/**
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.applications.instantapps.InstantAppDetails;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

/**
 * Utility class for generating fake InstantAppDetails data to use in tests.
 */
public class InstantDataBuilder {
    public enum Param {
        MATURITY_RATING_ICON,
        MATURITY_RATING_STRING,
        MONETIZATION_NOTICE,
        DEVELOPER_TITLE,
        PRIVACY_POLICY,
        DEVELOPER_WEBSITE,
        DEVELOPER_EMAIL,
        DEVELOPER_MAILING_ADDRESS
    }

    /**
     * Creates an InstantAppDetails with any desired combination of null/non-null members.
     *
     * @param context An optional context, required only if MATURITY_RATING_ICON is a member of
     * params
     * @param params Specifies which elements of the returned InstantAppDetails should be non-null
     * @return InstantAppDetails
     */
    public static InstantAppDetails build(@Nullable Context context, EnumSet<Param> params) {
        Drawable ratingIcon = null;
        String rating = null;
        String monetizationNotice = null;
        String developerTitle = null;
        URL privacyPolicy = null;
        URL developerWebsite = null;
        String developerEmail = null;
        String developerMailingAddress = null;

        if (params.contains(Param.MATURITY_RATING_ICON)) {
            ratingIcon = context.getDrawable(R.drawable.ic_android);
        }
        if (params.contains(Param.MATURITY_RATING_STRING)) {
            rating = "everyone";
        }
        if (params.contains(Param.MONETIZATION_NOTICE)) {
            monetizationNotice = "Uses in-app purchases";
        }
        if (params.contains(Param.DEVELOPER_TITLE)) {
            developerTitle = "Instant Apps Inc.";
        }
        if (params.contains(Param.DEVELOPER_EMAIL)) {
            developerEmail = "developer@instant-apps.com";
        }
        if (params.contains(Param.DEVELOPER_MAILING_ADDRESS)) {
            developerMailingAddress = "1 Main Street, Somewhere, CA, 94043";
        }

        if (params.contains(Param.PRIVACY_POLICY)) {
            try {
                privacyPolicy = new URL("https://test.com/privacy");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        if (params.contains(Param.DEVELOPER_WEBSITE)) {
            try {
                developerWebsite = new URL("https://test.com");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return InstantAppDetails.builder()
                .maturityRatingIcon(ratingIcon)
                .maturityRatingString(rating)
                .monetizationNotice(monetizationNotice)
                .developerTitle(developerTitle)
                .privacyPolicy(privacyPolicy)
                .developerWebsite(developerWebsite)
                .developerEmail(developerEmail)
                .developerMailingAddress(developerMailingAddress)
                .build();
    }

    /**
     * Convenience method to create an InstantAppDetails with all non-null members.
     *
     * @param context a required Context for loading a test maturity rating icon
     * @return InstantAppDetails
     */
    public static InstantAppDetails build(Context context) {
        return build(context, EnumSet.allOf(Param.class));
    }
}
