/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.users;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/** Test class, to demonstrate the features. TODO: Remove or modify with real restrictions */
public class RestrictionsReceiver extends BroadcastReceiver {

    private static final String TAG = RestrictionsReceiver.class.getSimpleName();

    public static final String KEY_VERSION = "version";
    public static final String KEY_ENABLE_APPS = "enable_apps";
    public static final String KEY_SECTIONS_TO_SHOW = "enable_sections";
    public static final String KEY_CONTENT_RATING = "content_rating";

    private static final int[] SECTION_IDS = {
        R.id.wifi_settings,
        R.id.bluetooth_settings,
        R.id.data_usage_settings,
        R.id.app_settings,
        R.id.date_time_settings,
        R.id.about_settings
    };

    private static final int[] SECTION_TITLE_IDS = {
        R.string.wifi_settings,
        R.string.bluetooth_settings,
        R.string.data_usage_summary_title,
        R.string.manageapplications_settings_title,
        R.string.date_and_time,
        R.string.about_settings
    };

    @Override
    public void onReceive(final Context context, Intent intent) {
        final PendingResult result = goAsync();
        final ArrayList<RestrictionEntry> oldRestrictions =
                intent.getParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS);
        Log.i(TAG, "oldRestrictions = " + oldRestrictions);
        new Thread() {
            public void run() {
                createRestrictions(context, result, oldRestrictions);
            }
        }.start();
    }

    private void createRestrictions(Context context,
            PendingResult result, List<RestrictionEntry> old) {
        ArrayList<RestrictionEntry> newRestrictions = new ArrayList<RestrictionEntry>();
        boolean oldEnableApps = false;
        String oldContentRating = "";
        String[] oldEnabledSections = new String[0];
        if (old != null) {
            for (RestrictionEntry r : old) {
                if (r.getKey().equals(KEY_ENABLE_APPS)) {
                    oldEnableApps = r.getSelectedState();
                } else if (r.getKey().equals(KEY_CONTENT_RATING)) {
                    oldContentRating = r.getSelectedString();
                } else if (r.getKey().equals(KEY_SECTIONS_TO_SHOW)) {
                    oldEnabledSections = r.getAllSelectedStrings();
                }
            }
        }

        RestrictionEntry r0 = new RestrictionEntry(KEY_VERSION, "1");
        newRestrictions.add(r0);

        RestrictionEntry r1 = new RestrictionEntry(KEY_ENABLE_APPS,
                Boolean.toString(oldEnableApps));
        r1.setTitle("Enable apps");
        r1.setDescription("Show the Apps section in Settings");
        r1.setType(RestrictionEntry.TYPE_BOOLEAN);
        newRestrictions.add(r1);

        RestrictionEntry r2 = new RestrictionEntry(KEY_CONTENT_RATING, oldContentRating);
        r2.setTitle("Test: Content rating");
        r2.setDescription("Limit content to chosen rating and lower");
        r2.setType(RestrictionEntry.TYPE_CHOICE_LEVEL);
        r2.setChoiceValues(new String[] { "G", "PG", "PG13", "R", "NR"});
        r2.setChoiceEntries(new String[] { "G", "PG", "PG-13", "Restricted", "Not Rated" });
        newRestrictions.add(r2);

        String [] values = new String[SECTION_IDS.length];
        String [] choices = new String[SECTION_IDS.length];
        int i = 0;
        for (int sectionId : SECTION_IDS) {
            values[i] = Integer.toString(sectionId);
            choices[i] = context.getString(SECTION_TITLE_IDS[i]);
            i++;
        }
        RestrictionEntry r3 = new RestrictionEntry(KEY_SECTIONS_TO_SHOW, oldEnabledSections);
        r3.setType(RestrictionEntry.TYPE_MULTI_SELECT);
        r3.setChoiceEntries(choices);
        r3.setChoiceValues(values);
        r3.setTitle("Test: Sections to show");
        newRestrictions.add(r3);

        Bundle extras = new Bundle();
        extras.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS, newRestrictions);
        result.setResult(0, null, extras);
        result.finish();
    }
}
