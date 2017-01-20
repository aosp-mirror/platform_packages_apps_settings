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

package com.android.settings.search2;

import android.annotation.IntDef;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A interface for search results types. Examples include Inline results, third party apps
 * or any future possibilities.
 */
public abstract class ResultPayload implements Parcelable {

    @IntDef({PayloadType.INLINE_SLIDER, PayloadType.INLINE_SWITCH,
            PayloadType.INTENT, PayloadType.SAVED_QUERY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PayloadType {
        /**
         * Resulting page will be started using an intent
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
         * Result is a recently saved query.
         */
        int SAVED_QUERY = 3;
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


    @ResultPayload.PayloadType
    public abstract int getType();
}
