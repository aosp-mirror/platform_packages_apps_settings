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
import android.provider.Settings;
import android.util.Log;

import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.location.LocationSliceBuilder;
import com.android.settings.notification.ZenModeSliceBuilder;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.WifiSliceBuilder;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;

import java.net.URISyntaxException;

public class SliceDeepLinkSpringBoard extends Activity {

    private static final String TAG = "DeeplinkSpringboard";
    public static final String INTENT = "intent";
    public static final String SETTINGS = "settings";
    public static final String ACTION_VIEW_SLICE = "com.android.settings.action.VIEW_SLICE";
    public static final String EXTRA_SLICE = "slice";

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
            if (ACTION_VIEW_SLICE.equals(intent.getAction())) {
                // This shouldn't matter since the slice is shown instead of the device
                // index caring about the launch uri.
                final Uri slice = Uri.parse(intent.getStringExtra(EXTRA_SLICE));
                final Intent launchIntent;

                // TODO (b/80263568) Avoid duplicating this list of Slice Uris.
                if (WifiSliceBuilder.WIFI_URI.equals(slice)) {
                    launchIntent = WifiSliceBuilder.getIntent(this /* context */);
                } else if (ZenModeSliceBuilder.ZEN_MODE_URI.equals(slice)) {
                    launchIntent = ZenModeSliceBuilder.getIntent(this /* context */);
                } else if (BluetoothSliceBuilder.BLUETOOTH_URI.equals(slice)) {
                    launchIntent = BluetoothSliceBuilder.getIntent(this /* context */);
                } else if (LocationSliceBuilder.LOCATION_URI.equals(slice)) {
                    launchIntent = LocationSliceBuilder.getIntent(this /* context */);
                } else {
                    final SlicesDatabaseAccessor slicesDatabaseAccessor =
                            new SlicesDatabaseAccessor(this /* context */);
                    // Sadly have to block here because we don't know where to go.
                    final SliceData sliceData = slicesDatabaseAccessor.getSliceDataFromUri(slice);
                    launchIntent = SliceBuilderUtils.getContentIntent(this, sliceData);
                }

                startActivity(launchIntent);
            } else {
                startActivity(intent);
            }
            finish();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error decoding uri", e);
            finish();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Couldn't launch Slice intent", e);
            startActivity(new Intent(Settings.ACTION_SETTINGS));
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
