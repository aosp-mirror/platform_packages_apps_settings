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

package com.android.settings.notification;

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Contacts;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeStarredContactsPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceClickListener {
    private Preference mPreference;
    private final int mPriorityCategory;
    private final PackageManager mPackageManager;

    private Intent mStarredContactsIntent;
    private Intent mFallbackIntent;

    public ZenModeStarredContactsPreferenceController(Context context, Lifecycle lifecycle, int
            priorityCategory, String key) {
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
        if (mPriorityCategory == PRIORITY_CATEGORY_CALLS) {
            return mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_CALLS)
                    && mBackend.getPriorityCallSenders() == PRIORITY_SENDERS_STARRED
                    && isIntentValid();
        } else if (mPriorityCategory == PRIORITY_CATEGORY_MESSAGES) {
            return mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_MESSAGES)
                    && mBackend.getPriorityMessageSenders() == PRIORITY_SENDERS_STARRED
                    && isIntentValid();
        } else {
            // invalid category
            return false;
        }
    }

    @Override
    public CharSequence getSummary() {
        return mBackend.getStarredContactsSummary();
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
