/*
 * Copyright (C) 2016 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

import android.net.Uri;
import android.os.Parcel;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.InlineSliderPayload;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InlineSliderPayloadTest {
    private InlineSliderPayload mPayload;

    @Test
    public void testParcelOrdering_StaysValid() {
        Uri uri = Uri.parse("http://www.TESTURI.com");
        Parcel parcel = Parcel.obtain();

        mPayload = new InlineSliderPayload(uri);
        mPayload.writeToParcel(parcel, 0);
        // Reset parcel for reading
        parcel.setDataPosition(0);
        InlineSliderPayload newPayload = InlineSliderPayload.CREATOR.createFromParcel(parcel);

        String originalUri = mPayload.uri.toString();
        String copiedUri = newPayload.uri.toString();
        assertThat(originalUri).isEqualTo(copiedUri);
    }
}
