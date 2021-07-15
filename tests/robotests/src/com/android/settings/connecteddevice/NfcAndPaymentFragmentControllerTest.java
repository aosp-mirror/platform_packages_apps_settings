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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowNfcAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNfcAdapter.class)
public class NfcAndPaymentFragmentControllerTest {
    private NfcAndPaymentFragmentController mController;
    private Context mContext;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private NfcManager mNfcManager;

    private ShadowNfcAdapter mShadowNfcAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mShadowNfcAdapter = Shadow.extract(NfcAdapter.getDefaultAdapter(mContext));

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.NFC_SERVICE)).thenReturn(mNfcManager);

        mController = new NfcAndPaymentFragmentController(mContext, "fakeKey");
    }

    @Test
    public void getAvailabilityStatus_hasNfc_shouldReturnAvailable() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mUserManager.isAdminUser()).thenReturn(true);
        mShadowNfcAdapter.setEnabled(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(NfcAndPaymentFragmentController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noNfcAdapter_shouldReturnUnsupported() {
        ReflectionHelpers.setField(mController, "mNfcAdapter", null);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(NfcAndPaymentFragmentController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_nfcOn_shouldProvideOnSummary() {
        mShadowNfcAdapter.setEnabled(true);
        assertThat(mController.getSummary().toString()).contains(
                mContext.getString(R.string.switch_on_text));
    }

    @Test
    public void getSummary_nfcOff_shouldProvideOffSummary() {
        mShadowNfcAdapter.setEnabled(false);
        assertThat(mController.getSummary().toString()).contains(
                mContext.getString(R.string.switch_off_text));
    }
}
