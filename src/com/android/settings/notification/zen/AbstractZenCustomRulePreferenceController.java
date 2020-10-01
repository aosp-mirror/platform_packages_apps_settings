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

import android.app.AutomaticZenRule;
import android.content.Context;
import android.os.Bundle;
import android.util.Slog;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

abstract class AbstractZenCustomRulePreferenceController extends
        AbstractZenModePreferenceController implements PreferenceControllerMixin {

    String mId;
    AutomaticZenRule mRule;

    AbstractZenCustomRulePreferenceController(Context context, String key,
            Lifecycle lifecycle) {
        super(context, key, lifecycle);
    }

    @Override
    public void updateState(Preference preference) {
        if (mId != null) {
            mRule = mBackend.getAutomaticZenRule(mId);
        }
    }

    @Override
    public boolean isAvailable() {
        return mRule != null;
    }

    public void onResume(AutomaticZenRule rule, String id) {
        mId = id;
        mRule = rule;
    }

    Bundle createBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(ZenCustomRuleSettings.RULE_ID, mId);
        return bundle;
    }
}
