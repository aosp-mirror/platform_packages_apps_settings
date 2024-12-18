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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.os.Bundle;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialogCompat.class,
            ShadowBluetoothAdapter.class,
        })
public class AudioSharingCallAudioDialogFragmentTest {
    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM1 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME1, /* groupId= */ 1, /* isActive= */ true);

    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME2, /* groupId= */ 1, /* isActive= */ true);

    private Fragment mParent;
    private AudioSharingCallAudioDialogFragment mFragment;

    @Before
    public void setUp() {
        ShadowAlertDialogCompat.reset();
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
    }

    @After
    public void tearDown() {
        ShadowAlertDialogCompat.reset();
    }

    @Test
    public void getMetricsCategory_correctValue() {
        mFragment = new AudioSharingCallAudioDialogFragment();
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.DIALOG_AUDIO_SHARING_CALL_AUDIO);
    }

    @Test
    public void onCreateDialog_flagOff_dialogNotExist() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        AudioSharingCallAudioDialogFragment.show(mParent, new ArrayList<>(), -1, (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_unattachedFragment_dialogNotExist() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mParent = new Fragment();
        AudioSharingCallAudioDialogFragment.show(mParent, new ArrayList<>(), -1, (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_nullDeviceItems_showEmptyDialog() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mFragment = new AudioSharingCallAudioDialogFragment();
        mFragment.setArguments(Bundle.EMPTY);
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(dialog.getListView()).isNull();
    }

    @Test
    public void onCreateDialog_showCorrectItems() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        ArrayList<AudioSharingDeviceItem> deviceItemList = new ArrayList<>();
        deviceItemList.add(TEST_DEVICE_ITEM1);
        deviceItemList.add(TEST_DEVICE_ITEM2);
        AudioSharingCallAudioDialogFragment.show(mParent, deviceItemList, 0, (item) -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.getListView().getCount()).isEqualTo(2);
    }
}
