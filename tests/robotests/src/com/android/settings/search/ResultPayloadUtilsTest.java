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
import static junit.framework.Assert.fail;

import android.content.Intent;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SettingsRobolectricTestRunner.class)
public class ResultPayloadUtilsTest {

    private static final String EXTRA_KEY = "key";
    private static final String EXTRA_VALUE = "value";

    private ResultPayload payload;

    @Before
    public void setUp() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY, EXTRA_VALUE);
        payload = new ResultPayload(intent);
    }

    @Test
    public void testUnmarshallBadData_ExceptionThrown() {
        byte[] badData = "I'm going to fail :)".getBytes();
        try {
            ResultPayloadUtils.unmarshall(badData, ResultPayload.CREATOR);
            fail("unmarshall should throw exception");
        } catch (RuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    public void testMarshallResultPayload_NonEmptyArray() {
        byte[] marshalledPayload = ResultPayloadUtils.marshall(payload);
        assertThat(marshalledPayload).isNotNull();
        assertThat(marshalledPayload).isNotEmpty();
    }

    @Test
    public void testUnmarshall_PreservedData() {
        byte[] marshalledPayload = ResultPayloadUtils.marshall(payload);
        ResultPayload newPayload =
            ResultPayloadUtils.unmarshall(marshalledPayload, ResultPayload.CREATOR);

        String originalIntentExtra = payload.getIntent().getStringExtra(EXTRA_KEY);
        String copiedIntentExtra = newPayload.getIntent().getStringExtra(EXTRA_KEY);
        assertThat(originalIntentExtra).isEqualTo(copiedIntentExtra);
    }
}