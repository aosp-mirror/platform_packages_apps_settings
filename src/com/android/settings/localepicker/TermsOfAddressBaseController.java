/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.localepicker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class TermsOfAddressBaseController extends BasePreferenceController {

    private static final Executor sExecutor = Executors.newSingleThreadExecutor();
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private SelectorWithWidgetPreference mPreference;

    private TermsOfAddressHelper mTermsOfAddressHelper;

    public TermsOfAddressBaseController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    public void setTermsOfAddressHelper(@NonNull TermsOfAddressHelper termsOfAddressHelper) {
        mTermsOfAddressHelper = termsOfAddressHelper;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setChecked(
                mTermsOfAddressHelper.getSystemGrammaticalGender() == getGrammaticalGenderType());
        mPreference.setOnClickListener(v -> {
            sExecutor.execute(
                    () -> {
                        mTermsOfAddressHelper.setSystemGrammaticalGender(
                                getGrammaticalGenderType());
                    });
            mMetricsFeatureProvider.action(mContext, getMetricsActionKey());
        });
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    protected abstract int getMetricsActionKey();

    protected abstract int getGrammaticalGenderType();
}
