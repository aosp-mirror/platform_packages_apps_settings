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
 * limitations under the License
 */

package com.android.settings.applications;

import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

/**
 * UI grouping of important intents that can be configured by device and profile owners.
 */
public enum EnterpriseDefaultApps {
    BROWSER(new Intent[] {
            buildIntent(Intent.ACTION_VIEW, Intent.CATEGORY_BROWSABLE, "http:", null)}),
    CALENDAR(new Intent[] {
            buildIntent(Intent.ACTION_INSERT, null, null, "vnd.android.cursor.dir/event")}),
    CAMERA(new Intent[] {
            new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            new Intent(MediaStore.ACTION_VIDEO_CAPTURE)}),
    CONTACTS(new Intent[] {
            buildIntent(Intent.ACTION_PICK, null, null, ContactsContract.Contacts.CONTENT_TYPE)}),
    EMAIL(new Intent[] {
            new Intent(Intent.ACTION_SENDTO), new Intent(Intent.ACTION_SEND),
            new Intent(Intent.ACTION_SEND_MULTIPLE)}),
    MAP(new Intent[] {buildIntent(Intent.ACTION_VIEW, null, "geo:", null)}),
    PHONE(new Intent[] {new Intent(Intent.ACTION_DIAL), new Intent(Intent.ACTION_CALL)});
    private final Intent[] mIntents;

    EnterpriseDefaultApps(Intent[] intents) {
        mIntents = intents;
    }

    public Intent[] getIntents() {
        return mIntents;
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
