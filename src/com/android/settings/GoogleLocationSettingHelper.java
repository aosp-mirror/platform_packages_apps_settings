/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.settings;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Helper class to read and write the 'Use My Location' setting used by Google Apps (e.g. GoogleQSB,
 * VoiceSearch).
 *
 * This class duplicates a small amount of functionality from GSF (Google Services Framework) to
 * allow the open source Settings app to interface to the 'Use My Location' setting owned by GSF.
 */
public class GoogleLocationSettingHelper {

    private static final String TAG = "GoogleLocationSettingHelper";

    /**
     * User has disagreed to use location for Google services.
     */
    public static final int USE_LOCATION_FOR_SERVICES_OFF = 0;

    /**
     * User has agreed to use location for Google services.
     */
    public static final int USE_LOCATION_FOR_SERVICES_ON = 1;

    /**
     * The user has neither agreed nor disagreed to use location for Google services yet.
     */
    public static final int USE_LOCATION_FOR_SERVICES_NOT_SET = 2;

    private static final String GOOGLE_SETTINGS_AUTHORITY = "com.google.settings";
    private static final Uri GOOGLE_SETTINGS_CONTENT_URI =
        Uri.parse("content://" + GOOGLE_SETTINGS_AUTHORITY + "/partner");
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String USE_LOCATION_FOR_SERVICES = "use_location_for_services";

    private static final String ACTION_SET_USE_LOCATION_FOR_SERVICES =
        "com.google.android.gsf.action.SET_USE_LOCATION_FOR_SERVICES";
    public static final String EXTRA_DISABLE_USE_LOCATION_FOR_SERVICES = "disable";

    /**
     * Determine if the 'Use My Location' setting is applicable on this device, i.e. if the
     * activity used to enabled/disable it is present.
     */
    public static boolean isAvailable(Context context) {
        ResolveInfo ri = context.getPackageManager().resolveActivity(getSetUseLocationIntent(),
                PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

    private static Intent getSetUseLocationIntent() {
        Intent i = new Intent(ACTION_SET_USE_LOCATION_FOR_SERVICES);
        return i;
    }

    /**
     * Get the current value for the 'Use value for location' setting.
     * @return One of {@link #USE_LOCATION_FOR_SERVICES_NOT_SET},
     *      {@link #USE_LOCATION_FOR_SERVICES_OFF} or {@link #USE_LOCATION_FOR_SERVICES_ON}.
     */
    public static int getUseLocationForServices(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor c = null;
        String stringValue = null;
        try {
            c = resolver.query(GOOGLE_SETTINGS_CONTENT_URI, new String[] { VALUE }, NAME + "=?",
                    new String[] { USE_LOCATION_FOR_SERVICES }, null);
            if (c != null && c.moveToNext()) {
                stringValue = c.getString(0);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to get 'Use My Location' setting", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (stringValue == null) {
            return USE_LOCATION_FOR_SERVICES_NOT_SET;
        }
        int value;
        try {
            value = Integer.parseInt(stringValue);
        } catch (NumberFormatException nfe) {
            value = USE_LOCATION_FOR_SERVICES_NOT_SET;
        }
        return value;
    }

    /**
     * Change the value of the 'Use My Location' setting. This launches a GSF activity which has
     * the permissions to actually make the change, prompting the user if necessary.
     */
    public static void setUseLocationForServices(Context context, boolean use) {
        Intent i = getSetUseLocationIntent();
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(EXTRA_DISABLE_USE_LOCATION_FOR_SERVICES, !use);
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e("GoogleLocationSettingHelper", "Problem while starting GSF location activity");
        }
    }

}
