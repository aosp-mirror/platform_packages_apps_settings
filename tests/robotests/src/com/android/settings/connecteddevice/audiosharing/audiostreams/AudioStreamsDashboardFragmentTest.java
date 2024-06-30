/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsDashboardFragment.KEY_BROADCAST_METADATA;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsScanQrCodeController.REQUEST_SCAN_BT_BROADCAST_QR_CODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioStreamsDashboardFragmentTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String VALID_METADATA =
            "BLUETOOTH:UUID:184F;BN:VGVzdA==;AT:1;AD:00A1A1A1A1A1;BI:1E240;BC:VGVzdENvZGU=;"
                    + "MD:BgNwVGVzdA==;AS:1;PI:A0;NS:1;BS:3;NB:2;SM:BQNUZXN0BARlbmc=;;";

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private AudioStreamsProgressCategoryController mController;
    private TestFragment mTestFragment;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mTestFragment = spy(new TestFragment());
        doReturn(mContext).when(mTestFragment).getContext();
        mController = spy(new AudioStreamsProgressCategoryController(mContext, "key"));
        doReturn(mController).when(mTestFragment).use(AudioStreamsProgressCategoryController.class);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mTestFragment.getPreferenceScreenResId())
                .isEqualTo(R.xml.bluetooth_le_audio_streams);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mTestFragment.getLogTag()).isEqualTo("AudioStreamsDashboardFrag");
    }

    @Test
    public void getHelpResource_returnsCorrectResource() {
        assertThat(mTestFragment.getHelpResource()).isEqualTo(R.string.help_url_audio_sharing);
    }

    @Test
    public void onActivityResult_invalidRequestCode_doNothing() {
        mTestFragment.onAttach(mContext);

        mTestFragment.onActivityResult(0, 0, null);
        verify(mController, never()).setSourceFromQrCode(any(), any());
    }

    @Test
    public void onActivityResult_invalidRequestResult_doNothing() {
        mTestFragment.onAttach(mContext);

        mTestFragment.onActivityResult(REQUEST_SCAN_BT_BROADCAST_QR_CODE, 0, null);
        verify(mController, never()).setSourceFromQrCode(any(), any());
    }

    @Test
    public void onActivityResult_nullData_doNothing() {
        mTestFragment.onAttach(mContext);

        mTestFragment.onActivityResult(REQUEST_SCAN_BT_BROADCAST_QR_CODE, Activity.RESULT_OK, null);
        verify(mController, never()).setSourceFromQrCode(any(), any());
    }

    @Test
    public void onActivityResult_setSourceFromQrCode() {
        mTestFragment.onAttach(mContext);
        Intent intent = new Intent();
        intent.putExtra(KEY_BROADCAST_METADATA, VALID_METADATA);

        mTestFragment.onActivityResult(
                REQUEST_SCAN_BT_BROADCAST_QR_CODE, Activity.RESULT_OK, intent);
        verify(mController).setSourceFromQrCode(any(), any());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(),
                        eq(SettingsEnums.ACTION_AUDIO_STREAM_QR_CODE_SCAN_SUCCEED),
                        anyInt());
    }

    @Test
    public void onAttach_hasArgument() {
        BluetoothLeBroadcastMetadata data = mock(BluetoothLeBroadcastMetadata.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_BROADCAST_METADATA, data);
        mTestFragment.setArguments(bundle);

        mTestFragment.onAttach(mContext);

        verify(mController).setSourceFromQrCode(eq(data), any());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(),
                        eq(SettingsEnums.ACTION_AUDIO_STREAM_QR_CODE_SCAN_SUCCEED),
                        anyInt());
    }

    public static class TestFragment extends AudioStreamsDashboardFragment {
        @Override
        protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }
    }
}
