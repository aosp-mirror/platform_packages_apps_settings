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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RemoteDeviceNameDialogFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;

    private RemoteDeviceNameDialogFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest(mContext);

        String deviceAddress = "55:66:77:88:99:AA";
        when(mCachedDevice.getAddress()).thenReturn(deviceAddress);
        mFragment = spy(RemoteDeviceNameDialogFragment.newInstance(mCachedDevice));
        doReturn(mCachedDevice).when(mFragment).getDevice(any());
    }

    /**
     * Helper method to set the mock device's name and show the dialog.
     *
     * @param deviceName what name to set
     * @return the dialog created
     */
    AlertDialog startDialog(String deviceName) {
        when(mCachedDevice.getName()).thenReturn(deviceName);
        FragmentTestUtil.startFragment(mFragment);
        return (AlertDialog) ShadowDialog.getLatestDialog();
    }

    @Test
    public void deviceNameDisplayIsCorrect() {
        String deviceName = "ABC Corp Headphones";
        AlertDialog dialog = startDialog(deviceName);
        EditText editText = (EditText) dialog.findViewById(R.id.edittext);
        assertThat(editText.getText().toString()).isEqualTo(deviceName);

        // Make sure that the "rename" button isn't enabled since the text hasn't changed yet, but
        // the "cancel" button should be enabled.
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertThat(positiveButton.isEnabled()).isFalse();
        Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(negativeButton.isEnabled()).isTrue();
    }

    @Test
    public void deviceNameEditSucceeds() {
        String deviceNameInitial = "ABC Corp Headphones";
        String deviceNameModified = "My Headphones";
        AlertDialog dialog = startDialog(deviceNameInitial);

        // Before modifying the text the "rename" button should be disabled but the cancel button
        // should be enabled.
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(negativeButton.isEnabled()).isTrue();
        assertThat(positiveButton.isEnabled()).isFalse();

        // Once we modify the text, the positive button should be clickable, and clicking it should
        // cause a call to change the name.
        EditText editText = (EditText) dialog.findViewById(R.id.edittext);
        editText.setText(deviceNameModified);
        assertThat(positiveButton.isEnabled()).isTrue();
        positiveButton.performClick();
        verify(mCachedDevice).setName(deviceNameModified);
    }

    @Test
    public void deviceNameEditThenCancelDoesntRename() {
        String deviceNameInitial = "ABC Corp Headphones";
        String deviceNameModified = "My Headphones";
        AlertDialog dialog = startDialog(deviceNameInitial);

        // Modifying the text but then hitting cancel should not cause the name to change.
        Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(negativeButton.isEnabled()).isTrue();
        EditText editText = (EditText) dialog.findViewById(R.id.edittext);
        editText.setText(deviceNameModified);
        negativeButton.performClick();
        verify(mCachedDevice, never()).setName(anyString());
    }
}
