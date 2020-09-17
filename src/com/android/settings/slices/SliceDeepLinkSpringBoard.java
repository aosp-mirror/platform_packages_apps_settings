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
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.notification.zen.ZenModeSliceBuilder;

public class SliceDeepLinkSpringBoard extends Activity {

    private static final String TAG = "DeeplinkSpringboard";
    public static final String EXTRA_SLICE = "slice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Uri sliceUri = parse(getIntent().getData());
        if (sliceUri == null) {
            Log.e(TAG, "No data found");
            finish();
            return;
        }
        try {
            // This shouldn't matter since the slice is shown instead of the device
            // index caring about the launch uri.
            Intent launchIntent;

            // TODO (b/80263568) Avoid duplicating this list of Slice Uris.
            if (CustomSliceRegistry.isValidUri(sliceUri)) {
                final CustomSliceable sliceable =
                        CustomSliceable.createInstance(getApplicationContext(),
                                CustomSliceRegistry.getSliceClassByUri(sliceUri));
                launchIntent = sliceable.getIntent();
            } else if (CustomSliceRegistry.ZEN_MODE_SLICE_URI.equals(sliceUri)) {
                launchIntent = ZenModeSliceBuilder.getIntent(this /* context */);
            } else if (CustomSliceRegistry.BLUETOOTH_URI.equals(sliceUri)) {
                launchIntent = BluetoothSliceBuilder.getIntent(this /* context */);
            } else {
                final SlicesDatabaseAccessor slicesDatabaseAccessor =
                        new SlicesDatabaseAccessor(this /* context */);
                // Sadly have to block here because we don't know where to go.
                final SliceData sliceData =
                        slicesDatabaseAccessor.getSliceDataFromUri(sliceUri);
                launchIntent = SliceBuilderUtils.getContentIntent(this, sliceData);
            }
            startActivity(launchIntent);
            finish();
        } catch (Exception e) {
            Log.w(TAG, "Couldn't launch Slice intent", e);
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            finish();
        }
    }

    private static Uri parse(Uri uri) {
        final String sliceParameter = uri.getQueryParameter(EXTRA_SLICE);
        if (TextUtils.isEmpty(sliceParameter)) {
            EventLog.writeEvent(0x534e4554, "122836081", -1, "");
            return null;
        } else {
            return Uri.parse(sliceParameter);
        }
    }
}
