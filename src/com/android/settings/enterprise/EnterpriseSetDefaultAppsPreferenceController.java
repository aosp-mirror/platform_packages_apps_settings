/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.enterprise;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class EnterpriseSetDefaultAppsPreferenceController extends PreferenceController {

    private static final String KEY_DEFAULT_APPS = "number_enterprise_set_default_apps";
    private final ApplicationFeatureProvider mFeatureProvider;

    public EnterpriseSetDefaultAppsPreferenceController(Context context) {
        super(context);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getApplicationFeatureProvider(context);
    }

    @Override
    public void updateState(Preference preference) {
        // Browser
        int num = mFeatureProvider.findPersistentPreferredActivities(new Intent[] {
                buildIntent(Intent.ACTION_VIEW, Intent.CATEGORY_BROWSABLE, "http:", null)}).size();
        // Camera
        num += mFeatureProvider.findPersistentPreferredActivities(new Intent[] {
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                new Intent(MediaStore.ACTION_VIDEO_CAPTURE)}).size();
        // Map
        num += mFeatureProvider.findPersistentPreferredActivities(new Intent[] {
                buildIntent(Intent.ACTION_VIEW, null, "geo:", null)}).size();
        // E-mail
        num += mFeatureProvider.findPersistentPreferredActivities(new Intent[] {
                new Intent(Intent.ACTION_SENDTO), new Intent(Intent.ACTION_SEND),
                new Intent(Intent.ACTION_SEND_MULTIPLE)}).size();
        // Calendar
        num += mFeatureProvider.findPersistentPreferredActivities(new Intent[] {
                buildIntent(Intent.ACTION_INSERT, null, null, "vnd.android.cursor.dir/event")})
                .size();
        // Contacts
        num += mFeatureProvider.findPersistentPreferredActivities(new Intent[] {
                buildIntent(Intent.ACTION_PICK, null, null,
                        ContactsContract.Contacts.CONTENT_TYPE)}).size();
        // Dialer
        num += mFeatureProvider.findPersistentPreferredActivities(new Intent[] {
                new Intent(Intent.ACTION_DIAL), new Intent(Intent.ACTION_CALL)}).size();

        if (num == 0) {
            preference.setVisible(false);
        } else {
            preference.setVisible(true);
            preference.setTitle(mContext.getResources().getQuantityString(
                    R.plurals.enterprise_privacy_number_enterprise_set_default_apps,
                    num, num));
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEFAULT_APPS;
    }

    private static Intent buildIntent(String action, String category, String protocol,
            String type) {
        final Intent intent = new Intent(action);
        if (category != null) {
            intent.addCategory(category);
        }
        if (protocol != null) {
            intent.setData(Uri.parse(protocol));
        }
        if (type != null) {
            intent.setType(type);
        }
        return intent;
    }
}
