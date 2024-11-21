/**
 * Copyright (C) 2024 The Android Open Source Project
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
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

/**
 * Preference controller for regional preference footer.
 */
public class NewRegionalFooterPreferenceController extends BasePreferenceController {

    private static final String TAG = "NewRegionalFooterPreferenceController";

    public NewRegionalFooterPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.regionalPreferencesApiEnabled()) {
            return AVAILABLE_UNSEARCHABLE;
        }


        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        FooterPreference footerPreference = screen.findPreference(getPreferenceKey());
        setupFooterPreference(footerPreference);
    }

    @VisibleForTesting
    void setupFooterPreference(FooterPreference footerPreference) {
        if (footerPreference != null) {
            footerPreference.setLearnMoreAction(v -> openLocaleLearnMoreLink());
            footerPreference.setLearnMoreText(mContext.getString(
                    R.string.desc_regional_pref_footer_learn_more));
        }
    }

    private void openLocaleLearnMoreLink() {
        Intent intent = HelpUtils.getHelpIntent(
                mContext,
                mContext.getString(R.string.regional_pref_footer_learn_more_link),
                mContext.getClass().getName());
        if (intent != null) {
            mContext.startActivity(intent);
        } else {
            Log.w(TAG, "HelpIntent is null");
        }
    }
}
