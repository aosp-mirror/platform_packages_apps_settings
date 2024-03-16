/*
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

package com.android.settings.privatespace;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

/** Preference controller for private space settings footer. */
public class PrivateSpaceFooterPreferenceController extends BasePreferenceController {
    public PrivateSpaceFooterPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        FooterPreference preference = screen.findPreference(getPreferenceKey());
        setupFooter(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return android.multiuser.Flags.enablePrivateSpaceFeatures()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @VisibleForTesting
    void setupFooter(FooterPreference preference) {
        final String helpUri = mContext.getString(R.string.private_space_learn_more_url);
        if (!TextUtils.isEmpty(helpUri) && preference != null) {
            preference.setLearnMoreAction(
                    v -> {
                        mContext.startActivity(
                                HelpUtils.getHelpIntent(
                                        mContext, helpUri, /* backupContext= */ ""));
                    });
            preference.setLearnMoreText(mContext.getString(R.string.private_space_learn_more_text));
        }
    }
}
