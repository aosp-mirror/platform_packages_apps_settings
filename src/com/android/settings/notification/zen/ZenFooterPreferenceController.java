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

import android.app.NotificationManager.Policy;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenFooterPreferenceController extends AbstractZenModePreferenceController {

    public ZenFooterPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return mBackend.mPolicy.suppressedVisualEffects == 0
                || Policy.areAllVisualEffectsSuppressed(mBackend.mPolicy.suppressedVisualEffects);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (mBackend.mPolicy.suppressedVisualEffects == 0) {
           preference.setTitle(R.string.zen_mode_restrict_notifications_mute_footer);
        } else if (Policy.areAllVisualEffectsSuppressed(mBackend.mPolicy.suppressedVisualEffects)) {
            preference.setTitle(R.string.zen_mode_restrict_notifications_hide_footer);
        } else {
            preference.setTitle(null);
        }
    }

    protected void hide(PreferenceScreen screen) {
        setVisible(screen, getPreferenceKey(), false /* visible */);
    }
}
