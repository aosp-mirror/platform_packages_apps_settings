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
 *
 */

package com.android.settings.search;

import android.annotation.IntDef;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A interface for search results types. Examples include Inline results, third party apps
 * or any future possibilities.
 */
public class ResultPayload implements Parcelable {
    protected final Intent mIntent;

    @IntDef({PayloadType.INTENT, PayloadType.INLINE_SLIDER, PayloadType.INLINE_SWITCH,
            PayloadType.INLINE_LIST, PayloadType.SAVED_QUERY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PayloadType {
        /**
         * Resulting page will be started using an mIntent
         */
        int INTENT = 0;

        /**
         * Result is a inline widget, using a slider widget as UI.
         */
        int INLINE_SLIDER = 1;

        /**
         * Result is a inline widget, using a toggle widget as UI.
         */
        int INLINE_SWITCH = 2;

        /**
         * Result is an inline list-select, with an undefined UI.
         */
        int INLINE_LIST = 3;

        /**
         * Result is a recently saved query.
         */
        int SAVED_QUERY = 4;
    }

    /**
     * Enumerates the possible values for the Availability of a setting.
     */
    @IntDef({Availability.AVAILABLE,
            Availability.DISABLED_DEPENDENT_SETTING,
            Availability.DISABLED_DEPENDENT_APP,
            Availability.DISABLED_UNSUPPORTED,
            Availability.RESOURCE_CONTENTION,
            Availability.INTENT_ONLY,
            Availability.DISABLED_FOR_USER,})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Availability {
        /**
         * The setting is available.
         */
        int AVAILABLE = 0;

        /**
         * The setting has a dependency in settings app which is currently disabled, blocking
         * access.
         */
        int DISABLED_DEPENDENT_SETTING = 1;

        /**
         * The setting is not supported by the device.
         */
        int DISABLED_UNSUPPORTED = 2;

        /**
         * The setting you are trying to change is being used by another application and cannot
         * be changed until it is released by said application.
         */
        int RESOURCE_CONTENTION = 3;

        /**
         * The setting is disabled because corresponding app is disabled.
         */
        int DISABLED_DEPENDENT_APP = 4;

        /**
         * This setting is supported on the device but cannot be changed inline.
         */
        int INTENT_ONLY = 5;

        /**
         * The setting cannot be changed by the current user.
         * ex: MobileNetworkTakeMeThereSetting should not be available to a secondary user.
         */
        int DISABLED_FOR_USER = 6;
    }

    @IntDef({SettingsSource.UNKNOWN, SettingsSource.SYSTEM, SettingsSource.SECURE,
            SettingsSource.GLOBAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SettingsSource {
        int UNKNOWN = 0;
        int SYSTEM = 1;
        int SECURE = 2;
        int GLOBAL = 3;
    }


    private ResultPayload(Parcel in) {
        mIntent = in.readParcelable(ResultPayload.class.getClassLoader());
    }

    public ResultPayload(Intent intent) {
        mIntent = intent;
    }

    @ResultPayload.PayloadType
    public int getType() {
        return PayloadType.INTENT;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mIntent, flags);
    }

    public static final Creator<ResultPayload> CREATOR = new Creator<ResultPayload>() {
        @Override
        public ResultPayload createFromParcel(Parcel in) {
            return new ResultPayload(in);
        }

        @Override
        public ResultPayload[] newArray(int size) {
            return new ResultPayload[size];
        }
    };

    public Intent getIntent() {
        return mIntent;
    }
}
