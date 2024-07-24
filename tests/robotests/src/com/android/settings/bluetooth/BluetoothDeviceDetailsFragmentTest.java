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

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceControlsController.KEY_DEVICE_CONTROLS_GENERAL_GROUP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.UserManager;
import android.util.FeatureFlagUtils;
import android.view.InputDevice;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenu;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowUserManager.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class BluetoothDeviceDetailsFragmentTest {

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";

    private BluetoothDeviceDetailsFragment mFragment;
    private Context mContext;
    private RoboMenu mMenu;
    private MenuInflater mInflater;
    private FragmentTransaction mFragmentTransaction;
    private FragmentActivity mActivity;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private InputManager mInputManager;
    @Mock
    private CompanionDeviceManager mCompanionDeviceManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mInputManager).when(mContext).getSystemService(InputManager.class);
        doReturn(mCompanionDeviceManager).when(mContext)
                .getSystemService(CompanionDeviceManager.class);
        when(mCompanionDeviceManager.getAllAssociations()).thenReturn(ImmutableList.of());
        removeInputDeviceWithMatchingBluetoothAddress();
        FakeFeatureFactory.setupForTest();

        mFragment = setupFragment();
        mFragment.onAttach(mContext);

        mMenu = new RoboMenu(mContext);
        mInflater = new MenuInflater(mContext);
    }

    @Test
    public void verifyOnAttachResult() {
        assertThat(mFragment.mDeviceAddress).isEqualTo(TEST_ADDRESS);
        assertThat(mFragment.mManager).isEqualTo(mLocalManager);
        assertThat(mFragment.mCachedDevice).isEqualTo(mCachedDevice);
        assertThat(mFragment.mInputDevice).isEqualTo(null);
    }

    @Test
    public void verifyOnAttachResult_flagEnabledAndInputDeviceSet_returnsInputDevice() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES,
                true);
        InputDevice inputDevice = createInputDeviceWithMatchingBluetoothAddress();
        BluetoothDeviceDetailsFragment fragment = setupFragment();
        FragmentActivity activity = mock(FragmentActivity.class);
        doReturn(inputDevice).when(fragment).getInputDevice(any());
        doReturn(activity).when(fragment).getActivity();

        fragment.onAttach(mContext);

        assertThat(fragment.mDeviceAddress).isEqualTo(TEST_ADDRESS);
        assertThat(fragment.mManager).isEqualTo(mLocalManager);
        assertThat(fragment.mCachedDevice).isEqualTo(mCachedDevice);
        assertThat(fragment.mInputDevice).isEqualTo(inputDevice);
    }

    @Test
    public void verifyOnAttachResult_flagDisabled_returnsNullInputDevice() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES,
                false);
        InputDevice inputDevice = createInputDeviceWithMatchingBluetoothAddress();
        BluetoothDeviceDetailsFragment fragment = setupFragment();
        FragmentActivity activity = mock(FragmentActivity.class);
        doReturn(inputDevice).when(fragment).getInputDevice(any());
        doReturn(activity).when(fragment).getActivity();

        fragment.onAttach(mContext);

        assertThat(fragment.mDeviceAddress).isEqualTo(TEST_ADDRESS);
        assertThat(fragment.mManager).isEqualTo(mLocalManager);
        assertThat(fragment.mCachedDevice).isEqualTo(mCachedDevice);
        assertThat(fragment.mInputDevice).isEqualTo(null);
    }

    @Test
    public void getTitle_displayEditTitle() {
        mFragment.onCreateOptionsMenu(mMenu, mInflater);

        final MenuItem item = mMenu.getItem(0);

        assertThat(item.getTitle()).isEqualTo(mContext.getString(R.string.bluetooth_rename_button));
    }

    @Test
    public void getTitle_inputDeviceTitle() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES,
                true);
        InputDevice inputDevice = mock(InputDevice.class);
        doReturn(true).when(inputDevice).supportsSource(InputDevice.SOURCE_STYLUS);
        doReturn(inputDevice).when(mFragment).getInputDevice(mContext);
        mFragment.onAttach(mContext);

        mFragment.setTitleForInputDevice();

        assertThat(mActivity.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_device_details_title));
    }

    @Test
    public void getTitle_inputDeviceNull_doesNotSetTitle() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES,
                true);
        doReturn(null).when(mFragment).getInputDevice(mContext);
        mFragment.onAttach(mContext);

        mFragment.setTitleForInputDevice();

        verify(mActivity, times(0)).setTitle(any());
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

    @Test
    public void createPreferenceControllers_launchFromHAPage_deviceControllerNotExist() {
        BluetoothDeviceDetailsFragment fragment = setupFragment();
        Intent intent = fragment.getActivity().getIntent();
        intent.putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS);
        fragment.onAttach(mContext);

        List<AbstractPreferenceController> controllerList = fragment.createPreferenceControllers(
                mContext);

        assertThat(controllerList.stream()
                .anyMatch(controller -> controller.getPreferenceKey().equals(
                        KEY_DEVICE_CONTROLS_GENERAL_GROUP))).isFalse();
    }

    @Test
    public void createPreferenceControllers_notLaunchFromHAPage_deviceControllerExist() {
        BluetoothDeviceDetailsFragment fragment = setupFragment();
        Intent intent = fragment.getActivity().getIntent();
        intent.putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                SettingsEnums.PAGE_UNKNOWN);
        fragment.onAttach(mContext);

        List<AbstractPreferenceController> controllerList = fragment.createPreferenceControllers(
                mContext);

        assertThat(controllerList.stream()
                .anyMatch(controller -> controller.getPreferenceKey().equals(
                        KEY_DEVICE_CONTROLS_GENERAL_GROUP))).isTrue();
    }

    private InputDevice createInputDeviceWithMatchingBluetoothAddress() {
        doReturn(new int[]{0}).when(mInputManager).getInputDeviceIds();
        InputDevice device = mock(InputDevice.class);
        doReturn(TEST_ADDRESS).when(mInputManager).getInputDeviceBluetoothAddress(0);
        doReturn(device).when(mInputManager).getInputDevice(0);
        return device;
    }

    private InputDevice removeInputDeviceWithMatchingBluetoothAddress() {
        doReturn(new int[]{0}).when(mInputManager).getInputDeviceIds();
        doReturn(null).when(mInputManager).getInputDeviceBluetoothAddress(0);
        return null;
    }

    private BluetoothDeviceDetailsFragment setupFragment() {
        BluetoothDeviceDetailsFragment fragment = spy(
                BluetoothDeviceDetailsFragment.newInstance(TEST_ADDRESS));
        doReturn(mLocalManager).when(fragment).getLocalBluetoothManager(any());
        doReturn(mCachedDevice).when(fragment).getCachedDevice(any());
        doReturn(mPreferenceScreen).when(fragment).getPreferenceScreen();
        doReturn(mUserManager).when(fragment).getUserManager();

        mActivity = spy(Robolectric.setupActivity(FragmentActivity.class));
        doReturn(mActivity).when(fragment).getActivity();
        doReturn(mContext).when(fragment).getContext();

        FragmentManager fragmentManager = mock(FragmentManager.class);
        doReturn(fragmentManager).when(fragment).getFragmentManager();
        mFragmentTransaction = mock(FragmentTransaction.class);
        doReturn(mFragmentTransaction).when(fragmentManager).beginTransaction();

        doReturn(TEST_ADDRESS).when(mCachedDevice).getAddress();
        doReturn(TEST_ADDRESS).when(mCachedDevice).getIdentityAddress();
        Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS, TEST_ADDRESS);
        fragment.setArguments(args);

        return fragment;
    }
}
