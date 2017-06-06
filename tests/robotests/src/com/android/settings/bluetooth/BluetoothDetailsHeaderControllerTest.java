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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowBluetoothDevice;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.R;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows={SettingsShadowBluetoothDevice.class, ShadowEntityHeaderController.class})
public class BluetoothDetailsHeaderControllerTest extends BluetoothDetailsControllerTestBase {
    private BluetoothDetailsHeaderController mController;
    private Preference mPreference;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mHeaderController;

    @Override
    public void setUp() {
        super.setUp();
        FakeFeatureFactory.setupForTest(spy(mContext));
        ShadowEntityHeaderController.setUseMock(mHeaderController);
        mController = new BluetoothDetailsHeaderController(mContext, mFragment, mCachedDevice,
                mLifecycle);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mPreference);
        setupDevice(mDeviceConfig);
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
    }

    @Test
    public void header() {
        showScreen(mController);

        verify(mHeaderController).setLabel(mDeviceConfig.getName());
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController).setIconContentDescription(any(String.class));
        verify(mHeaderController).setSummary(any(String.class));
        verify(mHeaderController).done(mActivity, mContext);
        verify(mHeaderController).done(mActivity, false);
    }

    @Test
    public void connectionStatusChangesWhileScreenOpen() {
        ArrayList<LocalBluetoothProfile> profiles = new ArrayList<>();
        InOrder inOrder = inOrder(mHeaderController);
        when(mCachedDevice.getConnectionSummary()).thenReturn(R.string.bluetooth_connected);
        showScreen(mController);
        inOrder.verify(mHeaderController).setSummary(mContext.getString(R.string.bluetooth_connected));

        when(mCachedDevice.getConnectionSummary()).thenReturn(0);
        mController.onDeviceAttributesChanged();
        inOrder.verify(mHeaderController).setSummary((CharSequence) null);

        when(mCachedDevice.getConnectionSummary()).thenReturn(R.string.bluetooth_connecting);
        mController.onDeviceAttributesChanged();
        inOrder.verify(mHeaderController).setSummary(
                mContext.getString(R.string.bluetooth_connecting));
    }
}
