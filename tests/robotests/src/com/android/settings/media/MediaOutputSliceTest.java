/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.media;

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.text.TextUtils;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class MediaOutputSliceTest {

    private static final String TEST_DEVICE_1_ID = "test_device_1_id";
    private static final String TEST_DEVICE_2_ID = "test_device_2_id";
    private static final String TEST_DEVICE_1_NAME = "test_device_1_name";
    private static final String TEST_DEVICE_2_NAME = "test_device_2_name";
    private static final int TEST_DEVICE_1_ICON =
            com.android.internal.R.drawable.ic_bt_headphones_a2dp;

    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private Drawable mTestDrawable;

    private final List<MediaDevice> mDevices = new ArrayList<>();

    private Context mContext;
    private MediaOutputSlice mMediaOutputSlice;
    private MediaDeviceUpdateWorker mMediaDeviceUpdateWorker;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private AudioManager mAudioManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        // Setup BluetoothAdapter
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);

        mMediaOutputSlice = new MediaOutputSlice(mContext);
        mMediaDeviceUpdateWorker = spy(new MediaDeviceUpdateWorker(mContext,
                MEDIA_OUTPUT_SLICE_URI));
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);
        mMediaDeviceUpdateWorker.mLocalMediaManager = mLocalMediaManager;
        doReturn(false).when(mMediaDeviceUpdateWorker).hasAdjustVolumeUserRestriction();
        mMediaOutputSlice.init(mMediaDeviceUpdateWorker);
    }

    @Test
    public void getSlice_workerIsNull_shouldReturnZeroRow() {
        mMediaOutputSlice.init(null);

        final Slice slice = mMediaOutputSlice.getSlice();

        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_bluetoothIsDisable_shouldReturnZeroRow() {
        mShadowBluetoothAdapter.setEnabled(false);

        final Slice slice = mMediaOutputSlice.getSlice();

        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_audioModeIsOngoingCall_shouldReturnZeroRow() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);

        final Slice slice = mMediaOutputSlice.getSlice();

        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();

        assertThat(rows).isEqualTo(0);
    }

    @Test
    public void getSlice_shouldHaveActiveDeviceName() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(false);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mDevices.add(device);
        mDevices.add(device2);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        assertThat(primaryAction.getTitle().toString()).isEqualTo(TEST_DEVICE_1_NAME);
    }

    @Test
    public void getSlice_disconnectedBluetooth_verifyTitle() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(false);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(false);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mDevices.add(device);
        mDevices.add(device2);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        assertThat(primaryAction.getTitle().toString()).isEqualTo(mContext.getString(
                R.string.media_output_disconnected_status, TEST_DEVICE_1_NAME));
    }

    @Test
    public void getSlice_inGroupState_checkSliceSize() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(true);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectedDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        when(mMediaDeviceUpdateWorker.getSessionVolumeMax()).thenReturn(100);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();

        assertThat(SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM, null).size())
                .isEqualTo(mDevices.size() + 1);
    }

    @Test
    public void getSlice_notInGroupState_checkSliceSize() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(true);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectableDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();

        assertThat(SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM, null).size())
                .isEqualTo(mDevices.size());
    }

    @Test
    public void getSlice_singleCastDevice_notContainGroupIconText() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(null);
        mSelectedDevices.add(device);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();

        final String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo, mContext.getText(R.string.add))).isEqualTo(-1);
    }

    @Test
    public void getSlice_multipleCastDevices_containGroupIconText() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(true);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectableDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo, mContext.getText(R.string.add))).isNotEqualTo(-1);
    }

    @Test
    public void getSlice_onTransferring_containTransferringSubtitle() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(false);
        when(device2.getState()).thenReturn(LocalMediaManager.MediaDeviceState.STATE_CONNECTING);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectableDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        final String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo, mContext.getText(R.string.media_output_switching)))
                .isNotEqualTo(-1);
    }

    @Test
    public void getSlice_onTransferringFailed_containFailedSubtitle() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(false);
        when(device2.getState()).thenReturn(LocalMediaManager.MediaDeviceState
                .STATE_CONNECTING_FAILED);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectableDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        final String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo, mContext.getText(
                R.string.media_output_switch_error_text))).isNotEqualTo(-1);
    }

    @Test
    public void getSlice_zeroState_containPairingText() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_PHONE_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        mSelectedDevices.add(device);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo, mContext.getText(
                R.string.bluetooth_pairing_pref_title))).isNotEqualTo(-1);
    }

    @Test
    public void getSlice_twoConnectedDevices_notContainPairingText() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(true);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectableDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo, mContext.getText(
                R.string.bluetooth_pairing_pref_title))).isEqualTo(-1);
    }

    @Test
    public void getSlice_disconnectedBtOnTransferring_containTransferringSubtitle() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(false);
        when(device2.getState()).thenReturn(LocalMediaManager.MediaDeviceState.STATE_CONNECTING);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectableDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        final String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo, mContext.getText(R.string.media_output_switching)))
                .isNotEqualTo(-1);
    }

    @Test
    public void getSlice_disconnectedBtOnTransferringFailed_containTransferringFailedSubtitle() {
        final List<MediaDevice> mSelectedDevices = new ArrayList<>();
        final List<MediaDevice> mSelectableDevices = new ArrayList<>();
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(device.getIcon()).thenReturn(mTestDrawable);
        when(device.getMaxVolume()).thenReturn(100);
        when(device.isConnected()).thenReturn(true);
        when(device.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        final MediaDevice device2 = mock(MediaDevice.class);
        when(device2.getName()).thenReturn(TEST_DEVICE_2_NAME);
        when(device2.getIcon()).thenReturn(mTestDrawable);
        when(device2.getMaxVolume()).thenReturn(100);
        when(device2.isConnected()).thenReturn(false);
        when(device2.getState()).thenReturn(
                LocalMediaManager.MediaDeviceState.STATE_CONNECTING_FAILED);
        when(device2.getDeviceType()).thenReturn(MediaDevice.MediaDeviceType.TYPE_BLUETOOTH_DEVICE);
        when(device2.getId()).thenReturn(TEST_DEVICE_2_ID);
        mSelectedDevices.add(device);
        mSelectableDevices.add(device2);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(device);
        mDevices.add(device);
        mDevices.add(device2);
        when(mLocalMediaManager.getSelectedMediaDevice()).thenReturn(mSelectedDevices);
        when(mLocalMediaManager.getSelectableMediaDevice()).thenReturn(mSelectableDevices);
        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Slice mediaSlice = mMediaOutputSlice.getSlice();
        final String sliceInfo = SliceQuery.findAll(mediaSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null).toString();

        assertThat(TextUtils.indexOf(sliceInfo,
                mContext.getText(R.string.bluetooth_connect_failed))).isNotEqualTo(-1);
    }

    @Test
    public void onNotifyChange_foundMediaDevice_connect() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mLocalMediaManager.getMediaDeviceById(mDevices, TEST_DEVICE_1_ID)).thenReturn(device);
        mDevices.add(device);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Intent intent = new Intent();
        intent.putExtra("media_device_id", TEST_DEVICE_1_ID);

        mMediaOutputSlice.onNotifyChange(intent);

        verify(mLocalMediaManager).connectDevice(device);
    }

    @Test
    public void onNotifyChange_notFoundMediaDevice_doNothing() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mLocalMediaManager.getMediaDeviceById(mDevices, TEST_DEVICE_1_ID)).thenReturn(device);
        mDevices.add(device);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Intent intent = new Intent();
        intent.putExtra("media_device_id", "fake_123");

        mMediaOutputSlice.onNotifyChange(intent);

        verify(mLocalMediaManager, never()).connectDevice(device);
    }

    @Test
    public void onNotifyChange_adjustVolume() {
        mDevices.clear();
        final MediaDevice device = mock(MediaDevice.class);
        when(device.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mLocalMediaManager.getMediaDeviceById(mDevices, TEST_DEVICE_1_ID)).thenReturn(device);
        mDevices.add(device);

        mMediaDeviceUpdateWorker.onDeviceListUpdate(mDevices);

        final Intent intent = new Intent();
        intent.putExtra("media_device_id", TEST_DEVICE_1_ID);
        intent.putExtra(EXTRA_RANGE_VALUE, 30);

        mMediaOutputSlice.onNotifyChange(intent);

        verify(device).requestSetVolume(30);
    }
}
