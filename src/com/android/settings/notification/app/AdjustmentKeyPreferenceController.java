/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.notification.app;

import android.app.Flags;
import android.content.Context;
import android.service.notification.Adjustment;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

/**
 * Used for the app-level preference screen to opt the app in or out of a provided Adjustment key.
 * E.g. to say an app can or cannot be classified by the NotificationAssistantService.
 */
public class AdjustmentKeyPreferenceController extends
        NotificationPreferenceController implements Preference.OnPreferenceChangeListener {
    private String mKey;

    public AdjustmentKeyPreferenceController(@NonNull Context context,
            @NonNull NotificationBackend backend, String key) {
        super(context, backend);
        mKey = key;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public boolean isAvailable() {
        if (!(Flags.notificationClassificationUi() || Flags.nmSummarizationUi()
                || Flags.nmSummarization())) {
            return false;
        }
        boolean isBundlePref = Adjustment.KEY_TYPE.equals(mKey);
        boolean isSummarizePref = Adjustment.KEY_SUMMARIZATION.equals(mKey);
        if (!Flags.notificationClassificationUi() && isBundlePref) {
            return false;
        }
        if (!(Flags.nmSummarizationUi() || Flags.nmSummarization()) && isSummarizePref) {
            return false;
        }
        if (!isSummarizePref && !isBundlePref) {
            return false;
        }
        if (isSummarizePref && !(mBackend.hasSentValidMsg(mAppRow.pkg, mAppRow.uid)
                || mBackend.isInInvalidMsgState(mAppRow.pkg, mAppRow.uid))) {
            return false;
        }
        return super.isAvailable();
    }

    @Override
    boolean isIncludedInFilter() {
        // not a channel-specific preference; only at the app level
        return false;
    }

    public void updateState(@NonNull Preference preference) {
        RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
        if (pref.getParent() != null) {
            pref.getParent().setVisible(true);
        }

        if (pref != null && mAppRow != null) {
            pref.setDisabledByAdmin(mAdmin);
            pref.setEnabled(!pref.isDisabledByAdmin());
            pref.setChecked(mBackend.getAllowedAssistantAdjustments(mAppRow.pkg).contains(mKey));
            pref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        final boolean allowedForPkg = (Boolean) newValue;
        mBackend.setAdjustmentSupportedForPackage(mKey, mAppRow.pkg, allowedForPkg);
        return true;
    }
}
