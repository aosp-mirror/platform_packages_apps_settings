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

package com.android.settings.notification;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.utils.ZenServiceListing;

public class ZenModeAddAutomaticRulePreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController implements
        Preference.OnPreferenceClickListener {

    private final String KEY_ADD_RULE;
    private final ZenServiceListing mZenServiceListing;

    public ZenModeAddAutomaticRulePreferenceController(Context context, String key,
            Fragment parent, ZenServiceListing serviceListing) {
        super(context, parent);
        KEY_ADD_RULE = key;
        mZenServiceListing = serviceListing;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ADD_RULE;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference pref = screen.findPreference(KEY_ADD_RULE);
        pref.setPersistent(false);
        pref.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        new ZenRuleSelectionDialog(mContext, mZenServiceListing) {
            @Override
            public void onSystemRuleSelected(ZenRuleInfo ri) {
                showNameRuleDialog(ri);
            }

            @Override
            public void onExternalRuleSelected(ZenRuleInfo ri) {
                Intent intent = new Intent().setComponent(ri.configurationActivity);
                mParent.startActivity(intent);
            }
        }.show();
        return true;
    }
}
