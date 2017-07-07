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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowBluetoothDevice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = SettingsShadowBluetoothDevice.class)
public class BluetoothDetailsButtonsControllerTest extends BluetoothDetailsControllerTestBase {
    private BluetoothDetailsButtonsController mController;
    private LayoutPreference mLayoutPreference;
    private Button mLeftButton;
    private Button mRightButton;

    @Override
    public void setUp() {
        super.setUp();
        mController = new BluetoothDetailsButtonsController(mContext, mFragment, mCachedDevice,
                mLifecycle);
        mLeftButton = new Button(mContext);
        mRightButton = new Button(mContext);
        mLayoutPreference = new LayoutPreference(mContext, R.layout.app_action_buttons);
        mLayoutPreference.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mLayoutPreference);
        mLeftButton = (Button) mLayoutPreference.findViewById(R.id.left_button);
        mRightButton = (Button) mLayoutPreference.findViewById(R.id.right_button);
        setupDevice(mDeviceConfig);
        when(mCachedDevice.isBusy()).thenReturn(false);
    }

    @Test
    public void connected() {
        showScreen(mController);
        assertThat(mLeftButton.getText()).isEqualTo(
                mContext.getString(R.string.bluetooth_device_context_disconnect));
        assertThat(mRightButton.getText()).isEqualTo(mContext.getString(R.string.forget));
    }

    @Test
    public void clickOnDisconnect() {
        showScreen(mController);
        mLeftButton.callOnClick();
        verify(mCachedDevice).disconnect();
    }

    @Test
    public void clickOnConnect() {
        when(mCachedDevice.isConnected()).thenReturn(false);
        showScreen(mController);

        assertThat(mLeftButton.getText()).isEqualTo(
                mContext.getString(R.string.bluetooth_device_context_connect));

        mLeftButton.callOnClick();
        verify(mCachedDevice).connect(eq(true));
    }

    @Test
    public void becomeDisconnected() {
        showScreen(mController);
        // By default we start out with the device connected.
        assertThat(mLeftButton.getText()).isEqualTo(
                mContext.getString(R.string.bluetooth_device_context_disconnect));

        // Now make the device appear to have changed to disconnected.
        when(mCachedDevice.isConnected()).thenReturn(false);
        mController.onDeviceAttributesChanged();
        assertThat(mLeftButton.getText()).isEqualTo(
                mContext.getString(R.string.bluetooth_device_context_connect));

        // Click the button and make sure that connect (not disconnect) gets called.
        mLeftButton.callOnClick();
        verify(mCachedDevice).connect(eq(true));
    }

    @Test
    public void becomeConnected() {
        // Start out with the device disconnected.
        when(mCachedDevice.isConnected()).thenReturn(false);
        showScreen(mController);

        assertThat(mLeftButton.getText()).isEqualTo(
                mContext.getString(R.string.bluetooth_device_context_connect));

        // Now make the device appear to have changed to connected.
        when(mCachedDevice.isConnected()).thenReturn(true);
        mController.onDeviceAttributesChanged();
        assertThat(mLeftButton.getText()).isEqualTo(
                mContext.getString(R.string.bluetooth_device_context_disconnect));

        // Click the button and make sure that disconnnect (not connect) gets called.
        mLeftButton.callOnClick();
        verify(mCachedDevice).disconnect();
    }

    @Test
    public void forgetDialog() {
        showScreen(mController);
        FragmentManager fragmentManager = mock(FragmentManager.class);
        when(mFragment.getFragmentManager()).thenReturn(fragmentManager);
        FragmentTransaction ft = mock(FragmentTransaction.class);
        when(fragmentManager.beginTransaction()).thenReturn(ft);
        mRightButton.callOnClick();

        ArgumentCaptor<ForgetDeviceDialogFragment> dialogCaptor =
                ArgumentCaptor.forClass(ForgetDeviceDialogFragment.class);
        verify(ft).add(dialogCaptor.capture(), anyString());

        ForgetDeviceDialogFragment dialogFragment = dialogCaptor.getValue();
        assertThat(dialogFragment).isNotNull();
    }

    @Test
    public void startsOutBusy() {
        when(mCachedDevice.isBusy()).thenReturn(true);
        showScreen(mController);
        assertThat(mLeftButton.getText()).isEqualTo(
                mContext.getString(R.string.bluetooth_device_context_disconnect));
        assertThat(mRightButton.getText()).isEqualTo(mContext.getString(R.string.forget));
        assertThat(mLeftButton.isEnabled()).isFalse();

        // Now pretend the device became non-busy.
        when(mCachedDevice.isBusy()).thenReturn(false);
        mController.onDeviceAttributesChanged();
        assertThat(mLeftButton.isEnabled()).isTrue();
    }

    @Test
    public void becomesBusy() {
        showScreen(mController);
        assertThat(mLeftButton.isEnabled()).isTrue();

        when(mCachedDevice.isBusy()).thenReturn(true);
        mController.onDeviceAttributesChanged();
        assertThat(mLeftButton.isEnabled()).isFalse();
    }
}
