/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.nfc;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.nfc.NfcAdapter;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowNfcAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNfcAdapter.class)
public class NfcTagAppsPreferenceControllerTest {

    private NfcTagAppsPreferenceController mController;
    private static final String KEY = "key";
    private ShadowNfcAdapter mShadowNfcAdapter;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new NfcTagAppsPreferenceController(mContext, KEY);
        mShadowNfcAdapter = Shadow.extract(NfcAdapter.getDefaultAdapter(mContext));
    }

    @Test
    public void getAvailabilityStatus_featureNotEnabled_shouldNotReturnAvailable() {
        mShadowNfcAdapter.setTagIntentAppPreferenceSupported(false);
        assertThat(mController.getAvailabilityStatus()).isNotEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_featureEnabled_shouldReturnAvailable() {
        mShadowNfcAdapter.setTagIntentAppPreferenceSupported(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
