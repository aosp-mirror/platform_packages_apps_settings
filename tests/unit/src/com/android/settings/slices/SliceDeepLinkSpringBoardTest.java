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

import static com.android.settings.search.DeviceIndexFeatureProvider.createDeepLink;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SliceDeepLinkSpringBoardTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void launcheDeepLinkIntent_shouldNotCrash() {
        final Uri springBoardIntentUri = createDeepLink(
                new Intent(SliceDeepLinkSpringBoard.ACTION_VIEW_SLICE)
                        .setPackage(mContext.getPackageName())
                        .putExtra(SliceDeepLinkSpringBoard.EXTRA_SLICE,
                                "content://com.android.settings.slices/action/test_slice")
                        .toUri(Intent.URI_ANDROID_APP_SCHEME));

        final Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW)
                .setData(springBoardIntentUri)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mContext.startActivity(deepLinkIntent);
    }
}
