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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamStateHandler.EMPTY_STRING_RES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.AlertDialog;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialog.class,
        })
public class SyncedStateTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private AudioStreamsProgressCategoryController mMockController;
    @Mock private AudioStreamPreference mMockPreference;
    @Mock private BluetoothLeBroadcastMetadata mMockMetadata;
    private Context mMockContext;
    private SyncedState mInstance;

    @Before
    public void setUp() {
        ShadowAlertDialog.reset();
        mMockContext = ApplicationProvider.getApplicationContext();
        mInstance = SyncedState.getInstance();
    }

    @After
    public void tearDown() {
        ShadowAlertDialog.reset();
    }

    @Test
    public void testGetInstance() {
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(AudioStreamStateHandler.class);
    }

    @Test
    public void testGetSummary() {
        int summary = mInstance.getSummary();
        assertThat(summary).isEqualTo(EMPTY_STRING_RES);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(AudioStreamsProgressCategoryController.AudioStreamState.SYNCED);
    }

    @Test
    public void testGetOnClickListener_isNotEncrypted_handleSourceAddRequest() {
        Preference.OnPreferenceClickListener listener =
                mInstance.getOnClickListener(mMockController);
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(mMockMetadata);

        listener.onPreferenceClick(mMockPreference);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog).isNull();
        verify(mMockController).handleSourceAddRequest(mMockPreference, mMockMetadata);
    }

    @Test
    public void testGetOnClickListener_isEncrypted_passwordDialogShowing() {
        Preference.OnPreferenceClickListener listener =
                mInstance.getOnClickListener(mMockController);
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(mMockMetadata);
        when(mMockPreference.getContext()).thenReturn(mMockContext);
        when(mMockMetadata.isEncrypted()).thenReturn(true);

        listener.onPreferenceClick(mMockPreference);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        verify(mMockController, never()).handleSourceAddRequest(mMockPreference, mMockMetadata);
    }
}
