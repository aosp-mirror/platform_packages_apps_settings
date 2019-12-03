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

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeAddAutomaticRulePreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController implements
        Preference.OnPreferenceClickListener {

    protected static final String KEY = "zen_mode_add_automatic_rule";
    private final ZenServiceListing mZenServiceListing;

    public ZenModeAddAutomaticRulePreferenceController(Context context, Fragment parent,
            ZenServiceListing serviceListing, Lifecycle lifecycle) {
        super(context, KEY, parent, lifecycle);
        mZenServiceListing = serviceListing;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference pref = screen.findPreference(KEY);
        pref.setPersistent(false);
        pref.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        ZenRuleSelectionDialog.show(mContext, mParent, new RuleSelectionListener(),
                mZenServiceListing);
        return true;
    }

    public class RuleSelectionListener implements ZenRuleSelectionDialog.PositiveClickListener {
        public RuleSelectionListener() {}

        @Override
        public void onSystemRuleSelected(ZenRuleInfo ri, Fragment parent) {
            showNameRuleDialog(ri, parent);
        }

        @Override
        public void onExternalRuleSelected(ZenRuleInfo ri, Fragment parent) {
            Intent intent = new Intent().setComponent(ri.configurationActivity);
            parent.startActivity(intent);
        }
    }
}
