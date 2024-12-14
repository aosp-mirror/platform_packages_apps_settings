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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.AlertDialog;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;

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
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialog.class,
        })
public class SyncedStateTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String ENCRYPTED_METADATA =
            "BLUETOOTH:UUID:184F;BN:VGVzdA==;AT:1;AD:00A1A1A1A1A1;BI:1E240;BC:VGVzdENvZGU=;"
                    + "MD:BgNwVGVzdA==;AS:1;PI:A0;NS:1;BS:3;NB:2;SM:BQNUZXN0BARlbmc=;;";
    private static final String BROADCAST_TITLE = "title";
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
        when(mMockPreference.getAudioStreamMetadata())
                .thenReturn(
                        BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(
                                ENCRYPTED_METADATA));
        when(mMockPreference.getContext()).thenReturn(mMockContext);
        when(mMockPreference.getTitle()).thenReturn(BROADCAST_TITLE);

        Preference.OnPreferenceClickListener listener =
                mInstance.getOnClickListener(mMockController);
        assertThat(listener).isNotNull();

        listener.onPreferenceClick(mMockPreference);
        shadowMainLooper().idle();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();

        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();

        Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        assertThat(neutralButton).isNotNull();
        assertThat(neutralButton.getText().toString())
                .isEqualTo(mMockContext.getString(android.R.string.cancel));

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertThat(positiveButton).isNotNull();
        assertThat(positiveButton.getText().toString())
                .isEqualTo(
                        mMockContext.getString(R.string.bluetooth_connect_access_dialog_positive));

        positiveButton.callOnClick();
        ShadowLooper.idleMainLooper();
        verify(mMockController).handleSourceAddRequest(any(), any());

        ShadowAlertDialog shadowDialog = Shadow.extract(dialog);
        TextView title = shadowDialog.getView().findViewById(R.id.broadcast_name_text);
        assertThat(title).isNotNull();
        assertThat(title.getText().toString()).isEqualTo(BROADCAST_TITLE);
        assertThat(shadowDialog.getTitle().toString())
                .isEqualTo(mMockContext.getString(R.string.find_broadcast_password_dialog_title));

        dialog.cancel();
    }
}
