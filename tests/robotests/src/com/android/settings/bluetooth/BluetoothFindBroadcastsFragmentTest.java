/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceCategory;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class BluetoothFindBroadcastsFragmentTest {

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";

    private BluetoothFindBroadcastsFragment mFragment;
    private FragmentActivity mActivity;
    private Context mContext;
    private FragmentTransaction mFragmentTransaction;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock
    private PreferenceCategory mPreferenceCategroy;
    @Mock
    private LocalBluetoothLeBroadcastAssistant mBroadcastAssistant;
    @Mock
    private BluetoothLeBroadcastMetadata mBroadcastMetadata;
    @Mock
    private BluetoothBroadcastSourcePreference mBroadcastSourcePreference;
    @Mock
    private BluetoothBroadcastSourcePreference mBroadcastSourcePreferenceUserClick;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest();

        mFragment = spy(new BluetoothFindBroadcastsFragment());
        doReturn(mLocalManager).when(mFragment).getLocalBluetoothManager(any());
        doReturn(mCachedDevice).when(mFragment).getCachedDevice(any());
        doReturn(mBroadcastAssistant).when(mFragment).getLeBroadcastAssistant();
        doReturn(mPreferenceCategroy).when(mFragment).findPreference(any());
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        when(mFragment.getActivity()).thenReturn(mActivity);

        FragmentManager fragmentManager = mock(FragmentManager.class);
        when(mFragment.getFragmentManager()).thenReturn(fragmentManager);
        mFragmentTransaction = mock(FragmentTransaction.class);
        when(fragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);

        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);
        when(mCachedDevice.getIdentityAddress()).thenReturn(TEST_ADDRESS);
        Bundle args = new Bundle();
        args.putString(BluetoothFindBroadcastsFragment.KEY_DEVICE_ADDRESS, TEST_ADDRESS);
        mFragment.setArguments(args);
        mFragment.onAttach(mContext);
    }

    @Test
    public void verifyOnAttachResult() {
        assertThat(mFragment.mDeviceAddress).isEqualTo(TEST_ADDRESS);
        assertThat(mFragment.mManager).isEqualTo(mLocalManager);
        assertThat(mFragment.mCachedDevice).isEqualTo(mCachedDevice);
    }

    @Test
    public void addSource_selectedPrefIsNull_returnsNewPref() {
        mFragment.mSelectedPreference = null;

        mFragment.addSource(mBroadcastSourcePreference);

        assertThat(mFragment.mSelectedPreference).isEqualTo(mBroadcastSourcePreference);
    }

    @Test
    public void addSource_sourcePrefIsCreatedByReceiveState_removesOldPref() {
        mFragment.mSelectedPreference = mBroadcastSourcePreference;
        mFragment.mBroadcastSourceListCategory = mPreferenceCategroy;
        doReturn(true).when(mFragment.mSelectedPreference).isCreatedByReceiveState();

        mFragment.addSource(mBroadcastSourcePreferenceUserClick);

        verify(mFragment.mBroadcastSourceListCategory).removePreference(mBroadcastSourcePreference);
        assertThat(mFragment.mSelectedPreference).isEqualTo(mBroadcastSourcePreferenceUserClick);
    }

    @Test
    public void addSource_sourcePrefIsCreatedByMetadata_updatesOldPref() {
        when(mBroadcastSourcePreference.isCreatedByReceiveState()).thenReturn(false);
        when(mBroadcastSourcePreference.getBluetoothLeBroadcastMetadata())
                .thenReturn(mBroadcastMetadata);
        mFragment.mSelectedPreference = mBroadcastSourcePreference;
        mFragment.mBroadcastSourceListCategory = mPreferenceCategroy;

        mFragment.addSource(mBroadcastSourcePreferenceUserClick);

        verify(mBroadcastSourcePreference).updateMetadataAndRefreshUi(
                any(BluetoothLeBroadcastMetadata.class), anyBoolean());
        assertThat(mFragment.mSelectedPreference).isEqualTo(mBroadcastSourcePreferenceUserClick);
    }

}
