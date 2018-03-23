/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.net.URISyntaxException;

public class SliceDeepLinkSpringBoard extends Activity {

    private static final String TAG = "DeeplinkSpringboard";
    public static final String INTENT = "intent";
    public static final String SETTINGS = "settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri == null) {
            Log.e(TAG, "No data found");
            finish();
            return;
        }
        try {
            Intent intent = parse(uri, getPackageName());
            startActivity(intent);
            finish();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error decoding uri", e);
            finish();
        }
    }

    public static Intent parse(Uri uri, String pkg) throws URISyntaxException {
        Intent intent = Intent.parseUri(uri.getQueryParameter(INTENT),
                Intent.URI_ANDROID_APP_SCHEME);
        // Start with some really strict constraints and loosen them if we need to.
        // Don't allow components.
        intent.setComponent(null);
        // Clear out the extras.
        if (intent.getExtras() != null) {
            intent.getExtras().clear();
        }
        // Make sure this points at Settings.
        intent.setPackage(pkg);
        return intent;
    }
}
