/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.MainSwitchPreference;

public class ZenAutomaticRuleSwitchPreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController implements
        OnCheckedChangeListener {

    private static final String KEY = "zen_automatic_rule_switch";
    private AutomaticZenRule mRule;
    private String mId;
    private MainSwitchPreference mSwitchBar;

    public ZenAutomaticRuleSwitchPreferenceController(Context context, Fragment parent,
            Lifecycle lifecycle) {
        super(context, KEY, parent, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    void setIdAndRule(String id, AutomaticZenRule rule) {
        mId = id;
        mRule = rule;
    }

    @Override
    public boolean isAvailable() {
        return mRule != null && mId != null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY);
        mSwitchBar = (MainSwitchPreference) pref;

        if (mSwitchBar != null) {
            mSwitchBar.setTitle(mContext.getString(R.string.zen_mode_use_automatic_rule));
            try {
                pref.setOnPreferenceClickListener(preference -> {
                    mRule.setEnabled(!mRule.isEnabled());
                    mBackend.updateZenRule(mId, mRule);
                    return true;
                });
                mSwitchBar.addOnSwitchChangeListener(this);
            } catch (IllegalStateException e) {
                // an exception is thrown if you try to add the listener twice
            }
        }
    }

    public void updateState(Preference preference) {
        if (mRule != null) {
            mSwitchBar.updateStatus(mRule.isEnabled());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final boolean enabled = isChecked;
        if (enabled == mRule.isEnabled()) return;
        mRule.setEnabled(enabled);
        mBackend.updateZenRule(mId, mRule);
    }
}
