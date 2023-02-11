/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.localepicker.LocaleFeatureProviderImpl;

/** A controller for the entry of Numbering System's page */
public class NumberingSystemController extends BasePreferenceController {
    private static final String TAG = NumberingSystemController.class.getSimpleName();

    public NumberingSystemController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * @return {@link AvailabilityStatus} for the Setting. This status is used to determine if the
     * Setting should be shown or disabled in Settings. Further, it can be used to produce
     * appropriate error / warning Slice in the case of unavailability.
     * </p>
     * The status is used for the convenience methods: {@link #isAvailable()}, {@link
     * #isSupported()}
     * </p>
     * The inherited class doesn't need to check work profile if android:forWork="true" is set in
     * preference xml.
     */
    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return new LocaleFeatureProviderImpl().getLocaleNames();
    }
}
