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

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import static com.android.settingslib.flags.Flags.FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowThreadUtils.class,
        })
public class AudioStreamsHelperTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final int GROUP_ID = 1;
    private static final int BROADCAST_ID_1 = 1;
    private static final int BROADCAST_ID_2 = 2;
    private static final String BROADCAST_NAME = "name";
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    @Mock private LocalBluetoothManager mLocalBluetoothManager;
    @Mock private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private CachedBluetoothDeviceManager mDeviceManager;
    @Mock private BluetoothLeBroadcastMetadata mMetadata;
    @Mock private CachedBluetoothDevice mCachedDevice;
    @Mock private BluetoothDevice mDevice;
    @Mock private BluetoothDevice mSourceDevice;
    private AudioStreamsHelper mHelper;

    @Before
    public void setUp() {
        mSetFlagsRule.disableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastAssistantProfile())
                .thenReturn(mAssistant);
        mHelper = spy(new AudioStreamsHelper(mLocalBluetoothManager));
    }

    @Test
    public void addSource_noDevice_doNothing() {
        when(mAssistant.getAllConnectedDevices()).thenReturn(Collections.emptyList());
        mHelper.addSource(mMetadata);

        verify(mAssistant, never()).addSource(any(), any(), anyBoolean());
    }

    @Test
    public void addSource_hasDevice() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);

        mHelper.addSource(mMetadata);

        verify(mAssistant).addSource(eq(mDevice), eq(mMetadata), anyBoolean());
    }

    @Test
    public void removeSource_noDevice_doNothing() {
        when(mAssistant.getAllConnectedDevices()).thenReturn(Collections.emptyList());
        mHelper.removeSource(BROADCAST_ID_1);

        verify(mAssistant, never()).removeSource(any(), anyInt());
    }

    @Test
    public void removeSource_noConnectedSource_doNothing() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        BluetoothLeBroadcastReceiveState source = mock(BluetoothLeBroadcastReceiveState.class);
        when(source.getBroadcastId()).thenReturn(BROADCAST_ID_2);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(source));

        mHelper.removeSource(BROADCAST_ID_1);

        verify(mAssistant, never()).removeSource(any(), anyInt());
    }

    @Test
    public void removeSource_hasConnectedSource() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        BluetoothLeBroadcastReceiveState source = mock(BluetoothLeBroadcastReceiveState.class);
        when(source.getBroadcastId()).thenReturn(BROADCAST_ID_2);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(source));
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(source.getBisSyncState()).thenReturn(bisSyncState);

        mHelper.removeSource(BROADCAST_ID_2);

        verify(mAssistant).removeSource(eq(mDevice), anyInt());
    }

    @Test
    public void removeSource_memberHasConnectedSource() {
        String address = "11:22:33:44:55:66";
        List<BluetoothDevice> devices = new ArrayList<>();
        var memberDevice = mock(BluetoothDevice.class);
        devices.add(mDevice);
        devices.add(memberDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        BluetoothLeBroadcastReceiveState source = mock(BluetoothLeBroadcastReceiveState.class);
        when(source.getBroadcastId()).thenReturn(BROADCAST_ID_2);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        var memberCachedDevice = mock(CachedBluetoothDevice.class);
        when(memberCachedDevice.getDevice()).thenReturn(memberDevice);
        when(mCachedDevice.getMemberDevice()).thenReturn(ImmutableSet.of(memberCachedDevice));
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);
        when(mAssistant.getAllSources(mDevice)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(memberDevice)).thenReturn(ImmutableList.of(source));
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(source.getBisSyncState()).thenReturn(bisSyncState);
        when(source.getSourceDevice()).thenReturn(mSourceDevice);
        when(mSourceDevice.getAddress()).thenReturn(address);

        mHelper.removeSource(BROADCAST_ID_2);

        verify(mAssistant).removeSource(eq(memberDevice), anyInt());
    }

    @Test
    public void getAllConnectedSources_noAssistant() {
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(null);
        mHelper = new AudioStreamsHelper(mLocalBluetoothManager);

        assertThat(mHelper.getAllConnectedSources()).isEmpty();
    }

    @Test
    public void getAllConnectedSources_returnSource() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        BluetoothLeBroadcastReceiveState source = mock(BluetoothLeBroadcastReceiveState.class);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(source));
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(source.getBisSyncState()).thenReturn(bisSyncState);

        var list = mHelper.getAllConnectedSources();
        assertThat(list).isNotEmpty();
        assertThat(list.get(0)).isEqualTo(source);
    }

    @Test
    public void getAllPresentSources_noSource() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);

        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mDevice);

        String address = "00:00:00:00:00:00";

        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        BluetoothLeBroadcastReceiveState source = mock(BluetoothLeBroadcastReceiveState.class);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(source));
        when(source.getSourceDevice()).thenReturn(mSourceDevice);
        when(mSourceDevice.getAddress()).thenReturn(address);

        var list = mHelper.getAllPresentSources();
        assertThat(list).isEmpty();
    }

    @Test
    public void getAllPresentSources_returnSource() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        String address = "11:22:33:44:55:66";

        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mDevice);

        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        BluetoothLeBroadcastReceiveState source = mock(BluetoothLeBroadcastReceiveState.class);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(source));
        when(source.getSourceDevice()).thenReturn(mSourceDevice);
        when(mSourceDevice.getAddress()).thenReturn(address);
        List<Long> bisSyncState = new ArrayList<>();
        when(source.getBisSyncState()).thenReturn(bisSyncState);

        var list = mHelper.getAllPresentSources();
        assertThat(list).isNotEmpty();
        assertThat(list.get(0)).isEqualTo(source);
    }

    @Test
    public void startMediaService_noDevice_doNothing() {
        mHelper.startMediaService(mContext, BROADCAST_ID_1, BROADCAST_NAME);

        verify(mContext, never()).startService(any());
    }

    @Test
    public void startMediaService_hasDevice() {
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(mDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(devices);
        BluetoothLeBroadcastReceiveState source = mock(BluetoothLeBroadcastReceiveState.class);
        when(mDeviceManager.findDevice(any())).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(GROUP_ID);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(source));
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(source.getBisSyncState()).thenReturn(bisSyncState);

        mHelper.startMediaService(mContext, BROADCAST_ID_1, BROADCAST_NAME);

        verify(mContext).startService(any());
    }

    @Test
    public void configureAppBarByOrientation_landscape_shouldNotExpand() {
        FragmentActivity fragmentActivity = mock(FragmentActivity.class);
        // AppBarLayout requires a Theme.AppCompat.
        mContext.setTheme(R.style.Theme_Settings_Home);
        AppBarLayout appBarLayout = spy(new AppBarLayout(mContext));
        setUpFragment(fragmentActivity, appBarLayout, ORIENTATION_LANDSCAPE);

        AudioStreamsHelper.configureAppBarByOrientation(fragmentActivity);

        verify(appBarLayout).setExpanded(eq(false));
    }

    @Test
    public void configureAppBarByOrientation_portrait_shouldExpand() {
        FragmentActivity fragmentActivity = mock(FragmentActivity.class);
        // AppBarLayout requires a Theme.AppCompat.
        mContext.setTheme(R.style.Theme_Settings_Home);
        AppBarLayout appBarLayout = spy(new AppBarLayout(mContext));
        setUpFragment(fragmentActivity, appBarLayout, ORIENTATION_PORTRAIT);

        AudioStreamsHelper.configureAppBarByOrientation(fragmentActivity);

        verify(appBarLayout).setExpanded(eq(true));
    }

    private void setUpFragment(
            FragmentActivity fragmentActivity, AppBarLayout appBarLayout, int orientationPortrait) {
        Resources resources = mock(Resources.class);
        when(fragmentActivity.getResources()).thenReturn(resources);
        Configuration configuration = new Configuration();
        configuration.orientation = orientationPortrait;
        when(resources.getConfiguration()).thenReturn(configuration);
        when(fragmentActivity.findViewById(anyInt())).thenReturn(appBarLayout);
    }
}
