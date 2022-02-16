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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Contacts;
import android.service.notification.ZenPolicy;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleStarredContactsPreferenceController extends
        AbstractZenCustomRulePreferenceController implements Preference.OnPreferenceClickListener {

    private Preference mPreference;
    private final @ZenPolicy.PriorityCategory int mPriorityCategory;
    private final PackageManager mPackageManager;

    private Intent mStarredContactsIntent;
    private Intent mFallbackIntent;

    public ZenRuleStarredContactsPreferenceController(Context context, Lifecycle lifecycle,
            @ZenPolicy.PriorityCategory int priorityCategory, String key) {
        super(context, key, lifecycle);
        mPriorityCategory = priorityCategory;
        mPackageManager = mContext.getPackageManager();

        mStarredContactsIntent = new Intent(Contacts.Intents.UI.LIST_STARRED_ACTION);

        mFallbackIntent =  new Intent(Intent.ACTION_MAIN);
        mFallbackIntent.addCategory(Intent.CATEGORY_APP_CONTACTS);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);

        if (mPreference != null) {
            mPreference.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable() || mRule.getZenPolicy() == null || !isIntentValid()) {
            return false;
        }

        if (mPriorityCategory == ZenPolicy.PRIORITY_CATEGORY_CALLS) {
            return mRule.getZenPolicy().getPriorityCallSenders() == ZenPolicy.PEOPLE_TYPE_STARRED;
        } else if (mPriorityCategory == ZenPolicy.PRIORITY_CATEGORY_MESSAGES) {
            return mRule.getZenPolicy().getPriorityMessageSenders()
                    == ZenPolicy.PEOPLE_TYPE_STARRED;
        } else {
            // invalid category
            return false;
        }
    }

    @Override
    public CharSequence getSummary() {
        return mBackend.getStarredContactsSummary(mContext);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mStarredContactsIntent.resolveActivity(mPackageManager) != null) {
            mContext.startActivity(mStarredContactsIntent);
        } else {
            mContext.startActivity(mFallbackIntent);
        }
        return true;
    }

    private boolean isIntentValid() {
        return mStarredContactsIntent.resolveActivity(mPackageManager) != null
                || mFallbackIntent.resolveActivity(mPackageManager) != null;
    }
}
