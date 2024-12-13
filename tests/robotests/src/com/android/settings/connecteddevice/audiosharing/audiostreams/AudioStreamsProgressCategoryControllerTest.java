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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState.ADD_SOURCE_BAD_CODE;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState.ADD_SOURCE_FAILED;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState.SOURCE_ADDED;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState.SOURCE_PRESENT;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState.SYNCED;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState.WAIT_FOR_SYNC;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.UNSET_BROADCAST_ID;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settingslib.flags.Flags.FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import static java.util.Collections.emptyList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothUtils.class,
            ShadowAudioStreamsHelper.class,
            ShadowThreadUtils.class,
            ShadowAlertDialog.class,
        })
public class AudioStreamsProgressCategoryControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String VALID_METADATA =
            "BLUETOOTH:UUID:184F;BN:VGVzdA==;AT:1;AD:00A1A1A1A1A1;BI:1E240;BC:VGVzdENvZGU=;"
                    + "MD:BgNwVGVzdA==;AS:1;PI:A0;NS:1;BS:3;NB:2;SM:BQNUZXN0BARlbmc=;;";
    private static final String KEY = "audio_streams_nearby_category";
    private static final int QR_CODE_BROADCAST_ID = 1;
    private static final int ALREADY_CONNECTED_BROADCAST_ID = 2;
    private static final int NEWLY_FOUND_BROADCAST_ID = 3;
    private static final String BROADCAST_NAME_1 = "name_1";
    private static final String BROADCAST_NAME_2 = "name_2";
    private static final byte[] BROADCAST_CODE = new byte[] {1};
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBluetoothEventManager;
    @Mock private PreferenceScreen mScreen;
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    @Mock private BluetoothLeBroadcastMetadata mMetadata;
    @Mock private CachedBluetoothDevice mDevice;
    @Mock private AudioStreamsProgressCategoryPreference mPreference;
    @Mock private BluetoothDevice mSourceDevice;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private Fragment mFragment;
    private TestController mController;

    @Before
    public void setUp() {
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        when(mAudioStreamsHelper.getLeBroadcastAssistant()).thenReturn(mLeBroadcastAssistant);
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(emptyList());
        mSetFlagsRule.disableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        when(mLocalBtManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLeBroadcastAssistant.isSearchInProgress()).thenReturn(false);

        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);

        mFragment = new Fragment();
        mController = spy(new TestController(mContext, KEY));
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
        ShadowAudioStreamsHelper.reset();
    }

    @Test
    public void testGetAvailabilityStatus() {
        int status = mController.getAvailabilityStatus();

        assertThat(status).isEqualTo(AVAILABLE);
    }

    @Test
    public void testDisplayPreference() {
        mController.displayPreference(mScreen);

        verify(mPreference).setVisible(true);
    }

    @Test
    public void testSetScanning() {
        mController.displayPreference(mScreen);
        mController.setScanning(true);

        verify(mPreference).setProgress(true);
    }

    @Test
    public void testShowToast_noError() {
        mController.showToast(BROADCAST_NAME_1);
    }

    @Test
    public void testOnStop_unregister() {
        mController.onStop(mLifecycleOwner);

        verify(mBluetoothEventManager).unregisterCallback(any());
    }

    @Test
    public void testGetFragment_returnFragment() {
        mController.setFragment(mFragment);

        assertThat(mController.getFragment()).isEqualTo(mFragment);
    }

    @Test
    public void testOnStart_initNoDevice_showDialog() {
        when(mLeBroadcastAssistant.isSearchInProgress()).thenReturn(true);

        FragmentController.setupFragment(mFragment);
        mController.setFragment(mFragment);
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        // Called twice, once in displayPreference, the other in init()
        verify(mPreference, times(2)).setVisible(anyBoolean());
        verify(mPreference).removeAudioStreamPreferences();
        verify(mLeBroadcastAssistant).stopSearchingForSources();
        verify(mLeBroadcastAssistant).unregisterServiceCallBack(any());

        var dialog = ShadowAlertDialog.getLatestAlertDialog();
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
        Button rightButton = dialog.findViewById(R.id.right_button);
        assertThat(rightButton).isNotNull();
        assertThat(rightButton.getText())
                .isEqualTo(mContext.getString(R.string.audio_streams_dialog_no_le_device_button));
        assertThat(rightButton.hasOnClickListeners()).isTrue();

        dialog.cancel();
    }

    @Test
    public void testBluetoothOff_triggerRunnable() {
        mController.mBluetoothCallback.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);

        verify(mController.mExecutor).execute(any());
    }

    @Test
    public void testDeviceConnectionStateChanged_triggerRunnable() {
        mController.mBluetoothCallback.onProfileConnectionStateChanged(
                mDevice,
                BluetoothAdapter.STATE_DISCONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);

        verify(mController.mExecutor).execute(any());
    }

    @Test
    public void testOnStart_initHasDevice_noPreference() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mLeBroadcastAssistant).registerServiceCallBack(any(), any());
        verify(mLeBroadcastAssistant).startSearchingForSources(any());

        var dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog).isNull();

        verify(mController, never()).moveToState(any(), any());
    }

    @Test
    public void testOnStart_initHasDevice_scanningInProgress() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);
        when(mLeBroadcastAssistant.isSearchInProgress()).thenReturn(true);

        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mLeBroadcastAssistant).registerServiceCallBack(any(), any());
        verify(mLeBroadcastAssistant).stopSearchingForSources();
        verify(mLeBroadcastAssistant).startSearchingForSources(any());

        var dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog).isNull();

        verify(mController, never()).moveToState(any(), any());
    }

    @Test
    public void testOnStart_initHasDevice_getPresentSources() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);

        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        List<BluetoothLeBroadcastReceiveState> connectedList = new ArrayList<>();
        // Empty connected device list
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(connectedList);

        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAudioStreamsHelper).getAllPresentSources();
        verify(mLeBroadcastAssistant).startSearchingForSources(any());

        var dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(dialog).isNull();

        verify(mController, never()).moveToState(any(), any());
    }

    @Test
    public void testOnStart_handleSourceFromQrCode() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup a source from qr code
        mController.setSourceFromQrCode(mMetadata, SourceOriginForLogging.UNKNOWN);
        when(mMetadata.getBroadcastId()).thenReturn(QR_CODE_BROADCAST_ID);

        // Handle the source from qr code in onStart
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        // Verify the connected source is created and moved to WAIT_FOR_SYNC
        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        verify(mController).moveToState(preference.capture(), state.capture());
        assertThat(preference.getValue()).isNotNull();
        assertThat(preference.getValue().getAudioStreamBroadcastId())
                .isEqualTo(QR_CODE_BROADCAST_ID);
        assertThat(state.getValue()).isEqualTo(WAIT_FOR_SYNC);
    }

    @Test
    public void testOnStart_handleSourceAlreadyConnected() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup a connected source
        BluetoothLeBroadcastReceiveState connected =
                createConnectedMock(ALREADY_CONNECTED_BROADCAST_ID);
        List<BluetoothLeBroadcastReceiveState> list = new ArrayList<>();
        list.add(connected);
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(list);

        // Handle already connected source in onStart
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        // Verify the connected source is created and moved to SOURCE_ADDED
        verify(mController).moveToState(preference.capture(), state.capture());
        assertThat(preference.getValue()).isNotNull();
        assertThat(preference.getValue().getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
        assertThat(state.getValue()).isEqualTo(SOURCE_ADDED);
    }

    @Test
    public void testOnStart_sourceFromQrCodeNoId_sourceAlreadyConnected_sameName_updateId() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup source from qr code with unset id and BROADCAST_NAME_1. Creating a real metadata
        // for properly update its id.
        var metadata =
                BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(VALID_METADATA);
        assertThat(metadata).isNotNull();
        var metadataWithNoIdAndSameName =
                new BluetoothLeBroadcastMetadata.Builder(metadata)
                        .setBroadcastId(UNSET_BROADCAST_ID)
                        .setBroadcastName(BROADCAST_NAME_1)
                        .build();
        mController.setSourceFromQrCode(
                metadataWithNoIdAndSameName, SourceOriginForLogging.UNKNOWN);

        // Setup a connected source with name BROADCAST_NAME_1 and id
        BluetoothLeBroadcastReceiveState connected =
                createConnectedMock(ALREADY_CONNECTED_BROADCAST_ID);
        var data = mock(BluetoothLeAudioContentMetadata.class);
        when(connected.getSubgroupMetadata()).thenReturn(ImmutableList.of(data));
        when(data.getProgramInfo()).thenReturn(BROADCAST_NAME_1);
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(ImmutableList.of(connected));

        // Handle both source from qr code and already connected source in onStart
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        // Verify two preferences created, one moved to state WAIT_FOR_SYNC, one to SOURCE_ADDED.
        // Both has ALREADY_CONNECTED_BROADCAST_ID as the UNSET_ID is updated to match.
        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);
        verify(mController, times(2)).moveToState(preference.capture(), state.capture());

        List<AudioStreamPreference> preferences = preference.getAllValues();
        assertThat(preferences.size()).isEqualTo(2);
        List<AudioStreamsProgressCategoryController.AudioStreamState> states = state.getAllValues();
        assertThat(states.size()).isEqualTo(2);

        // The preference contains source from qr code
        assertThat(preferences.get(0).getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
        assertThat(states.get(0)).isEqualTo(WAIT_FOR_SYNC);

        // The preference contains already connected source
        assertThat(preferences.get(1).getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
        assertThat(states.get(1)).isEqualTo(SOURCE_ADDED);
    }

    @Test
    public void testHandleSourceFound_addNew() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        when(mMetadata.getBroadcastId()).thenReturn(NEWLY_FOUND_BROADCAST_ID);
        // A new source is found
        mController.handleSourceFound(mMetadata);

        // Verify a preference is created with state SYNCED.
        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        verify(mController).moveToState(preference.capture(), state.capture());
        assertThat(preference.getValue()).isNotNull();
        assertThat(preference.getValue().getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(state.getValue()).isEqualTo(SYNCED);
    }

    @Test
    public void testHandleSourceAddRequest_updateMetadataAndState() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        var metadata =
                BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(VALID_METADATA);
        assertThat(metadata).isNotNull();
        var metadataWithNoCode =
                new BluetoothLeBroadcastMetadata.Builder(metadata)
                        .setBroadcastId(NEWLY_FOUND_BROADCAST_ID)
                        .setBroadcastName(BROADCAST_NAME_1)
                        .build();
        // A new source is found
        mController.handleSourceFound(metadataWithNoCode);

        ArgumentCaptor<AudioStreamPreference> preferenceCaptor =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> stateCaptor =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        // moving state to SYNCED
        verify(mController).moveToState(preferenceCaptor.capture(), stateCaptor.capture());
        var preference = preferenceCaptor.getValue();
        var state = stateCaptor.getValue();

        assertThat(preference).isNotNull();
        assertThat(preference.getAudioStreamBroadcastId()).isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(state).isEqualTo(SYNCED);

        var updatedMetadata =
                new BluetoothLeBroadcastMetadata.Builder(metadataWithNoCode)
                        .setBroadcastCode(BROADCAST_CODE)
                        .build();
        mController.handleSourceAddRequest(preference, updatedMetadata);
        // state updated to ADD_SOURCE_WAIT_FOR_RESPONSE
        assertThat(preference.getAudioStreamBroadcastId()).isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(preference.getAudioStreamMetadata().getBroadcastCode())
                .isEqualTo(BROADCAST_CODE);
        assertThat(preference.getAudioStreamState()).isEqualTo(ADD_SOURCE_WAIT_FOR_RESPONSE);
    }

    @Test
    public void testHandleSourceFound_sameIdWithSourceFromQrCode_updateMetadataAndState() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup source from qr code with QR_CODE_BROADCAST_ID, BROADCAST_NAME_1 and BROADCAST_CODE.
        var metadata =
                BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(VALID_METADATA);
        assertThat(metadata).isNotNull();
        var metadataFromQrCode =
                new BluetoothLeBroadcastMetadata.Builder(metadata)
                        .setBroadcastId(QR_CODE_BROADCAST_ID)
                        .setBroadcastName(BROADCAST_NAME_1)
                        .setBroadcastCode(BROADCAST_CODE)
                        .build();
        mController.setSourceFromQrCode(metadataFromQrCode, SourceOriginForLogging.UNKNOWN);

        // Handle the source from qr code in onStart
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        // A new source is found
        mController.handleSourceFound(
                new BluetoothLeBroadcastMetadata.Builder(metadata)
                        .setBroadcastId(QR_CODE_BROADCAST_ID)
                        .setBroadcastName(BROADCAST_NAME_2)
                        .build());
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        verify(mController, times(2)).moveToState(preference.capture(), state.capture());
        List<AudioStreamPreference> preferences = preference.getAllValues();
        List<AudioStreamsProgressCategoryController.AudioStreamState> states = state.getAllValues();

        // Verify the qr code source is created with WAIT_FOR_SYNC, broadcast name got updated to
        // BROADCAST_NAME_2
        var sourceFromQrCode = preferences.get(0);
        assertThat(sourceFromQrCode.getAudioStreamBroadcastId()).isEqualTo(QR_CODE_BROADCAST_ID);
        assertThat(sourceFromQrCode.getAudioStreamMetadata()).isNotNull();
        assertThat(sourceFromQrCode.getAudioStreamMetadata().getBroadcastName())
                .isEqualTo(BROADCAST_NAME_2);
        assertThat(sourceFromQrCode.getAudioStreamMetadata().getBroadcastCode())
                .isEqualTo(BROADCAST_CODE);
        assertThat(states.get(0)).isEqualTo(WAIT_FOR_SYNC);

        // Verify the newly found source is created, broadcast code is retrieved from the source
        // from qr code, and state updated to ADD_SOURCE_WAIT_FOR_RESPONSE
        var newlyFoundSource = preferences.get(1);
        assertThat(newlyFoundSource.getAudioStreamBroadcastId()).isEqualTo(QR_CODE_BROADCAST_ID);
        assertThat(newlyFoundSource.getAudioStreamMetadata()).isNotNull();
        assertThat(newlyFoundSource.getAudioStreamMetadata().getBroadcastName())
                .isEqualTo(BROADCAST_NAME_2);
        assertThat(newlyFoundSource.getAudioStreamMetadata().getBroadcastCode())
                .isEqualTo(BROADCAST_CODE);
        assertThat(states.get(1)).isEqualTo(ADD_SOURCE_WAIT_FOR_RESPONSE);
    }

    @Test
    public void testHandleSourceFound_sameIdWithOtherState_doNothing() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup source already connected
        BluetoothLeBroadcastReceiveState connected =
                createConnectedMock(ALREADY_CONNECTED_BROADCAST_ID);
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(ImmutableList.of(connected));

        // Handle source already connected in onStart
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        // A new source found
        when(mMetadata.getBroadcastId()).thenReturn(ALREADY_CONNECTED_BROADCAST_ID);
        mController.handleSourceFound(mMetadata);
        shadowOf(Looper.getMainLooper()).idle();

        // Verify only the connected source has created a preference, and its state remains as
        // SOURCE_ADDED
        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        verify(mController).moveToState(preference.capture(), state.capture());
        assertThat(preference.getValue()).isNotNull();
        assertThat(preference.getValue().getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
        assertThat(preference.getValue().getAudioStreamState()).isEqualTo(SOURCE_ADDED);
    }

    @Test
    public void testHandleSourceLost_removed() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup mPreference so it's not null
        mController.displayPreference(mScreen);

        // A new source found
        when(mMetadata.getBroadcastId()).thenReturn(NEWLY_FOUND_BROADCAST_ID);
        mController.handleSourceFound(mMetadata);
        shadowOf(Looper.getMainLooper()).idle();

        // A new source found is lost
        mController.handleSourceLost(NEWLY_FOUND_BROADCAST_ID);
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preferenceToAdd =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamPreference> preferenceToRemove =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        // Verify a new preference is created with state SYNCED.
        verify(mController).moveToState(preferenceToAdd.capture(), state.capture());
        assertThat(preferenceToAdd.getValue()).isNotNull();
        assertThat(preferenceToAdd.getValue().getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(state.getValue()).isEqualTo(SYNCED);

        // Verify the preference with NEWLY_FOUND_BROADCAST_ID is removed.
        verify(mPreference).removePreference(preferenceToRemove.capture());
        assertThat(preferenceToRemove.getValue().getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
    }

    @Test
    public void testHandleSourceLost_sourceConnected_doNothing() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup mPreference so it's not null
        mController.displayPreference(mScreen);

        // A new source found
        when(mMetadata.getBroadcastId()).thenReturn(NEWLY_FOUND_BROADCAST_ID);
        mController.handleSourceFound(mMetadata);
        shadowOf(Looper.getMainLooper()).idle();

        // A new source found is lost, but the source is still connected
        BluetoothLeBroadcastReceiveState connected = createConnectedMock(NEWLY_FOUND_BROADCAST_ID);
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(ImmutableList.of(connected));
        mController.handleSourceLost(NEWLY_FOUND_BROADCAST_ID);
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preferenceToAdd =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        // Verify a new preference is created with state SYNCED.
        verify(mController).moveToState(preferenceToAdd.capture(), state.capture());
        assertThat(preferenceToAdd.getValue()).isNotNull();
        assertThat(preferenceToAdd.getValue().getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(state.getValue()).isEqualTo(SYNCED);

        // No preference is removed.
        verify(mPreference, never()).removePreference(any());
    }

    @Test
    public void testHandleSourceRemoved_removed() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup already connected source
        BluetoothLeBroadcastReceiveState connected =
                createConnectedMock(ALREADY_CONNECTED_BROADCAST_ID);
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(ImmutableList.of(connected));

        // Handle connected source in onStart
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        // The connect source is no longer connected
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(emptyList());
        mController.handleSourceRemoved();
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preferenceToAdd =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamPreference> preferenceToRemove =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        // Verify a new preference is created with state SOURCE_ADDED.
        verify(mController).moveToState(preferenceToAdd.capture(), state.capture());
        assertThat(preferenceToAdd.getValue()).isNotNull();
        assertThat(preferenceToAdd.getValue().getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
        assertThat(state.getValue()).isEqualTo(SOURCE_ADDED);

        // Verify the preference with ALREADY_CONNECTED_BROADCAST_ID is removed.
        verify(mPreference).removePreference(preferenceToRemove.capture());
        assertThat(preferenceToRemove.getValue().getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
    }

    @Test
    public void testHandleSourceRemoved_updateState() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup a connected source
        BluetoothLeBroadcastReceiveState connected =
                createConnectedMock(ALREADY_CONNECTED_BROADCAST_ID);
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(ImmutableList.of(connected));

        // Handle connected source in onStart
        mController.displayPreference(mScreen);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        // The connected source is identified as having a bad code
        BluetoothLeBroadcastReceiveState badCode = mock(BluetoothLeBroadcastReceiveState.class);
        when(badCode.getBroadcastId()).thenReturn(ALREADY_CONNECTED_BROADCAST_ID);
        when(badCode.getPaSyncState())
                .thenReturn(BluetoothLeBroadcastReceiveState.PA_SYNC_STATE_SYNCHRONIZED);
        when(badCode.getBigEncryptionState())
                .thenReturn(BluetoothLeBroadcastReceiveState.BIG_ENCRYPTION_STATE_BAD_CODE);
        mController.handleSourceConnectBadCode(badCode);
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        verify(mController, times(2)).moveToState(preference.capture(), state.capture());
        List<AudioStreamPreference> preferences = preference.getAllValues();
        assertThat(preferences.size()).isEqualTo(2);
        List<AudioStreamsProgressCategoryController.AudioStreamState> states = state.getAllValues();
        assertThat(states.size()).isEqualTo(2);

        // Verify the connected source is created state SOURCE_ADDED
        assertThat(preferences.get(0).getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
        assertThat(states.get(0)).isEqualTo(SOURCE_ADDED);

        // Verify the connected source is updated to state ADD_SOURCE_BAD_CODE
        assertThat(preferences.get(1).getAudioStreamBroadcastId())
                .isEqualTo(ALREADY_CONNECTED_BROADCAST_ID);
        assertThat(states.get(1)).isEqualTo(ADD_SOURCE_BAD_CODE);
    }

    @Test
    public void testHandleSourceFailedToConnect_updateState() {
        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup mPreference so it's not null
        mController.displayPreference(mScreen);

        // A new source found
        when(mMetadata.getBroadcastId()).thenReturn(NEWLY_FOUND_BROADCAST_ID);
        mController.handleSourceFound(mMetadata);
        shadowOf(Looper.getMainLooper()).idle();

        // The new found source is identified as failed to connect
        mController.handleSourceFailedToConnect(NEWLY_FOUND_BROADCAST_ID);
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        verify(mController, times(2)).moveToState(preference.capture(), state.capture());
        List<AudioStreamPreference> preferences = preference.getAllValues();
        assertThat(preferences.size()).isEqualTo(2);
        List<AudioStreamsProgressCategoryController.AudioStreamState> states = state.getAllValues();
        assertThat(states.size()).isEqualTo(2);

        // Verify one preference is created with SYNCED
        assertThat(preferences.get(0).getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(states.get(0)).isEqualTo(SYNCED);

        // Verify the preference is updated to state ADD_SOURCE_FAILED
        assertThat(preferences.get(1).getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(states.get(1)).isEqualTo(ADD_SOURCE_FAILED);
    }

    @Test
    public void testHandleSourcePresent_updateState() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        String address = "11:22:33:44:55:66";

        // Setup a device
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);

        // Setup mPreference so it's not null
        mController.displayPreference(mScreen);

        // A new source found
        when(mMetadata.getBroadcastId()).thenReturn(NEWLY_FOUND_BROADCAST_ID);
        mController.handleSourceFound(mMetadata);
        shadowOf(Looper.getMainLooper()).idle();

        // The connected source is identified as having a bad code
        BluetoothLeBroadcastReceiveState receiveState =
                mock(BluetoothLeBroadcastReceiveState.class);
        when(receiveState.getBroadcastId()).thenReturn(NEWLY_FOUND_BROADCAST_ID);
        when(receiveState.getSourceDevice()).thenReturn(mSourceDevice);
        when(mSourceDevice.getAddress()).thenReturn(address);
        List<Long> bisSyncState = new ArrayList<>();
        when(receiveState.getBisSyncState()).thenReturn(bisSyncState);

        // The new found source is identified as failed to connect
        mController.handleSourcePresent(receiveState);
        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<AudioStreamPreference> preference =
                ArgumentCaptor.forClass(AudioStreamPreference.class);
        ArgumentCaptor<AudioStreamsProgressCategoryController.AudioStreamState> state =
                ArgumentCaptor.forClass(
                        AudioStreamsProgressCategoryController.AudioStreamState.class);

        verify(mController, times(2)).moveToState(preference.capture(), state.capture());
        List<AudioStreamPreference> preferences = preference.getAllValues();
        assertThat(preferences.size()).isEqualTo(2);
        List<AudioStreamsProgressCategoryController.AudioStreamState> states = state.getAllValues();
        assertThat(states.size()).isEqualTo(2);

        // Verify one preference is created with SYNCED
        assertThat(preferences.get(0).getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(states.get(0)).isEqualTo(SYNCED);

        // Verify the preference is updated to state ADD_SOURCE_FAILED
        assertThat(preferences.get(1).getAudioStreamBroadcastId())
                .isEqualTo(NEWLY_FOUND_BROADCAST_ID);
        assertThat(states.get(1)).isEqualTo(SOURCE_PRESENT);
    }

    private static BluetoothLeBroadcastReceiveState createConnectedMock(int id) {
        var connected = mock(BluetoothLeBroadcastReceiveState.class);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(connected.getBroadcastId()).thenReturn(id);
        when(connected.getBisSyncState()).thenReturn(bisSyncState);
        return connected;
    }

    static class TestController extends AudioStreamsProgressCategoryController {
        TestController(Context context, String preferenceKey) {
            super(context, preferenceKey);
            mExecutor = spy(mContext.getMainExecutor());
        }

        @Override
        void moveToState(AudioStreamPreference preference, AudioStreamState state) {
            preference.setAudioStreamState(state);
            // Do nothing else to avoid side effect from AudioStreamStateHandler#performAction
        }
    }
}
