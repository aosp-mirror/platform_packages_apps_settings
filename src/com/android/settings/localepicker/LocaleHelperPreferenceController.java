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

package com.android.settings.localepicker;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.FooterPreference;

/**
 * A controller to update current locale information of application.
 */
public class LocaleHelperPreferenceController extends AbstractPreferenceController {
    private static final String TAG = LocaleHelperPreferenceController.class.getSimpleName();

    private static final String KEY_FOOTER_LANGUAGE_PICKER = "footer_languages_picker";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public LocaleHelperPreferenceController(Context context) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_FOOTER_LANGUAGE_PICKER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        FooterPreference footerPreference = screen.findPreference(getPreferenceKey());
        updateFooterPreference(footerPreference);
    }

    @VisibleForTesting
    void updateFooterPreference(FooterPreference footerPreference) {
        if (footerPreference != null) {
            footerPreference.setLearnMoreAction(v -> openLocaleLearnMoreLink());
            footerPreference.setLearnMoreText(mContext.getString(
                    R.string.desc_locale_helper_footer_general));
        }
    }

    private void openLocaleLearnMoreLink() {
        Intent intent = HelpUtils.getHelpIntent(
                mContext,
                mContext.getString(R.string.link_locale_picker_footer_learn_more),
                mContext.getClass().getName());
        if (intent != null) {
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_LANGUAGES_LEARN_MORE);
            mContext.startActivity(intent);
        } else {
            Log.w(TAG, "HelpIntent is null");
        }
    }
}
