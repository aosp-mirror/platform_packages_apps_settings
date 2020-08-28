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

import static android.bluetooth.BluetoothDevice.BOND_NONE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.fakes.RoboMenu;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDeviceDetailsFragmentTest {

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";

    private BluetoothDeviceDetailsFragment mFragment;
    private Context mContext;
    private RoboMenu mMenu;
    private MenuInflater mInflater;
    private FragmentTransaction mFragmentTransaction;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest();

        mFragment = spy(BluetoothDeviceDetailsFragment.newInstance(TEST_ADDRESS));
        doReturn(mLocalManager).when(mFragment).getLocalBluetoothManager(any());
        doReturn(mCachedDevice).when(mFragment).getCachedDevice(any());
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();

        FragmentManager fragmentManager = mock(FragmentManager.class);
        when(mFragment.getFragmentManager()).thenReturn(fragmentManager);
        mFragmentTransaction = mock(FragmentTransaction.class);
        when(fragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);

        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);
        Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS, TEST_ADDRESS);
        mFragment.setArguments(args);
        mFragment.onAttach(mContext);

        mMenu = new RoboMenu(mContext);
        mInflater = new MenuInflater(mContext);
    }

    @Test
    public void verifyOnAttachResult() {
        assertThat(mFragment.mDeviceAddress).isEqualTo(TEST_ADDRESS);
        assertThat(mFragment.mManager).isEqualTo(mLocalManager);
        assertThat(mFragment.mCachedDevice).isEqualTo(mCachedDevice);
    }

    @Test
    public void getTitle_displayEditTitle() {
        mFragment.onCreateOptionsMenu(mMenu, mInflater);

        final MenuItem item = mMenu.getItem(0);

        assertThat(item.getTitle()).isEqualTo(mContext.getString(R.string.bluetooth_rename_button));
    }

    @Test
    public void editMenu_clicked_showDialog() {
        mFragment.onCreateOptionsMenu(mMenu, mInflater);
        final MenuItem item = mMenu.getItem(0);
        ArgumentCaptor<Fragment> captor = ArgumentCaptor.forClass(Fragment.class);

        mFragment.onOptionsItemSelected(item);

        assertThat(item.getItemId())
            .isEqualTo(BluetoothDeviceDetailsFragment.EDIT_DEVICE_NAME_ITEM_ID);
        verify(mFragmentTransaction).add(captor.capture(), eq(RemoteDeviceNameDialogFragment.TAG));
        RemoteDeviceNameDialogFragment dialog = (RemoteDeviceNameDialogFragment) captor.getValue();
        assertThat(dialog).isNotNull();
    }

    @Test
    public void finishFragmentIfNecessary_deviceIsBondNone_finishFragment() {
        when(mCachedDevice.getBondState()).thenReturn(BOND_NONE);

        mFragment.finishFragmentIfNecessary();

        verify(mFragment).finish();
    }
}
