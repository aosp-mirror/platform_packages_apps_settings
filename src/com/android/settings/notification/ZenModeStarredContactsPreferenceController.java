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
import android.database.Cursor;
import android.icu.text.ListFormatter;
import android.provider.Contacts;
import android.provider.ContactsContract;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class ZenModeStarredContactsPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceClickListener {

    protected static final String KEY = "zen_mode_starred_contacts";
    private Preference mPreference;
    private final int mPriorityCategory;
    private final PackageManager mPackageManager;

    @VisibleForTesting
    Intent mStarredContactsIntent;
    @VisibleForTesting
    Intent mFallbackIntent;

    public ZenModeStarredContactsPreferenceController(Context context, Lifecycle lifecycle, int
            priorityCategory) {
        super(context, KEY, lifecycle);
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
        mPreference.setOnPreferenceClickListener(this);
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
    public void updateState(Preference preference) {
        super.updateState(preference);

        List<String> starredContacts = getStarredContacts();
        int numStarredContacts = starredContacts.size();

        List<String> displayContacts = new ArrayList<>();

        if (numStarredContacts == 0) {
            displayContacts.add(mContext.getString(R.string.zen_mode_from_none));
        } else {
            for (int i = 0; i < 2 && i < numStarredContacts; i++) {
                displayContacts.add(starredContacts.get(i));
            }

            if (numStarredContacts == 3) {
                displayContacts.add(starredContacts.get(2));
            } else if (numStarredContacts > 2) {
                displayContacts.add(mContext.getResources().getQuantityString(
                        R.plurals.zen_mode_starred_contacts_summary_additional_contacts,
                        numStarredContacts - 2, numStarredContacts - 2));
            }
        }

        mPreference.setSummary(ListFormatter.getInstance().format(displayContacts));
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

    private List<String> getStarredContacts() {
        List<String> starredContacts = new ArrayList<>();

        Cursor cursor = mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                ContactsContract.Data.STARRED + "=1", null,
                ContactsContract.Data.TIMES_CONTACTED);

        if (cursor.moveToFirst()) {
            do {
                starredContacts.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        return starredContacts;
    }

    private boolean isIntentValid() {
        return mStarredContactsIntent.resolveActivity(mPackageManager) != null
                || mFallbackIntent.resolveActivity(mPackageManager) != null;
    }
}
