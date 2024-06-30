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

import static android.app.settings.SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_WAIT_FOR_SYNC_TIMEOUT;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsScanQrCodeController.REQUEST_SCAN_BT_BROADCAST_QR_CODE;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.WaitForSyncState.AUDIO_STREAM_WAIT_FOR_SYNC_STATE_SUMMARY;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.WaitForSyncState.WAIT_FOR_SYNC_TIMEOUT_MILLIS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class WaitForSyncStateTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    @Mock private AudioStreamPreference mMockPreference;
    @Mock private AudioStreamsProgressCategoryController mMockController;
    @Mock private AudioStreamsHelper mMockHelper;
    @Mock private BluetoothLeBroadcastMetadata mMockMetadata;
    private FakeFeatureFactory mFeatureFactory;
    private WaitForSyncState mInstance;

    @Before
    public void setUp() {
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mInstance = new WaitForSyncState();
    }

    @Test
    public void testGetInstance() {
        mInstance = WaitForSyncState.getInstance();
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(AudioStreamStateHandler.class);
    }

    @Test
    public void testGetSummary() {
        int summary = mInstance.getSummary();
        assertThat(summary).isEqualTo(AUDIO_STREAM_WAIT_FOR_SYNC_STATE_SUMMARY);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(AudioStreamsProgressCategoryController.AudioStreamState.WAIT_FOR_SYNC);
    }

    @Test
    public void testPerformAction_timeout_stateNotMatching_doNothing() {
        when(mMockPreference.isShown()).thenReturn(true);
        when(mMockPreference.getAudioStreamState())
                .thenReturn(AudioStreamsProgressCategoryController.AudioStreamState.UNKNOWN);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);
        ShadowLooper.idleMainLooper(WAIT_FOR_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        verify(mMockController, never()).handleSourceLost(anyInt());
    }

    @Test
    public void testPerformAction_timeout_stateMatching_sourceLost() {
        when(mMockPreference.isShown()).thenReturn(true);
        when(mMockPreference.getAudioStreamState())
                .thenReturn(AudioStreamsProgressCategoryController.AudioStreamState.WAIT_FOR_SYNC);
        when(mMockPreference.getAudioStreamBroadcastId()).thenReturn(1);
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(mMockMetadata);
        when(mMockPreference.getContext()).thenReturn(mContext);
        when(mMockPreference.getSourceOriginForLogging())
                .thenReturn(SourceOriginForLogging.BROADCAST_SEARCH);
        when(mMockController.getFragment()).thenReturn(mock(AudioStreamsDashboardFragment.class));

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);
        ShadowLooper.idleMainLooper(WAIT_FOR_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        verify(mMockController).handleSourceLost(1);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        eq(mContext),
                        eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN_FAILED_WAIT_FOR_SYNC_TIMEOUT),
                        eq(SourceOriginForLogging.BROADCAST_SEARCH.ordinal()));
        verify(mContext).getString(R.string.audio_streams_dialog_stream_is_not_available);
        verify(mContext).getString(R.string.audio_streams_is_not_playing);
        verify(mContext).getString(R.string.audio_streams_dialog_close);
        verify(mContext).getString(R.string.audio_streams_dialog_retry);
    }

    @Test
    public void testLaunchQrCodeScanFragment() {
        // mContext is not an Activity context, calling startActivity() from outside of an Activity
        // context requires the FLAG_ACTIVITY_NEW_TASK flag, create a mock to avoid this
        // AndroidRuntimeException.
        Context activityContext = mock(Context.class);
        AudioStreamsDashboardFragment fragment = mock(AudioStreamsDashboardFragment.class);
        mInstance.launchQrCodeScanFragment(activityContext, fragment);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(fragment)
                .startActivityForResult(intentCaptor.capture(), requestCodeCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AudioStreamsQrCodeScanFragment.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.audio_streams_main_page_scan_qr_code_title);
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, 0))
                .isEqualTo(DIALOG_AUDIO_STREAM_MAIN_WAIT_FOR_SYNC_TIMEOUT);

        int requestCode = requestCodeCaptor.getValue();
        assertThat(requestCode).isEqualTo(REQUEST_SCAN_BT_BROADCAST_QR_CODE);
    }
}
