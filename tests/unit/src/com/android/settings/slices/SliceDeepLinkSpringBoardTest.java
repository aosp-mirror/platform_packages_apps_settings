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

package com.android.settings.slices;

import static com.android.settings.slices.SliceDeepLinkSpringBoard.createDeepLink;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.platform.test.annotations.Presubmit;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class SliceDeepLinkSpringBoardTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    @Presubmit
    public void launchesDeepLinkIntent_shouldNotCrash() {
        final Intent deepLinkIntent = getSpringboardIntent(
                "content://com.android.settings.slices/action/test_slice");

        mContext.startActivity(deepLinkIntent);
    }

    @Test
    @Presubmit
    public void launchesDeepLinkIntent_wifiSlice_shouldNotCrash() {
        final Intent deepLinkIntent = getSpringboardIntent(
                CustomSliceRegistry.WIFI_SLICE_URI.toString());

        mContext.startActivity(deepLinkIntent);
    }

    @Test
    @Presubmit
    public void launchesDeepLinkIntent_bluetoothSlice_shouldNotCrash() {
        final Intent deepLinkIntent = getSpringboardIntent(
                CustomSliceRegistry.BLUETOOTH_URI.toString());

        mContext.startActivity(deepLinkIntent);
    }

    @Test
    @Presubmit
    public void launchesDeepLinkIntent_dndSlice_shouldNotCrash() {
        final Intent deepLinkIntent = getSpringboardIntent(
                CustomSliceRegistry.ZEN_MODE_SLICE_URI.toString());

        mContext.startActivity(deepLinkIntent);
    }

    @Test
    @Presubmit
    public void launchesDeepLinkIntent_locationSlice_shouldNotCrash() {
        final Intent deepLinkIntent = getSpringboardIntent(
                CustomSliceRegistry.LOCATION_SLICE_URI.toString());

        mContext.startActivity(deepLinkIntent);
    }

    private Intent getSpringboardIntent(String uriString) {
        final Uri uri = createDeepLink(new Intent(SliceDeepLinkSpringBoard.ACTION_VIEW_SLICE)
                .setPackage(mContext.getPackageName())
                .putExtra(SliceDeepLinkSpringBoard.EXTRA_SLICE, uriString)
                .toUri(Intent.URI_ANDROID_APP_SCHEME));

        return new Intent(Intent.ACTION_VIEW)
                .setData(uri)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
