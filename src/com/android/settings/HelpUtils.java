/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import java.util.Locale;

/**
 * Functions to easily prepare contextual help menu option items with an intent that opens up the
 * browser to a particular URL, while taking into account the preferred language and app version.
 */
public class HelpUtils {
    private final static String TAG = HelpUtils.class.getName();

    /**
     * Help URL query parameter key for the preferred language.
     */
    private final static String PARAM_LANGUAGE_CODE = "hl";

    /**
     * Help URL query parameter key for the app version.
     */
    private final static String PARAM_VERSION = "version";

    /**
     * Cached version code to prevent repeated calls to the package manager.
     */
    private static String sCachedVersionCode = null;

    /** Static helper that is not instantiable*/
    private HelpUtils() { }

    /**
     * Prepares the help menu item by doing the following.
     * - If the string corresponding to the helpUrlResourceId is empty or null, then the help menu
     *   item is made invisible.
     * - Otherwise, this makes the help menu item visible and sets the intent for the help menu
     *   item to view the URL.
     *
     * @return returns whether the help menu item has been made visible.
     */
    public static boolean prepareHelpMenuItem(Context context, MenuItem helpMenuItem,
            int helpUrlResourceId) {
        String helpUrlString = context.getResources().getString(helpUrlResourceId);
        return prepareHelpMenuItem(context, helpMenuItem, helpUrlString);
    }

    /**
     * Prepares the help menu item by doing the following.
     * - If the helpUrlString is empty or null, the help menu item is made invisible.
     * - Otherwise, this makes the help menu item visible and sets the intent for the help menu
     *   item to view the URL.
     *
     * @return returns whether the help menu item has been made visible.
     */
    public static boolean prepareHelpMenuItem(Context context, MenuItem helpMenuItem,
            String helpUrlString) {
        if (TextUtils.isEmpty(helpUrlString)) {
            // The help url string is empty or null, so set the help menu item to be invisible.
            helpMenuItem.setVisible(false);

            // return that the help menu item is not visible (i.e. false)
            return false;
        } else {
            // The help url string exists, so first add in some extra query parameters.
            final Uri fullUri = uriWithAddedParameters(context, Uri.parse(helpUrlString));

            // Then, create an intent that will be fired when the user
            // selects this help menu item.
            Intent intent = new Intent(Intent.ACTION_VIEW, fullUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            // Set the intent to the help menu item, show the help menu item in the overflow
            // menu, and make it visible.
            ComponentName component = intent.resolveActivity(context.getPackageManager());
            if (component != null) {
                helpMenuItem.setIntent(intent);
                helpMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                helpMenuItem.setVisible(true);
            } else {
                helpMenuItem.setVisible(false);
                return false;
            }

            // return that the help menu item is visible (i.e., true)
            return true;
        }
    }

    /**
     * Adds two query parameters into the Uri, namely the language code and the version code
     * of the app's package as gotten via the context.
     * @return the uri with added query parameters
     */
    public static Uri uriWithAddedParameters(Context context, Uri baseUri) {
        Uri.Builder builder = baseUri.buildUpon();

        // Add in the preferred language
        builder.appendQueryParameter(PARAM_LANGUAGE_CODE, Locale.getDefault().toString());

        // Add in the package version code
        if (sCachedVersionCode == null) {
            // There is no cached version code, so try to get it from the package manager.
            try {
                // cache the version code
                PackageInfo info = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);
                sCachedVersionCode = Integer.toString(info.versionCode);

                // append the version code to the uri
                builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
            } catch (NameNotFoundException e) {
                // Cannot find the package name, so don't add in the version parameter
                // This shouldn't happen.
                Log.wtf(TAG, "Invalid package name for context", e);
            }
        } else {
            builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
        }

        // Build the full uri and return it
        return builder.build();
    }
}
