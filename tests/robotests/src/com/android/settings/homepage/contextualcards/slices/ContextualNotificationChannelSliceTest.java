/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ContextualNotificationChannelSliceTest {

    private Context mContext;
    private ContextualNotificationChannelSlice mNotificationChannelSlice;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mNotificationChannelSlice = new ContextualNotificationChannelSlice(mContext);
    }

    @Test
    public void getUri_shouldBeContextualNotificationChannelSliceUri() {
        final Uri uri = mNotificationChannelSlice.getUri();

        assertThat(uri).isEqualTo(CustomSliceRegistry.CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI);
    }

    @Test
    public void getSubTitle_shouldBeRecentlyInstalledApp() {
        final CharSequence subTitle = mNotificationChannelSlice.getSubTitle("com.test.package", 0);

        assertThat(subTitle).isEqualTo(mContext.getText(R.string.recently_installed_app));
    }
}
