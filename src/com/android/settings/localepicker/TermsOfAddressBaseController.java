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
import com.android.settings.widget.TickButtonPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class TermsOfAddressBaseController extends BasePreferenceController {

    private static final Executor sExecutor = Executors.newSingleThreadExecutor();
    private PreferenceScreen mPreferenceScreen;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private TickButtonPreference mPreference;
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
        mPreferenceScreen = screen;
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnPreferenceClickListener(clickedPref -> {
            sExecutor.execute(
                    () -> {
                        mTermsOfAddressHelper.setSystemGrammaticalGender(
                                getGrammaticalGenderType());
                    });
            setSelected(mPreference);
            mMetricsFeatureProvider.action(mContext, getMetricsActionKey());
            return true;
        });
        updatePreferences();
    }

    private void setSelected(TickButtonPreference preference) {
        for (int i = 1; i < mPreferenceScreen.getPreferenceCount(); i++) {
            TickButtonPreference pref = (TickButtonPreference) mPreferenceScreen.getPreference(i);
            pref.setSelected(pref.getKey().equals(preference.getKey()));
        }
    }

    private void updatePreferences() {
        if (mPreference == null) {
            return;
        }
        mPreference.setSelected(
                mTermsOfAddressHelper.getSystemGrammaticalGender() == getGrammaticalGenderType());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    protected abstract int getMetricsActionKey();

    protected abstract int getGrammaticalGenderType();
}
