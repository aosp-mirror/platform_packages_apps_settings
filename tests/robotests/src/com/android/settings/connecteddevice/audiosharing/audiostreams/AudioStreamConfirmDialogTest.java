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

import static android.app.settings.SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_LISTEN;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamConfirmDialog.DEFAULT_DEVICE_NAME;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsDashboardFragment.KEY_BROADCAST_METADATA;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.Intent;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.flags.Flags;

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
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioStreamConfirmDialogTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String VALID_METADATA =
            "BLUETOOTH:UUID:184F;BN:VGVzdA==;AT:1;AD:00A1A1A1A1A1;BI:1E240;BC:VGVzdENvZGU=;"
                    + "MD:BgNwVGVzdA==;AS:1;PI:A0;NS:1;BS:3;NB:2;SM:BQNUZXN0BARlbmc=;;";
    private static final String DEVICE_NAME = "device_name";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothManager mLocalBluetoothManager;
    @Mock private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private BluetoothDevice mBluetoothDevice;
    private AudioStreamConfirmDialog mDialogFragment;

    @Before
    public void setUp() {
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastAssistantProfile())
                .thenReturn(mAssistant);
        when(mLocalBluetoothProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);

        mDialogFragment = new AudioStreamConfirmDialog();
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
        mDialogFragment.dismiss();
    }

    @Test
    public void showDialog_unsupported() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        FragmentController.setupFragment(
                mDialogFragment,
                FragmentActivity.class,
                /* containerViewId= */ 0,
                /* bundle= */ null);
        shadowMainLooper().idle();

        assertThat(mDialogFragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_FEATURE_UNSUPPORTED);
        assertThat(mDialogFragment.mActivity).isNotNull();
        mDialogFragment.mActivity = spy(mDialogFragment.mActivity);

        var dialog = mDialogFragment.getDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView title = dialog.findViewById(R.id.dialog_title);
        assertThat(title).isNotNull();
        assertThat(title.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_cannot_listen));
        TextView subtitle1 = dialog.findViewById(R.id.dialog_subtitle);
        assertThat(subtitle1).isNotNull();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.GONE);
        TextView subtitle2 = dialog.findViewById(R.id.dialog_subtitle_2);
        assertThat(subtitle2).isNotNull();
        assertThat(subtitle2.getText())
                .isEqualTo(
                        mContext.getString(
                                R.string.audio_streams_dialog_unsupported_device_subtitle));
        View leftButton = dialog.findViewById(R.id.left_button);
        assertThat(leftButton).isNotNull();
        assertThat(leftButton.getVisibility()).isEqualTo(View.GONE);
        assertThat(leftButton.hasOnClickListeners()).isFalse();
        View rightButton = dialog.findViewById(R.id.right_button);
        assertThat(rightButton).isNotNull();
        assertThat(rightButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(rightButton.hasOnClickListeners()).isTrue();

        rightButton.callOnClick();
        assertThat(dialog.isShowing()).isFalse();
        verify(mDialogFragment.mActivity).finish();
    }

    @Test
    public void showDialog_noLeDevice() {
        FragmentController.setupFragment(
                mDialogFragment,
                FragmentActivity.class,
                /* containerViewId= */ 0,
                /* bundle= */ null);
        shadowMainLooper().idle();

        assertThat(mDialogFragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_NO_LE_DEVICE);
        assertThat(mDialogFragment.mActivity).isNotNull();
        mDialogFragment.mActivity = spy(mDialogFragment.mActivity);

        var dialog = mDialogFragment.getDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();

        TextView title = dialog.findViewById(R.id.dialog_title);
        assertThat(title).isNotNull();
        assertThat(title.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_no_le_device_title));
        TextView subtitle1 = dialog.findViewById(R.id.dialog_subtitle);
        assertThat(subtitle1).isNotNull();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.GONE);
        TextView subtitle2 = dialog.findViewById(R.id.dialog_subtitle_2);
        assertThat(subtitle2).isNotNull();
        assertThat(subtitle2.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_no_le_device_subtitle));
        View leftButton = dialog.findViewById(R.id.left_button);
        assertThat(leftButton).isNotNull();
        assertThat(leftButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(leftButton.hasOnClickListeners()).isTrue();

        leftButton.callOnClick();
        assertThat(dialog.isShowing()).isFalse();

        Button rightButton = dialog.findViewById(R.id.right_button);
        assertThat(rightButton).isNotNull();
        assertThat(rightButton.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_no_le_device_button));
        assertThat(rightButton.hasOnClickListeners()).isTrue();

        rightButton.callOnClick();
        assertThat(dialog.isShowing()).isFalse();
        verify(mDialogFragment.mActivity, times(2)).finish();
    }

    @Test
    public void showDialog_noMetadata() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mBluetoothDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        when(mBluetoothDevice.getAlias()).thenReturn(DEVICE_NAME);

        FragmentController.setupFragment(
                mDialogFragment,
                FragmentActivity.class,
                /* containerViewId= */ 0,
                /* bundle= */ null);
        shadowMainLooper().idle();

        assertThat(mDialogFragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_DATA_ERROR);
        assertThat(mDialogFragment.mActivity).isNotNull();
        mDialogFragment.mActivity = spy(mDialogFragment.mActivity);

        var dialog = mDialogFragment.getDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView title = dialog.findViewById(R.id.dialog_title);
        assertThat(title).isNotNull();
        assertThat(title.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_cannot_listen));
        TextView subtitle1 = dialog.findViewById(R.id.dialog_subtitle);
        assertThat(subtitle1).isNotNull();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.GONE);
        TextView subtitle2 = dialog.findViewById(R.id.dialog_subtitle_2);
        assertThat(subtitle2).isNotNull();
        assertThat(subtitle2.getText())
                .isEqualTo(
                        mContext.getString(R.string.audio_streams_dialog_cannot_play, DEVICE_NAME));
        View leftButton = dialog.findViewById(R.id.left_button);
        assertThat(leftButton).isNotNull();
        assertThat(leftButton.getVisibility()).isEqualTo(View.GONE);
        assertThat(leftButton.hasOnClickListeners()).isFalse();
        View rightButton = dialog.findViewById(R.id.right_button);
        assertThat(rightButton).isNotNull();
        assertThat(rightButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(rightButton.hasOnClickListeners()).isTrue();

        rightButton.callOnClick();
        assertThat(dialog.isShowing()).isFalse();
        verify(mDialogFragment.mActivity).finish();
    }

    @Test
    public void showDialog_invalidMetadata() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mBluetoothDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        when(mBluetoothDevice.getAlias()).thenReturn(DEVICE_NAME);

        Intent intent = new Intent();
        intent.putExtra(KEY_BROADCAST_METADATA, "invalid");
        FragmentController.of(mDialogFragment, intent)
                .create(/* containerViewId= */ 0, /* bundle= */ null)
                .start()
                .resume()
                .visible()
                .get();
        shadowMainLooper().idle();

        assertThat(mDialogFragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_DATA_ERROR);
        assertThat(mDialogFragment.mActivity).isNotNull();
        mDialogFragment.mActivity = spy(mDialogFragment.mActivity);

        var dialog = mDialogFragment.getDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView title = dialog.findViewById(R.id.dialog_title);
        assertThat(title).isNotNull();
        assertThat(title.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_cannot_listen));
        TextView subtitle1 = dialog.findViewById(R.id.dialog_subtitle);
        assertThat(subtitle1).isNotNull();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.GONE);
        TextView subtitle2 = dialog.findViewById(R.id.dialog_subtitle_2);
        assertThat(subtitle2).isNotNull();
        assertThat(subtitle2.getText())
                .isEqualTo(
                        mContext.getString(R.string.audio_streams_dialog_cannot_play, DEVICE_NAME));
        View leftButton = dialog.findViewById(R.id.left_button);
        assertThat(leftButton).isNotNull();
        assertThat(leftButton.getVisibility()).isEqualTo(View.GONE);
        assertThat(leftButton.hasOnClickListeners()).isFalse();
        View rightButton = dialog.findViewById(R.id.right_button);
        assertThat(rightButton).isNotNull();
        assertThat(rightButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(rightButton.hasOnClickListeners()).isTrue();

        rightButton.callOnClick();
        assertThat(dialog.isShowing()).isFalse();
        verify(mDialogFragment.mActivity).finish();
    }

    @Test
    public void showDialog_confirmListen() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mBluetoothDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        when(mBluetoothDevice.getAlias()).thenReturn("");

        Intent intent = new Intent();
        intent.putExtra(KEY_BROADCAST_METADATA, VALID_METADATA);
        FragmentController.of(mDialogFragment, intent)
                .create(/* containerViewId= */ 0, /* bundle= */ null)
                .start()
                .resume()
                .visible()
                .get();
        shadowMainLooper().idle();

        assertThat(mDialogFragment.getMetricsCategory())
                .isEqualTo(DIALOG_AUDIO_STREAM_CONFIRM_LISTEN);
        assertThat(mDialogFragment.mActivity).isNotNull();
        mDialogFragment.mActivity = spy(mDialogFragment.mActivity);

        Dialog dialog = mDialogFragment.getDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView title = dialog.findViewById(R.id.dialog_title);
        assertThat(title).isNotNull();
        assertThat(title.getText())
                .isEqualTo(
                        mContext.getString(R.string.audio_streams_dialog_listen_to_audio_stream));
        TextView subtitle1 = dialog.findViewById(R.id.dialog_subtitle);
        assertThat(subtitle1).isNotNull();
        assertThat(subtitle1.getVisibility()).isEqualTo(View.VISIBLE);
        TextView subtitle2 = dialog.findViewById(R.id.dialog_subtitle_2);
        assertThat(subtitle2).isNotNull();
        var defaultName = mContext.getString(DEFAULT_DEVICE_NAME);
        assertThat(subtitle2.getText())
                .isEqualTo(
                        mContext.getString(
                                R.string.audio_streams_dialog_control_volume, defaultName));
        View leftButton = dialog.findViewById(R.id.left_button);
        assertThat(leftButton).isNotNull();
        assertThat(leftButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(leftButton.hasOnClickListeners()).isTrue();

        leftButton.callOnClick();
        assertThat(dialog.isShowing()).isFalse();

        Button rightButton = dialog.findViewById(R.id.right_button);
        assertThat(rightButton).isNotNull();
        assertThat(rightButton.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_listen));
        assertThat(rightButton.hasOnClickListeners()).isTrue();

        rightButton.callOnClick();
        assertThat(dialog.isShowing()).isFalse();
        verify(mDialogFragment.mActivity, times(2)).finish();
    }
}
