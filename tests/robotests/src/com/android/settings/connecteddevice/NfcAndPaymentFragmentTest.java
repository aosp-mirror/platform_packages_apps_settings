/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.provider.SearchIndexableResource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NfcAndPaymentFragmentTest {
    private NfcAndPaymentFragment mFragment;
    private Context mContext;

    @Mock
    private NfcAdapter mNfcAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = new NfcAndPaymentFragment();
        mContext = spy(RuntimeEnvironment.application);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                NfcAndPaymentFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void searchIndexProvider_shouldIndexValidItems() {
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.isSecureNfcSupported()).thenReturn(true);

        final List<String> niks = NfcAndPaymentFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(niks).isNotNull();
        assertThat(niks).containsExactly("nfc_detection_point");
    }
}
