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
import android.widget.Switch;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

public class ZenAutomaticRuleSwitchPreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController implements
        SwitchBar.OnSwitchChangeListener {

    private static final String KEY = "zen_automatic_rule_switch";
    private AutomaticZenRule mRule;
    private String mId;
    private SwitchBar mSwitchBar;

    public ZenAutomaticRuleSwitchPreferenceController(Context context, Fragment parent,
            Lifecycle lifecycle) {
        super(context, KEY, parent, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return mRule != null && mId != null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference pref = screen.findPreference(KEY);
        mSwitchBar = pref.findViewById(R.id.switch_bar);

        if (mSwitchBar != null) {
            mSwitchBar.setSwitchBarText(R.string.zen_mode_use_automatic_rule,
                    R.string.zen_mode_use_automatic_rule);
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
            mSwitchBar.show();
        }
    }

    public void onResume(AutomaticZenRule rule, String id) {
        mRule = rule;
        mId = id;
    }

    public void updateState(Preference preference) {
        if (mRule != null) {
            mSwitchBar.setChecked(mRule.isEnabled());
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        final boolean enabled = isChecked;
        if (enabled == mRule.isEnabled()) return;
        mRule.setEnabled(enabled);
        mBackend.updateZenRule(mId, mRule);
    }
}
