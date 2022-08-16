/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.enterprise;

import android.content.Context;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

import java.util.Objects;

/** Preference controller which displays a financed preference for financed devices. */
public class FinancedPrivacyPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin {

    private final PrivacyPreferenceControllerHelper mPrivacyPreferenceControllerHelper;

    public FinancedPrivacyPreferenceController(Context context, String key) {
        this(Objects.requireNonNull(context), new PrivacyPreferenceControllerHelper(context), key);
    }

    @VisibleForTesting
    FinancedPrivacyPreferenceController(Context context,
            PrivacyPreferenceControllerHelper privacyPreferenceControllerHelper, String key) {
        super(Objects.requireNonNull(context), key);
        mPrivacyPreferenceControllerHelper = Objects.requireNonNull(
                privacyPreferenceControllerHelper);
    }

    @Override
    public void updateState(Preference preference) {
        mPrivacyPreferenceControllerHelper.updateState(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return mPrivacyPreferenceControllerHelper.isFinancedDevice()
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }
}
