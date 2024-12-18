/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.settings.network.apn;

import static com.android.settings.network.apn.ApnEditPageProviderKt.EDIT_URL;
import static com.android.settings.network.apn.ApnEditPageProviderKt.INSERT_URL;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.spa.SpaActivity;

/** Use to edit apn settings. */
public class ApnEditor extends SettingsPreferenceFragment {

    private static final String TAG = ApnEditor.class.getSimpleName();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        maybeRedirectToNewPage();
        finish();
    }

    private void maybeRedirectToNewPage() {
        if (isUserRestricted()) {
            Log.e(TAG, "This setting isn't available due to user restriction.");
            return;
        }

        final Intent intent = getIntent();
        final String action = intent.getAction();

        int subId =
                intent.getIntExtra(ApnSettings.SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        Uri uri = intent.getData();
        if (Intent.ACTION_EDIT.equals(action)) {
            if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Edit request not for carrier table. Uri: " + uri);
            } else {
                String route = ApnEditPageProvider.INSTANCE.getRoute(EDIT_URL, uri, subId);
                SpaActivity.startSpaActivity(requireContext(), route);
            }
        } else if (Intent.ACTION_INSERT.equals(action)) {
            if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Insert request not for carrier table. Uri: " + uri);
            } else {
                String route = ApnEditPageProvider.INSTANCE.getRoute(
                        INSERT_URL, Telephony.Carriers.CONTENT_URI, subId);
                SpaActivity.startSpaActivity(getContext(), route);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APN_EDITOR;
    }

    @VisibleForTesting
    boolean isUserRestricted() {
        UserManager userManager = getContext().getSystemService(UserManager.class);
        if (userManager == null) {
            return false;
        }
        if (!userManager.isAdminUser()) {
            Log.e(TAG, "User is not an admin");
            return true;
        }
        if (userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            Log.e(TAG, "User is not allowed to configure mobile network");
            return true;
        }
        return false;
    }
}
