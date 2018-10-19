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

package com.android.settings.nfc;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class PaymentSettingsTest {

    @Mock
    Context mContext;

    @Mock
    private PackageManager mManager;

    private PaymentSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new PaymentSettings();
        when(mContext.getPackageManager()).thenReturn(mManager);
    }

    @Test
    public void testNonIndexableKey_NoNFC_KeyAdded() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(false);

        final List<String> niks =
            PaymentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        assertThat(niks).contains(PaymentSettings.PAYMENT_KEY);
    }

    @Test
    public void testNonIndexableKey_NFC_NoKeyAdded() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);

        final List<String> niks =
            PaymentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
        assertThat(niks).isEmpty();
    }
}