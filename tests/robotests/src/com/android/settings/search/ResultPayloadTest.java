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
 * limitations under the License.
 *
 */

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.Parcel;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SettingsRobolectricTestRunner.class)
public class ResultPayloadTest {

    private static final String EXTRA_KEY = "key";
    private static final String EXTRA_VALUE = "value";

    @Test
    public void testParcelOrdering_StaysValid() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY, EXTRA_VALUE);
        Parcel parcel = Parcel.obtain();

        final ResultPayload payload = new ResultPayload(intent);
        payload.writeToParcel(parcel, 0);
        // Reset parcel for reading
        parcel.setDataPosition(0);
        ResultPayload newPayload = ResultPayload.CREATOR.createFromParcel(parcel);

        String originalIntentExtra = payload.getIntent().getStringExtra(EXTRA_KEY);
        String copiedIntentExtra = newPayload.getIntent().getStringExtra(EXTRA_KEY);
        assertThat(originalIntentExtra).isEqualTo(copiedIntentExtra);
    }
}
