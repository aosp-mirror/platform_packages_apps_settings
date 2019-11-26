/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleNotifFooterPreferenceController extends
        AbstractZenCustomRulePreferenceController {

    public ZenRuleNotifFooterPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable() || mRule.getZenPolicy() == null) {
            return false;
        }


        return mRule.getZenPolicy().shouldHideAllVisualEffects()
                || mRule.getZenPolicy().shouldShowAllVisualEffects();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mRule == null || mRule.getZenPolicy() == null) {
            return;
        }

        if (mRule.getZenPolicy().shouldShowAllVisualEffects()) {
            preference.setTitle(R.string.zen_mode_restrict_notifications_mute_footer);
        } else if (mRule.getZenPolicy().shouldHideAllVisualEffects()) {
            preference.setTitle(R.string.zen_mode_restrict_notifications_hide_footer);
        } else {
            preference.setTitle(null);
        }
    }
}
