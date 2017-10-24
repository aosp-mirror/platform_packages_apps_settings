/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenu;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothDeviceDetailsFragmentTest {
    private BluetoothDeviceDetailsFragment mFragment;
    private Context mContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest(mContext);

        String deviceAddress = "55:66:77:88:99:AA";
        mFragment = spy(BluetoothDeviceDetailsFragment.newInstance(deviceAddress));
        doReturn(mLocalManager).when(mFragment).getLocalBluetoothManager(any());
        doReturn(mCachedDevice).when(mFragment).getCachedDevice(any());

        when(mCachedDevice.getAddress()).thenReturn(deviceAddress);
        Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS, deviceAddress);
        mFragment.setArguments(args);
        mFragment.onAttach(mContext);
    }

    @Test
    public void renameControlGetsAdded() {
        RoboMenu menu = new RoboMenu(mContext);
        MenuInflater inflater = new MenuInflater(mContext);
        mFragment.onCreateOptionsMenu(menu, inflater);
        MenuItem item = menu.getItem(0);
        assertThat(item.getTitle()).isEqualTo(mContext.getString(R.string.bluetooth_rename_button));
        assertThat(item.getIcon()).isEqualTo(mContext.getDrawable(R.drawable.ic_mode_edit));
    }

    @Test
    public void renameControlClicked() {
        RoboMenu menu = new RoboMenu(mContext);
        MenuInflater inflater = new MenuInflater(mContext);
        mFragment.onCreateOptionsMenu(menu, inflater);
        MenuItem item = menu.getItem(0);
        assertThat(item.getItemId()).isEqualTo(
                BluetoothDeviceDetailsFragment.EDIT_DEVICE_NAME_ITEM_ID);

        FragmentManager fragmentManager = mock(FragmentManager.class);
        when(mFragment.getFragmentManager()).thenReturn(fragmentManager);
        FragmentTransaction ft = mock(FragmentTransaction.class);
        when(fragmentManager.beginTransaction()).thenReturn(ft);

        ArgumentCaptor<Fragment> captor = ArgumentCaptor.forClass(Fragment.class);
        mFragment.onOptionsItemSelected(item);
        verify(ft).add(captor.capture(), eq(RemoteDeviceNameDialogFragment.TAG));
        RemoteDeviceNameDialogFragment dialog = (RemoteDeviceNameDialogFragment) captor.getValue();
        assertThat(dialog).isNotNull();
    }
}
