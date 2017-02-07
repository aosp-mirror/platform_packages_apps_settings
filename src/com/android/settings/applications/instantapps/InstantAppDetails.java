/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.instantapps;

import android.graphics.drawable.Drawable;
import java.net.URL;

/**
 * Encapsulates state about instant apps that is provided by an app store implementation.
 */
public class InstantAppDetails {

    // Most of these members are self-explanatory; the one that may not be is
    // monetizationNotice, which is a string alerting users that the app contains ads and/or uses
    // in-app purchases (this may eventually become two separate members).
    public final Drawable maturityRatingIcon;
    public final String maturityRatingString;
    public final String monetizationNotice;
    public final String developerTitle;
    public final URL privacyPolicy;
    public final URL developerWebsite;
    public final String developerEmail;
    public final String developerMailingAddress;

    public static class Builder {
        private Drawable mMaturityRatingIcon;
        private String mMaturityRatingString;
        private String mMonetizationNotice;
        private String mDeveloperTitle;
        private URL mPrivacyPolicy;
        private URL mDeveloperWebsite;
        private String mDeveloperEmail;
        private String mDeveloperMailingAddress;

        public Builder maturityRatingIcon(Drawable maturityRatingIcon) {
            this.mMaturityRatingIcon = maturityRatingIcon;
            return this;
        }

        public Builder maturityRatingString(String maturityRatingString) {
            mMaturityRatingString = maturityRatingString;
            return this;
        }

        public Builder monetizationNotice(String monetizationNotice) {
            mMonetizationNotice = monetizationNotice;
            return this;
        }

        public Builder developerTitle(String developerTitle) {
            mDeveloperTitle = developerTitle;
            return this;
        }

        public Builder privacyPolicy(URL privacyPolicy) {
            mPrivacyPolicy = privacyPolicy;
            return this;
        }

        public Builder developerWebsite(URL developerWebsite) {
            mDeveloperWebsite = developerWebsite;
            return this;
        }

        public Builder developerEmail(String developerEmail) {
            mDeveloperEmail = developerEmail;
            return this;
        }

        public Builder developerMailingAddress(String developerMailingAddress) {
            mDeveloperMailingAddress = developerMailingAddress;
            return this;
        }

        public InstantAppDetails build() {
            return new InstantAppDetails(mMaturityRatingIcon, mMaturityRatingString,
                    mMonetizationNotice, mDeveloperTitle, mPrivacyPolicy, mDeveloperWebsite,
                    mDeveloperEmail, mDeveloperMailingAddress);
        }
    }

    public static Builder builder() { return new Builder(); }

    private InstantAppDetails(Drawable maturityRatingIcon, String maturityRatingString,
            String monetizationNotice, String developerTitle, URL privacyPolicy,
            URL developerWebsite, String developerEmail, String developerMailingAddress) {
        this.maturityRatingIcon = maturityRatingIcon;
        this.maturityRatingString = maturityRatingString;
        this.monetizationNotice = monetizationNotice;
        this.developerTitle = developerTitle;
        this.privacyPolicy = privacyPolicy;
        this.developerWebsite = developerWebsite;
        this.developerEmail = developerEmail;
        this.developerMailingAddress = developerMailingAddress;
    }
}
